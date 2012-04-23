package org.kohsuke;

import org.apache.commons.jexl.JexlContext;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandles.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class Sandbox {
    MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static void main(String[] args) throws Throwable {
        new Sandbox().run();
    }
    
    public void run() throws Throwable {
        MethodHandle accept = findStatic("accept");

        MethodHandle h = permuteArguments(
                filterArguments(accept, 0, findStatic("f1"), findStatic("f2"), findStatic("f1")),
                MethodType.methodType(void.class, String.class), 0, 0, 0);
        
        h.invokeWithArguments("Foo");
    }
    
    public static void accept(Object o1, Object o2, Object o3) {
        System.out.println(o1);
        System.out.println(o2);
        System.out.println(o3);
    }
    
    public static Object f1(String s) {
        return s.toLowerCase();
    }

    public static Object f2(String s) {
        return s.toUpperCase();
    }
    
    private MethodHandle findStatic(String name) {
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
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * Wraps the given method handle into a separate class that includes 'invokedynamic' via DynamicIndy,
     * so that we can see the compiler optimization.
     */
    public static MethodHandle wrap(MethodHandle h) {
        TEST = h;
        final MethodHandle root = new DynamicIndy().invokeDynamic("unusedMethodName",h.type(),
                Sandbox.class,"bsm", MethodType.methodType(CallSite.class,MethodHandles.Lookup.class,String.class,MethodType.class));
        return root;
    }

    private static MethodHandle TEST;

    public static CallSite bsm(MethodHandles.Lookup caller, String methodName, MethodType type) {
        return new ConstantCallSite(TEST);
    }
}
