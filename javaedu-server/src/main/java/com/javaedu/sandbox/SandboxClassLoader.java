package com.javaedu.sandbox;

import java.util.Map;
import java.util.Set;

/**
 * Custom ClassLoader for loading compiled student code in an isolated environment.
 * Restricts access to dangerous packages.
 */
public class SandboxClassLoader extends ClassLoader {

    private static final Set<String> BLOCKED_PACKAGES = Set.of(
            "java.io",
            "java.nio",
            "java.net",
            "java.lang.reflect",
            "java.lang.invoke",
            "java.security",
            "javax.net",
            "sun.",
            "com.sun.",
            "jdk."
    );

    private static final Set<String> ALLOWED_IO_CLASSES = Set.of(
            "java.io.Serializable",
            "java.io.PrintStream",
            "java.io.InputStream",
            "java.io.OutputStream",
            "java.io.IOException"
    );

    private final Map<String, byte[]> classes;

    public SandboxClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
        super(parent);
        this.classes = classes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isBlockedPackage(name) && !isAllowedClass(name)) {
            throw new ClassNotFoundException("Access to class " + name + " is not allowed in sandbox");
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }

            if (classes.containsKey(name)) {
                byte[] classData = classes.get(name);
                loadedClass = defineClass(name, classData, 0, classData.length);
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }

            return super.loadClass(name, resolve);
        }
    }

    private boolean isBlockedPackage(String className) {
        for (String blocked : BLOCKED_PACKAGES) {
            if (className.startsWith(blocked)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedClass(String className) {
        return ALLOWED_IO_CLASSES.contains(className);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] classData = classes.get(name);
        if (classData != null) {
            return defineClass(name, classData, 0, classData.length);
        }
        throw new ClassNotFoundException(name);
    }
}
