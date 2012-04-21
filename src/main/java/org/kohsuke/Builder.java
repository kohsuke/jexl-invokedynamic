package org.kohsuke;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTAndNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTArrayLiteral;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTDivNode;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTElvisNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTFalseNode;
import org.apache.commons.jexl.parser.ASTFloatLiteral;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTGENode;
import org.apache.commons.jexl.parser.ASTGTNode;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.parser.ASTLENode;
import org.apache.commons.jexl.parser.ASTLTNode;
import org.apache.commons.jexl.parser.ASTMapEntry;
import org.apache.commons.jexl.parser.ASTMapLiteral;
import org.apache.commons.jexl.parser.ASTMethod;
import org.apache.commons.jexl.parser.ASTModNode;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNENode;
import org.apache.commons.jexl.parser.ASTNotNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTOrNode;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTSizeMethod;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTSubtractNode;
import org.apache.commons.jexl.parser.ASTTernaryNode;
import org.apache.commons.jexl.parser.ASTTrueNode;
import org.apache.commons.jexl.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Node;
import org.apache.commons.jexl.parser.ParserVisitor;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.util.Coercion;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.*;

/**
 * {@link ParserVisitor} that builds {@link MethodHandle}.
 *
 * TODO: needs to be combined with the byte code generator
 * MethodHandle composers aren't powerful enough to cover the whole expression,
 * so we need to primarily rely on byte code generation, then only use MethodHandles
 * to link to the supporting runtime.
 *
 * TODO: faster map lookup
 * making map lookup inlinable ala JRuby is important.
 *
 * @author Kohsuke Kawaguchi
 */
public class Builder extends AbstractBuilder {
    private final ExecuteBuilder executeBuilder = new ExecuteBuilder(this);

    public MethodHandle build(Node n) {
        return (MethodHandle)n.jjtAccept(this,null);
    }
    
    public Object visit(ASTJexlScript node, Object data) {
        return block(node);
    }

    public Object visit(ASTBlock node, Object data) {
        return block(node);
    }
    
    private Object block(SimpleNode node) {
        MethodHandle r = child(node,0);
        for (int i=1; i<node.jjtGetNumChildren(); i++) {
            foldArguments(
                child(node,i),
                ignoreReturnValue(r)
            );
        }
        return r;
    }

    public Object visit(ASTEmptyFunction node, Object data) {
        return MethodHandles.filterReturnValue(
            child(node,0),
            findStatic("isEmpty")
        );
    }
    
