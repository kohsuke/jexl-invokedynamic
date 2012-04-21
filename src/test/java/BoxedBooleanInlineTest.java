import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * @author Kohsuke Kawaguchi
 */
public class BoxedBooleanInlineTest extends TestCase {
    public void test1() throws Exception {
        Method m = getClass().getMethod("body");
        for (int i=0; i<1000000; i++) {
            m.invoke(this);
        }
    }

    public boolean body() {
        return Boolean.FALSE.booleanValue();
    }

/*
    Boolean.FALSE.booleanValue() fails to optimize to "false" constant:

[Disassembling for mach='i386:x86-64']
[Entry Point]
[Constants]
  # {method} 'body' '()Z' in 'BoxedBooleanInlineTest'
  #           [sp+0x20]  (sp of caller)
  0x00007ff944429980: mov    0x8(%rsi),%r10d
  0x00007ff944429984: shl    $0x3,%r10
  0x00007ff944429988: cmp    %r10,%rax
  0x00007ff94442998b: jne    0x00007ff9443fa8a0  ;   {runtime_call}
  0x00007ff944429991: xchg   %ax,%ax
  0x00007ff944429994: nopl   0x0(%rax,%rax,1)
  0x00007ff94442999c: xchg   %ax,%ax
[Verified Entry Point]
  0x00007ff9444299a0: push   %rbp
  0x00007ff9444299a1: sub    $0x10,%rsp
  0x00007ff9444299a5: nop                       ;*synchronization entry
                                                ; - BoxedBooleanInlineTest::body@-1 (line 17)
  0x00007ff9444299a6: mov    $0x7d6602fe0,%r10  ;   {oop(a 'java/ang/lass' = 'java/ang/oolean')}
  0x00007ff9444299b0: mov    0x74(%r10),%r11d   ;*getstatic FALSE
                                                ; - BoxedBooleanInlineTest::body@0 (line 17)
  0x00007ff9444299b4: movzbl 0xc(%r12,%r11,8),%eax  ;*getfield value
                                                ; - java.lang.Boolean::booleanValue@1 (line 131)
                                                ; - BoxedBooleanInlineTest::body@3 (line 17)
  0x00007ff9444299ba: add    $0x10,%rsp
  0x00007ff9444299be: pop    %rbp
  0x00007ff9444299bf: test   %eax,0x5b4163b(%rip)        # 0x00007ff949f6b000
                                                ;   {poll_return}
  0x00007ff9444299c5: retq
 */
}
