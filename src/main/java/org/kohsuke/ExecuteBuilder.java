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
 * Generates {@link MethodHandle} of type {@code (Object lhs,JexlContext) -> Object}
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

        MethodHandle[] argFilters = new MethodHandle[argc];
        int[] permutatePos = new int[argc+1];
        for (int i=0; i<argc; i++) {
            argFilters[i] = valueBuilder.build(node.jjtGetChild(i+1));
            permutatePos[i+1] = 1;
        }

        MutableCallSite site = new MutableCallSite(methodType(Object.class, objectArrayOfSize(argc+1)));
        // h(lhs,arg1,arg2,...) => dispatchFallback(site,methodName,lsh,arg1,arg2,...)
        MethodHandle h = insertArguments(findBoundInstanceMethod("dispatchFallback"),0,site,methodName).asCollector(Object[].class,argc+1);
        
        site.setTarget(h);
        h = site.dynamicInvoker();
        
        // g(lhs,c,c,c,...) => h(lhs,arg1(c),arg2(c),...)
        MethodHandle g = filterArguments(h,1,argFilters);

        // f(lhs,context) => g(lhs,context,context,context,...)
        MethodHandle f = permuteArguments(g,methodType(Object.class,Object.class,JexlContext.class),permutatePos);

        return f;
    }

    public Object dispatchFallback(MutableCallSite caller, String methodName, Object[] args) throws Throwable {
        Method m = find(args[0],methodName);

        if (m==null)    return null;    // no method found

        MethodHandle target = lookup.unreflect(m).asType(methodType(Object.class,objectArrayOfSize(args.length)));

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

    private List<Class<?>> objectArrayOfSize(int n) {
        List<Class<?>> l = new ArrayList<Class<?>>(n);
        for (int i=0; i<n; i++)
            l.add(Object.class);
        return l;
    }
}
