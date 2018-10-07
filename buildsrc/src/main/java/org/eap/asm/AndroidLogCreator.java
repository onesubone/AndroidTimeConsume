package org.eap.asm;

import org.eap.PluginLogger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AndroidLogCreator {
    private final String TAG = "EAP.MethodConsume";

    public String getClassPath() {
        return "org.eap.time.consume.AndroidLogger";
    }

    public byte[] createAndroidLog() {
        PluginLogger.debug("############## AndroidLogCreator generateClass #############");
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        PluginLogger.debug("############## AndroidLogCreator generate super class #############");
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, getClassPath().replace(".", "/"), null, "java/lang/Object", null);

        PluginLogger.debug("############## AndroidLogCreator generate constructor method ############# ");
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        PluginLogger.debug("############## AndroidLogCreator generate debug method ############# ");
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "debug", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(TAG);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        PluginLogger.debug("############## AndroidLogCreator generate d method ############# ");
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "d", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(TAG);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        PluginLogger.debug("############## AndroidLogCreator generate info method ############# ");
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "info", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(TAG);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        PluginLogger.debug("############## AndroidLogCreator generate i method ############# ");
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "i", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(TAG);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
