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

//    public boolean body() {
//        return Boolean.FALSE.booleanValue();
//    }

    public boolean body() {
        return bool1() && bool2();
    }

    private Boolean bool2() {
        return true;
    }

    private Boolean bool1() {
        return false;
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
/*

  # {method} 'body' '()Z' in 'BoxedBooleanInlineTest'
  #           [sp+0x20]  (sp of caller)
  0x00007fadf24412c0: mov    0x8(%rsi),%r10d
  0x00007fadf24412c4: shl    $0x3,%r10
  0x00007fadf24412c8: cmp    %r10,%rax
  0x00007fadf24412cb: jne    0x00007fadf24138a0  ;   {runtime_call}
  0x00007fadf24412d1: xchg   %ax,%ax
  0x00007fadf24412d4: nopl   0x0(%rax,%rax,1)
  0x00007fadf24412dc: xchg   %ax,%ax
[Verified Entry Point]
  0x00007fadf24412e0: push   %rbp
  0x00007fadf24412e1: sub    $0x10,%rsp
  0x00007fadf24412e5: nop                       ;*synchronization entry
                                                ; - BoxedBooleanInlineTest::body@-1 (line 21)
  0x00007fadf24412e6: mov    $0x7d6602fe0,%r10  ;   {oop(a 'java/ang/lass' = 'java/ang/oolean')}
  0x00007fadf24412f0: mov    0x74(%r10),%r8d    ;*getstatic FALSE
                                                ; - java.lang.Boolean::valueOf@10 (line 149)
                                                ; - BoxedBooleanInlineTest::bool1@1 (line 29)
                                                ; - BoxedBooleanInlineTest::body@1 (line 21)
  0x00007fadf24412f4: movzbl 0xc(%r12,%r8,8),%r11d
  0x00007fadf24412fa: test   %r11d,%r11d
  0x00007fadf24412fd: jne    0x00007fadf244130d  ;*iconst_0
                                                ; - BoxedBooleanInlineTest::body@24 (line 21)
  0x00007fadf24412ff: xor    %eax,%eax          ;*ireturn
                                                ; - BoxedBooleanInlineTest::body@25 (line 21)
  0x00007fadf2441301: add    $0x10,%rsp
  0x00007fadf2441305: pop    %rbp
  0x00007fadf2441306: test   %eax,0x5b42cf4(%rip)        # 0x00007fadf7f84000
                                                ;   {poll_return}
  0x00007fadf244130c: retq
  0x00007fadf244130d: mov    0x70(%r10),%r10d   ;*getstatic TRUE
                                                ; - java.lang.Boolean::valueOf@4 (line 149)
                                                ; - BoxedBooleanInlineTest::bool2@1 (line 25)
                                                ; - BoxedBooleanInlineTest::body@11 (line 21)
  0x00007fadf2441311: movzbl 0xc(%r12,%r10,8),%r11d
  0x00007fadf2441317: test   %r11d,%r11d
  0x00007fadf244131a: je     0x00007fadf24412ff  ;*ifeq
                                                ; - BoxedBooleanInlineTest::body@17 (line 21)
  0x00007fadf244131c: mov    $0x1,%eax
  0x00007fadf2441321: jmp    0x00007fadf2441301

 */
}
