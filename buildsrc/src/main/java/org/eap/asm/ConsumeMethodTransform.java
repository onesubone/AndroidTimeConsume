package org.eap.asm;

import org.eap.PluginLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConsumeMethodTransform {
    private final List<String> excludeClasses = new LinkedList<>();

    public void setExcludeClasses(String... excludeClasses) {
        if (null == excludeClasses) {
            return;
        }
        Collections.addAll(this.excludeClasses, excludeClasses);
    }

    private static boolean match(String clazz, List<String> excludeClasses) {
        if (null == excludeClasses) {
            return false;
        }
        for (String ex : excludeClasses) {
            if (clazz.startsWith(ex)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnSupportAcc(int acc) {
        return (acc & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ENUM | Opcodes.ACC_SUPER
                | Opcodes.ACC_ANNOTATION | Opcodes.ACC_BRIDGE | Opcodes.ACC_MANDATED | Opcodes.ACC_STRICT
                | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VARARGS | Opcodes.ACC_SYNTHETIC)) != 0;
    }

    public byte[] transform(byte[] classBody) {
        ClassReader cr = new ClassReader(classBody);
        String clazz = cr.getClassName().replace("/", ".");
        if (match(clazz, excludeClasses)) {
            return classBody;
        }
        PluginLogger.info(cr.getClassName());
        Map<String, int[]> stacks = new HashMap<>();
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("<init>") || isUnSupportAcc(access)) { // 构造函数和void返回类型函数不进行任何操作
                    return visitor;
                }
                return new MethodVisitor(Opcodes.ASM6, visitor) {
                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        stacks.put(name + desc + signature, new int[]{maxStack, maxLocals});
                        super.visitMaxs(maxStack, maxLocals);
                    }
                };
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        // work now
        cr = new ClassReader(classBody);
        ClassWriter cw = new ClassWriter(cr, 0);
        cv = new ClassVisitor(Opcodes.ASM6, cw) {
            @Override
            public MethodVisitor visitMethod(int access,
                                             final String name, String desc, String signature, String[] exceptions) {
                final MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("<init>") || isUnSupportAcc(access)) { // 构造函数和void返回类型函数不进行任何操作
                    return visitor;
                }
                int[] localStacks = stacks.get(name + desc + signature);
                if (null == localStacks || localStacks.length != 2) {
                    return visitor;
                }
                return new MethodVisitor(Opcodes.ASM6, visitor) {
                    private int maxStackDepth = 0;

                    @Override
                    public void visitCode() {
                        onMethodEnter();
                        super.visitCode();
                    }

                    private void onMethodEnter() {
                        PluginLogger.info("====== 开始插入方法 = " + name);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false);
                        mv.visitVarInsn(Opcodes.LSTORE, localStacks[1]); // 会自动从栈中pop
                        maxStackDepth = 2;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        PluginLogger.info("====== visitInsn = " + opcode);
                        if (opcode == Opcodes.RETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.IRETURN
                                || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1]);
                            mv.visitInsn(Opcodes.LSUB);
                            mv.visitVarInsn(Opcodes.LSTORE, localStacks[1]);

                            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                            mv.visitLdcInsn(name + ": ");
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1]);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/eap/time/consume/AndroidLogger", "debug", "(Ljava/lang/String;)V", false);

                            maxStackDepth = 4;
                        }
                        super.visitInsn(opcode);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        PluginLogger.info("====== visitMaxs = " + name + ", " + maxStack + ", " + maxLocals);
                        mv.visitMaxs(maxStack + maxStackDepth, maxLocals + 2);
                    }
                };
            }
        };
        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
