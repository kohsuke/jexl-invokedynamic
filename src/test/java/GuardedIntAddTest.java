import junit.framework.TestCase;
import org.kohsuke.Sandbox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class GuardedIntAddTest extends TestCase {
    public void test1() throws Throwable {
        // builds 30+12 as tree
        MethodHandle a = constant(Object.class, 30);
        MethodHandle b = constant(Object.class, 12);

        MethodHandle h = lookup().unreflect(getClass().getMethod("add",int.class,int.class));
        MethodHandle r = foldArguments(foldArguments(h,asReturnType(int.class,a)),asReturnType(int.class,b));
        r = Sandbox.wrap(r);

        for (int i=0; i<100000; i++)
            assertEquals(42, r.invokeWithArguments());
    }

    public static int add(int a, int b) {
        return a+b;
    }
    
    public static MethodHandle asReturnType(Class type, MethodHandle h) {
        return h.asType(MethodType.methodType(type,h.type()));
    }

/*
If 30 and 12 are defined as int.class, they optimize as 42:

  # {method} 'invokedynamic' '()I' in 'Gen0'
  #           [sp+0x20]  (sp of caller)
  0x00007f326238a040: push   %rbp
  0x00007f326238a041: sub    $0x10,%rsp
  0x00007f326238a045: nop                       ;*synchronization entry
                                                ; - Gen0::invokedynamic@-1
  0x00007f326238a046: mov    $0x2a,%eax
  0x00007f326238a04b: add    $0x10,%rsp
  0x00007f326238a04f: pop    %rbp
  0x00007f326238a050: test   %eax,0x5b2cfaa(%rip)        # 0x00007f3267eb7000
                                                ;   {poll_return}
  0x00007f326238a056: retq

But if they are typed as Object.class constant, they do no optimize to primitives

  # {method} 'invokedynamic' '()I' in 'Gen0'
  #           [sp+0x20]  (sp of caller)
  0x00007f785ca29700: push   %rbp
  0x00007f785ca29701: sub    $0x10,%rsp
  0x00007f785ca29705: nop                       ;*synchronization entry
                                                ; - Gen0::invokedynamic@-1
  0x00007f785ca29706: mov    $0x7d66619d8,%r10  ;   {oop(a 'java/ang/nteger' = 30)}
  0x00007f785ca29710: mov    0xc(%r10),%eax
  0x00007f785ca29714: mov    $0x7d66618b8,%r10  ;   {oop(a 'java/ang/nteger' = 12)}
  0x00007f785ca2971e: add    0xc(%r10),%eax     ;*iadd
                                                ; - GuardedIntAddTest::add@2 (line 27)
                                                ; - java.lang.invoke.MethodHandle::invokeExact@14
                                                ; - Gen0::invokedynamic@0
  0x00007f785ca29722: add    $0x10,%rsp
  0x00007f785ca29726: pop    %rbp
  0x00007f785ca29727: test   %eax,0x5b2c8d3(%rip)        # 0x00007f7862556000
                                                ;   {poll_return}
  0x00007f785ca2972d: retq
 */
}
