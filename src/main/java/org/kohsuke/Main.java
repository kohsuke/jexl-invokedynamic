package org.kohsuke;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.context.HashMapContext;
import org.apache.commons.jexl.parser.Node;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Throwable {
//        MethodHandle h = build(compile("1"));
//        Object r = h.invokeWithArguments(new HashMapContext());
//        System.out.println(r);
        
        System.out.println("Zoo".hashCode());
        System.out.println(build(compile("x.hashCode()")).invokeWithArguments(context("x","Zoo")));
    }
    
    public static JexlContext context(Object... args) {
        HashMapContext r = new HashMapContext();
        for (int i=0; i<args.length; i+=2) {
            r.put(args[i],args[i+1]);
        }
        return r;
    }

    private static MethodHandle build(Expression exp) throws Exception {
        Class<?> c = Class.forName(Expression.class.getName() + "Impl");
        Field f = c.getDeclaredField("node");
        f.setAccessible(true);
        Node node = (Node)f.get(exp);
        return new Builder().build (node);
    }

    private static Expression compile(String s) throws Exception {
        return ExpressionFactory.createExpression(s);
    }
}
