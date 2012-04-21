package org.kohsuke;

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
import org.apache.commons.jexl.parser.ParserVisitor;
import org.apache.commons.jexl.parser.SimpleNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandles.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractBuilder implements ParserVisitor {
    protected final MethodHandles.Lookup lookup = lookup();

    protected MethodHandle ignoreReturnValue(MethodHandle h) {
        return asReturnType(void.class,h);
    }

    protected MethodHandle asReturnType(Class type, MethodHandle h) {
        return h.asType(MethodType.methodType(type, h.type()));
    }


    protected IllegalAccessError handle(IllegalAccessException e) {
        return (IllegalAccessError)new IllegalAccessError(e.getMessage()).initCause(e);
    }

    protected NoSuchMethodError handle(NoSuchMethodException e) {
        return (NoSuchMethodError)new NoSuchMethodError(e.getMessage()).initCause(e);
    }

    protected MethodHandle findStatic(String name) {
        // TODO: cache the result
        try {
            for (Method m : getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getName().equals(name))
                    return lookup.findStatic(getClass(), name, MethodType.methodType(
                            m.getReturnType(),
                            m.getParameterTypes()
                    ));
            }
            throw new NoSuchMethodError(name);
        } catch (NoSuchMethodException e) {
            throw handle(e);
        } catch (IllegalAccessException e) {
            throw handle(e);
        }
    }


// default dummy implementations of the visitor method follows
    public Object visit(SimpleNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTJexlScript node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBlock node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTEmptyFunction node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTSizeFunction node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTIdentifier node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTExpression node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTAssignment node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTElvisNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTTernaryNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTOrNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTAndNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBitwiseOrNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBitwiseXorNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBitwiseAndNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTEQNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTNENode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTLTNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTGTNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTLENode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTGENode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTAddNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTSubtractNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMulNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTDivNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTModNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTUnaryMinusNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTBitwiseComplNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTNotNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTNullLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTTrueNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTFalseNode node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTIntegerLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTFloatLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTStringLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTExpressionExpression node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTStatementExpression node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTReferenceExpression node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTIfStatement node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTWhileStatement node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTForeachStatement node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMapLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTArrayLiteral node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMapEntry node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTMethod node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTArrayAccess node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTSizeMethod node, Object data) {
        throw new UnsupportedOperationException();
    }

    public Object visit(ASTReference node, Object data) {
        throw new UnsupportedOperationException();
    }
}