    // TODO: share this with ASTEmptyFunction
    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }

        if (o instanceof String && "".equals(o)) {
            return true;
        }

        if (o instanceof Object[] && ((Object[])o).length==0)
            return true;
        
        if (o.getClass().isArray() && Array.getLength(o) == 0) {
            return true;
        }

        if (o instanceof Collection && ((Collection) o).isEmpty()) {
            return true;
        }

        /*
         * Map isn't a collection
         */
        if (o instanceof Map && ((Map) o).isEmpty()) {
            return true;
        }

        return false;
    }
    
    

    public Object visit(ASTSizeFunction node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTIdentifier node, Object data) {
        return insertArguments(findStatic("identifier"), 1,
                node.getIdentifierString());
    }
    
    public static Object identifier(JexlContext context, String name) {
        return context.getVars().get(name);
    }

    public Object visit(ASTExpression node, Object data) {
        return child(node,0);
    }

    public Object visit(ASTAssignment node, Object data) {
        Node left = node.jjtGetChild(0);
        if (left instanceof ASTReference) {
            ASTReference reference = (ASTReference) left;
            left = (SimpleNode) reference.jjtGetChild(0);
            if (left instanceof ASTIdentifier) {
                String identifier = ((ASTIdentifier) left).getIdentifierString();

                // we want to do assign(context,lhs(context))
                return foldArguments(
                        insertArguments(findStatic("assign"), 1, identifier),
                        child(node, 1));
            }
        }

        // if lhs is not assignable, just evaluate to rhs
        // IMHO this should be rejected as a parse error
        return child(node,1);
    }
    
    public static Object assign(JexlContext context, String name, Object rhs) {
        context.getVars().put(name,rhs);
        return rhs;
    }

    public Object visit(ASTElvisNode node, Object data) {
        return foldArguments(
            guardWithTest(// arg list is (x,context)
                    findStatic("isNull"),
                    dropArguments(identity(JexlContext.class), 0, Object.class),
                    dropArguments(child(node,1),0,Object.class)),
            child(node,0)
        );
    }
    
    public static boolean isNull(Object o) {
        return o==null;
    }

    public Object visit(ASTTernaryNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTOrNode node, Object data) {
        return foldArguments(
                guardWithTest(// arg list is (x,content)
                    findStatic("coerceBoolean"),
                    constantTrue,
                    dropArguments(child(node,1),0,Object.class)),
                child(node,0)   // evaluate lhs first
        );
    }
    
    public static Boolean coerceBoolean(Object o) {
        return Coercion.coerceBoolean(o);
    }

    public Object visit(ASTAndNode node, Object data) {
        return foldArguments(
                guardWithTest(// arg list is (x,content)
                        findStatic("coerceBoolean"),
                        dropArguments(child(node, 1), 0, Object.class),
                        constantFalse),
                child(node, 0)   // evaluate lhs first
        );
    }

    public Object visit(ASTBitwiseOrNode node, Object data) {
        return binary(node, "bitOr");
    }

    public static long bitOr(Object lhs,Object rhs) throws Exception {
        return Coercion.coerceLong(lhs).longValue()|Coercion.coerceLong(rhs).longValue();
    }

    public Object visit(ASTBitwiseXorNode node, Object data) {
        return binary(node, "bitXor");
    }

    public static long bitXor(Object lhs,Object rhs) throws Exception {
        return Coercion.coerceLong(lhs).longValue()^Coercion.coerceLong(rhs).longValue();
    }

    public Object visit(ASTBitwiseAndNode node, Object data) {
        return binary(node, "bitAnd");
    }

    public static long bitAnd(Object lhs,Object rhs) throws Exception {
        return Coercion.coerceLong(lhs).longValue()&Coercion.coerceLong(rhs).longValue();
    }

    public Object visit(ASTEQNode node, Object data) {
        return binary(node,"eq");
    }
    
    // TODO: share this with ASTEQNode
    public static Object eq(Object left, Object right) throws Exception {
        if (left == null && right == null) {
            /*
             * if both are null L == R
             */
            return Boolean.TRUE;
        } else if (left == null || right == null) {
            /*
             * we know both aren't null, therefore L != R
             */
            return Boolean.FALSE;
        } else if (left.getClass().equals(right.getClass())) {
            return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Float || left instanceof Double
                || right instanceof Float || right instanceof Double) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return l.equals(r) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Number || right instanceof Number
                || left instanceof Character || right instanceof Character) {
            return Coercion.coerceLong(left).equals(Coercion.coerceLong(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return Coercion.coerceBoolean(left).equals(
                    Coercion.coerceBoolean(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof java.lang.String || right instanceof String) {
            return left.toString().equals(right.toString()) ? Boolean.TRUE
                    : Boolean.FALSE;
        }

        return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
    }

    public Object visit(ASTNENode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTLTNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTGTNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTLENode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTGENode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTAddNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTSubtractNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMulNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTDivNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTModNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTUnaryMinusNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBitwiseComplNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTNotNode node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTNullLiteral node, Object data) {
        return constantNull;
    }

    public Object visit(ASTTrueNode node, Object data) {
        return constantTrue;
    }

    public Object visit(ASTFalseNode node, Object data) {
        return constantFalse;
    }

    public Object visit(ASTIntegerLiteral node, Object data) {
        return literal(node);
    }

    public Object visit(ASTFloatLiteral node, Object data) {
        return literal(node);
    }

    public Object visit(ASTStringLiteral node, Object data) {
        return literal(node);
    }

    public Object visit(ASTExpressionExpression node, Object data) {
        return child(node,0);
    }

    public Object visit(ASTStatementExpression node, Object data) {
        return child(node,0);
    }

    public Object visit(ASTReferenceExpression node, Object data) {
        return child(node,0);
    }

    public Object visit(ASTIfStatement node, Object data) {
        return guardWithTest(
                filterReturnValue(child(node,0),findStatic("coerceBoolean")),
                child(node,1),
                node.jjtGetNumChildren()==3?child(node,2):constantNull
        );
    }

    public Object visit(ASTWhileStatement node, Object data) {
        return insertArguments(findStatic("_while"),1,child(node,0),child(node,1));
    }
    
    public static Object _while(JexlContext context, MethodHandle cond, MethodHandle body) throws Throwable {
        // TODO: does MethodHandle.invokeExact() get inlined?
        Object result = null;
        while (Coercion.coerceBoolean(cond.invokeExact(context)).booleanValue()) {
//            result = body.invokeExact(context);
            result = body.invokeWithArguments(context); // invokeExact relies on method descriptor at the call site, which Java cannot provide. ouch.
        }

        return result;
    }

    public Object visit(ASTForeachStatement node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMapLiteral node, Object data) {
        // x=new HashMap()
        // x=add(x,k1,v1)
        // x=add(x,k2,v2)
        // ...
        // x
        
        // if ASTMMapLiteral only contains ASTMapEntry, this can be further optimized.
        MethodHandle r = findStatic("newHashMap");
        for (int i=0; i<node.jjtGetNumChildren(); i++) {
            r = binary(r,child(node,i),findStatic("mapAdd"));
        }
        return asReturnType(Object.class,r);
    }

    public Object visit(ASTMapEntry node, Object data) {
        return binary(node,"mapEntry");
    }
    
    public static HashMap mapAdd(HashMap hash,Object[] keyAndValue) {
        hash.put(keyAndValue[0],keyAndValue[1]);
        return hash;
    }
    
    public static HashMap newHashMap() {
        return new HashMap();
    }
    
    public static Object[] mapEntry(Object lhs, Object rhs) {
        return new Object[]{lhs,rhs};
    }

    public Object visit(ASTArrayLiteral node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMethod node, Object data) {
        // legal syntax doesn't allow this. Instead, it gets processed by ExecuteBuilder
        throw new AssertionError();
    }
    
    public Object visit(ASTArrayAccess node, Object data) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTSizeMethod node, Object data) {
        // TODO: trivial
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTReference node, Object data) {
        return executeBuilder.build(node);
    }



    private MethodHandle child(Node node, int i) {
        return (MethodHandle)node.jjtGetChild(i).jjtAccept(this, null);
    }

    private static MethodHandle ignoreContext(MethodHandle h) {
        return dropArguments(h,0,JexlContext.class);
    }
    
    /**
     * combine(lhs(context),rhs(context))
     */
    private MethodHandle binary(MethodHandle lhs, MethodHandle rhs, MethodHandle combiner) {
        // x=lhs(context)
        // op(x,rhs(context))
        return foldArguments(
            filterArguments(combiner, 1, rhs),
            lhs
        );
    }

    private MethodHandle binary(MethodHandle lhs, MethodHandle rhs, String combiner) {
        return binary(lhs,rhs,findStatic(combiner));
    }

    private MethodHandle binary(SimpleNode node, String combiner) {
        return binary(child(node,0),child(node,1),findStatic(combiner));
    }
    
    private static MethodHandle literal(SimpleNode node) {
        try {
            return literal(node.value(null));
        } catch (Exception e) {
            throw new Error(e); // impossible
        }
    }
    private static MethodHandle literal(Object o) {
        return ignoreContext(constant(Object.class, o));
    }
    
    private static final MethodHandle constantTrue = literal(true);
    private static final MethodHandle constantFalse = literal(false);
    private static final MethodHandle constantNull = literal((Object)null);
    
    
}
