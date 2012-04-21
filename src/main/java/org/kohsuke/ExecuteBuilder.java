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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandles.*;

/**
 * Generates {@link MethodHandle} of type {@code (JexlContext,Object lhs) -> Object}
 * that corresponds to {@link SimpleNode#execute(Object, JexlContext)}.
 * 
 * @author Kohsuke Kawaguchi
 */
class ExecuteBuilder extends AbstractBuilder{
    private final Builder valueBuilder;

    public MethodHandle build(Node n) {
        return (MethodHandle)n.jjtAccept(this,null);
    }
    
    ExecuteBuilder(Builder valueBuilder) {
        this.valueBuilder = valueBuilder;
    }

    public Object visit(ASTMethod node, Object data) {
        String methodName = ((ASTIdentifier) node.jjtGetChild(0)).getIdentifierString();

        MethodType.methodType(Object.class, )

        MutableCallSite site = new MutableCallSite(
                insertArguments(findStatic("dispatchFallback"),0,site
        );

        return site.dynamicInvoker();
    }

    public Object dispatchFallback(MutableCallSite caller, String methodName, Object[] args) throws Throwable {
        Method m = find(args[0],methodName);

        if (m==null)    return null;    // no method found

        MethodHandle target = lookup.unreflect(m);

        // update the call site to use this method for discovery, then fallback
        caller.setTarget(guardWithTest(
                insertArguments(findStatic("ofClass"),0,args[0].getClass()),
                target,
                insertArguments(findStatic("dispatchFallback"),0,caller,methodName).asCollector(Object[].class, args.length)
        ));

        return target.invokeWithArguments(args);
    }

    public static boolean ofClass(Class c, Object o) {
        return o.getClass()==c;
    }

    private static Method find(Object self, String methodName) throws IllegalAccessException {
        // TODO: replace with the proper method discovery code
        for (Method m : self.getClass().getMethods()) {
            if (m.getName().equals(methodName))
                return m;
        }
        return null;    // couldn't find it
    }

}
