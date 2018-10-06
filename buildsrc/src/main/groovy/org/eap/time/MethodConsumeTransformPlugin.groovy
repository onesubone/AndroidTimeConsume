package org.eap.time

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.eap.PluginLogger
import org.eap.Utils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class MethodConsumeTransformPlugin extends Transform implements Plugin<Project> {
    void apply(Project project) {
        project.android.registerTransform(this)
    }

    @Override
    String getName() {
        return "MethodConsume"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {
        outputProvider.deleteAll()

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        String name = file.name
                        if (name.endsWith(".class") && !name.startsWith("R\$") && !"R.class".equals(name)
                                && !"BuildConfig.class".equals(name)) {
                            // 不统计R.class和R$drawable.class这类的资源的映射
                            byte[] bytes = transform(file.bytes)
                            File destFile = new File(file.parentFile.absoluteFile, name)
                            FileOutputStream fileOutputStream = new FileOutputStream(destFile)
                            fileOutputStream.write(bytes)
                            fileOutputStream.close()
                        }
                    }
                }

                // 坐等遍历class并被ASM操作
                File dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            input.jarInputs.findAll { it.file.getAbsolutePath().endsWith(".jar") }.each { JarInput jarInput ->
                PluginLogger.info("scan jar " + jarInput.getName())
                def jarName = jarInput.name.substring(0, jarInput.name.length() - 4)
                File dest = outputProvider.getContentLocation(jarName + "_dest", jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
                JarOutputStream jos = new JarOutputStream(new FileOutputStream(dest))

                JarFile jarFile = new JarFile(jarInput.file)
                Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                while (null != jarEntryEnumeration && jarEntryEnumeration.hasMoreElements()) {
                    JarEntry entry = jarEntryEnumeration.nextElement();
                    if (null == entry) {
                        continue
                    }
                    JarEntry newEntry = new JarEntry(entry.getName());
                    jos.putNextEntry(newEntry)
                    if (entry.isDirectory()) {
                        continue
                    }
                    InputStream inputStream = jarFile.getInputStream(entry)
                    byte[] body = Utils.inputStreamToBytes(inputStream)
                    if (entry.getName().endsWith(".class")) {
                        body = transform(body)
                    }
                    jos.write(body, 0, body.length)
                }
                jos.finish();
                jos.close();
                jarFile.close();
            }
        }
    }

    byte[] transform(byte[] classBody) {
        ClassReader cr = new ClassReader(classBody)
        def clazz = cr.getClassName().replace("/", ".")
        if (clazz.startsWith("org.eap.time.consume.AndroidLogger")) {
            return classBody
        }
        PluginLogger.info(cr.getClassName())
        Map<String, int[]> stacks = new HashMap<>();
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6) {
            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions)
                if (name.equals("<init>")
                        || (access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ENUM | Opcodes.ACC_SUPER | Opcodes.ACC_ANNOTATION | Opcodes.ACC_BRIDGE | Opcodes.ACC_MANDATED | Opcodes.ACC_STRICT | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VARARGS | Opcodes.ACC_SYNTHETIC)) != 0) {
                    // 构造函数和void返回类型函数不进行任何操作
                    return visitor
                }
                return new MethodVisitor(Opcodes.ASM6, visitor) {
                    @Override
                    void visitMaxs(int maxStack, int maxLocals) {
                        stacks.put(name + desc + signature, [maxStack, maxLocals])
                        super.visitMaxs(maxStack, maxLocals)
                    }
                };
            }
        }
        cr.accept(cv, ClassReader.EXPAND_FRAMES)

        // work now
        cr = new ClassReader(classBody)
        ClassWriter cw = new ClassWriter(cr, 0) // ClassWriter.COMPUTE_FRAMES
        cv = new ClassVisitor(Opcodes.ASM6, cw) {
            @Override
            MethodVisitor visitMethod(int access,
                                      final String name, String desc, String signature, String[] exceptions) {
                final MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions)
                if (name.equals("<init>")
                        || (access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ENUM | Opcodes.ACC_SUPER | Opcodes.ACC_ANNOTATION | Opcodes.ACC_BRIDGE | Opcodes.ACC_MANDATED | Opcodes.ACC_STRICT | Opcodes.ACC_TRANSIENT | Opcodes.ACC_VARARGS | Opcodes.ACC_SYNTHETIC)) != 0) {
                    // 构造函数和void返回类型函数不进行任何操作
                    return visitor
                }
                int[] localStacks = stacks.get(name + desc + signature)
                if (null == localStacks || localStacks.length <= 0) {
                    return visitor
                }
                MethodVisitor res = new MethodVisitor(Opcodes.ASM6, visitor) {
                    private int maxStackDepth = 0;

                    @Override
                    public void visitCode() {
                        PluginLogger.info("====== visitCode = ");
                        onMethodEnter();
                        super.visitCode();
                    }

                    private void onMethodEnter() {
                        PluginLogger.info("====== 开始插入方法 = " + name);
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false);
                        mv.visitVarInsn(Opcodes.LSTORE, localStacks[1]) // 会自动从栈中pop

//                        mv.visitLdcInsn(Long.valueOf(1111111))
//                        mv.visitVarInsn(Opcodes.LSTORE, localStacks[1]) // 会自动从栈中pop
//                        mv.visitLdcInsn(Long.valueOf(22222222))
//                        mv.visitVarInsn(Opcodes.LSTORE, localStacks[1]) // 会自动从栈中pop
//                        mv.visitInsn(Opcodes.LSUB)

                        maxStackDepth = 2;
                    }

                    @Override
                    void visitIntInsn(int opcode, int operand) {
                        super.visitIntInsn(opcode, operand)
                    }

                    @Override
                    void visitInsn(int opcode) {
                        PluginLogger.info("====== visitInsn = " + opcode);
                        if (opcode == Opcodes.RETURN) {
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1])
                            mv.visitInsn(Opcodes.LSUB)
                            mv.visitVarInsn(Opcodes.LSTORE, localStacks[1])

                            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                            mv.visitLdcInsn(name + ": ");
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1])
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/eap/time/consume/AndroidLogger", "debug", "(Ljava/lang/String;)V", false);
                            maxStackDepth = 4;
                        }
                        if (opcode == Opcodes.ARETURN || opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN
                                || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN) {
                            int storeOpcode = Opcodes.ASTORE, loadOpcode = Opcodes.ALOAD
                            switch (opcode) {
                                case Opcodes.ARETURN:
                                    storeOpcode = Opcodes.ASTORE
                                    loadOpcode = Opcodes.ALOAD
                                    break;
                                case Opcodes.IRETURN:
                                    storeOpcode = Opcodes.ISTORE
                                    loadOpcode = Opcodes.ILOAD
                                    break;
                                case Opcodes.LRETURN:
                                    storeOpcode = Opcodes.LSTORE
                                    loadOpcode = Opcodes.LLOAD
                                    break;
                                case Opcodes.FRETURN:
                                    storeOpcode = Opcodes.FSTORE
                                    loadOpcode = Opcodes.FLOAD
                                    break;
                                case Opcodes.DRETURN:
                                    storeOpcode = Opcodes.DSTORE
                                    loadOpcode = Opcodes.DLOAD
                                    break;
                            }

//                            mv.visitVarInsn(storeOpcode, 0)
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/SystemClock", "elapsedRealtime", "()J", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1])
                            mv.visitInsn(Opcodes.LSUB)
                            mv.visitVarInsn(Opcodes.LSTORE, localStacks[1])

                            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
                            mv.visitInsn(Opcodes.DUP);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                            mv.visitLdcInsn(name + ": ");
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                            mv.visitVarInsn(Opcodes.LLOAD, localStacks[1])
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/eap/time/consume/AndroidLogger", "debug", "(Ljava/lang/String;)V", false);

//                            mv.visitVarInsn(loadOpcode, 0)

                            maxStackDepth = 4;
                        }
                        super.visitInsn(opcode)
                    }

//                    @Override
//                    protected void onMethodExit(int opcode) {
//                        if (isInject) {
//                            // NeacyCostManager.addEndTime("xxxx", System.currentTimeMillis());
//                            mv.visitLdcInsn(methodName);
//                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
//                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "neacy/router/NeacyCostManager", "addEndTime", "(Ljava/lang/String;J)V", false);
//
//                            // NeacyCostManager.startCost("xxxx");
//                            mv.visitLdcInsn(methodName);
//                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "neacy/router/NeacyCostManager", "startCost", "(Ljava/lang/String;)V", false);
//
//                            NeacyLog.log("==== 插入结束 ====");
//                        }
//                    }

                    @Override
                    void visitMaxs(int maxStack, int maxLocals) {
                        PluginLogger.info("====== visitMaxs = " + name + ", " + maxStack + ", " + maxLocals);
//                        super.visitMaxs(maxStack + 2, maxLocals + 2)
                        mv.visitMaxs(maxStack + 4, maxLocals + 2)
                    }
                }
                return res
            }
        }
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }
}

