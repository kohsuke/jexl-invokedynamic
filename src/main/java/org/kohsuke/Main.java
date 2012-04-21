package org.kohsuke;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.context.HashMapContext;
import org.apache.commons.jexl.parser.Node;
import org.junit.Assert;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Throwable {
//        MethodHandle h = build(compile("1"));
//        Object r = h.invokeWithArguments(new HashMapContext());
//        System.out.println(r);
        
        FastExpression e = build(compile("5"));
        Assert.assertEquals(5,e.evaluate(null));
        for (int i=0; i<10000; i++)
            invokeRepeatedly(e,context("x","Zoo"));
//        System.out.println(e.evaluate(context("x", "Zoo")));
    }

    private static void invokeRepeatedly(FastExpression h, JexlContext context) throws Throwable {
        for (int i=0; i<100; i++)
            for (int j=0; j<100; j++)
                h.evaluate(context);
    }

    public static JexlContext context(Object... args) {
        HashMapContext r = new HashMapContext();
        for (int i=0; i<args.length; i+=2) {
            r.put(args[i],args[i+1]);
        }
        return r;
    }

    private static FastExpression build(Expression exp) throws Exception {
        Class<?> c = Class.forName(Expression.class.getName() + "Impl");
        Field f = c.getDeclaredField("node");
        f.setAccessible(true);
        Node node = (Node)f.get(exp);
        MethodHandle h = new Builder().build(node);

        // SAM uses java.lang.reflect.Proxy so doesn't really inline very well AFAICT
        // FastExpression e = MethodHandleProxies.asInterfaceInstance(FastExpression.class,h);
        // return e;
        TEST = h;
        final MethodHandle root = new DynamicIndy().invokeDynamic("unusedMethodName",MethodType.methodType(Object.class,JexlContext.class),
                Main.class,"bsm", MethodType.methodType(CallSite.class,MethodHandles.Lookup.class,String.class,MethodType.class));

        return new FastExpression() {
            public Object evaluate(JexlContext context) {
                try {
                    return root.invokeWithArguments(context);
                } catch (Throwable t) {
                    t.printStackTrace();
                    return t;
                }
            }
        };
    }

    private static MethodHandle TEST;

    public static CallSite bsm(MethodHandles.Lookup caller, String methodName, MethodType type) {
        return new ConstantCallSite(TEST);
    }

    private static Expression compile(String s) throws Exception {
        return ExpressionFactory.createExpression(s);
    }
}
