package org.eap.time

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import org.eap.PluginLogger
import org.eap.Utils
import org.eap.asm.AndroidLogCreator
import org.eap.asm.ConsumeMethodTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

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
        final ConsumeMethodTransform consumeMethodTransform = new ConsumeMethodTransform();
        consumeMethodTransform.setExcludeClasses("org.eap.time.consume.AndroidLogger");

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        String name = file.name
                        if (name.endsWith(".class") && !name.startsWith("R\$") && !"R.class".equals(name)
                                && !"BuildConfig.class".equals(name)) {
                            // 不统计R.class和R$drawable.class这类的资源的映射
                            byte[] bytes = consumeMethodTransform.transform(file.bytes)
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
                def jarName = jarInput.name.substring(0, jarInput.name.length() - 4)
                File dest = outputProvider.getContentLocation(jarName + "_dest", jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                PluginLogger.info("scan jar " + dest.absolutePath)
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
                        body = consumeMethodTransform.transform(body)
                    }
                    jos.write(body, 0, body.length)
                }
                jos.finish();
                jos.close();
                jarFile.close();
            }
        }

        // copy androidLog
        AndroidLogCreator androidLogCreator = new AndroidLogCreator();
        File dest = outputProvider.getContentLocation("android_log",
                Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES),
                Sets.immutableEnumSet(QualifiedContent.Scope.EXTERNAL_LIBRARIES),
                Format.JAR)
        PluginLogger.info("############## androidLog jar ${dest.getAbsolutePath()} #############");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(dest))
        jos.putNextEntry(new JarEntry(androidLogCreator.getClassPath().replace(".", "/")+".class"))
        byte[] body = androidLogCreator.createAndroidLog()
        jos.write(body, 0, body.length)
        jos.finish();
        jos.close();
    }
}

