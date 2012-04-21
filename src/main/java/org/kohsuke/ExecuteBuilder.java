package org.kohsuke;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTMethod;
import org.apache.commons.jexl.parser.Node;
import org.apache.commons.jexl.parser.SimpleNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

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
        int argc = node.jjtGetNumChildren()-1;

        List<Class<?>> argType = new ArrayList<Class<?>>(argc);
        for (int i=0; i<argc; i++)  argType.add(Object.class);

        MutableCallSite site = new MutableCallSite(methodType(Object.class, argType));
        site.setTarget(
            insertArguments(findBoundInstanceMethod("dispatchFallback"),0,site,methodName).asCollector(Object[].class,argc)
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
                insertArguments(findBoundInstanceMethod("dispatchFallback"),0,caller,methodName).asCollector(Object[].class, args.length)
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
