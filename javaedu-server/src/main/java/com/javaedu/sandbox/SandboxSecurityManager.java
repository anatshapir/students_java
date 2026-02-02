package com.javaedu.sandbox;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.Set;

/**
 * Custom SecurityManager that restricts dangerous operations in student code.
 * Blocks file I/O, network access, process execution, and class loading manipulation.
 */
@SuppressWarnings("removal")
public class SandboxSecurityManager extends SecurityManager {

    private static final Set<String> ALLOWED_RUNTIME_PERMISSIONS = Set.of(
            "accessDeclaredMembers",
            "getProtectionDomain"
    );

    private static final Set<String> ALLOWED_PROPERTY_READS = Set.of(
            "java.version",
            "java.vendor",
            "os.name",
            "line.separator",
            "file.separator",
            "path.separator"
    );

    private final ThreadGroup sandboxThreadGroup;

    public SandboxSecurityManager(ThreadGroup sandboxThreadGroup) {
        this.sandboxThreadGroup = sandboxThreadGroup;
    }

    private boolean isInSandbox() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        while (currentGroup != null) {
            if (currentGroup == sandboxThreadGroup) {
                return true;
            }
            currentGroup = currentGroup.getParent();
        }
        return false;
    }

    @Override
    public void checkPermission(Permission perm) {
        if (!isInSandbox()) {
            return;
        }

        String permName = perm.getName();

        if (perm instanceof FilePermission) {
            throw new SecurityException("File access is not allowed: " + permName);
        }

        if (perm instanceof SocketPermission) {
            throw new SecurityException("Network access is not allowed: " + permName);
        }

        if (perm instanceof RuntimePermission) {
            if (permName.startsWith("exitVM")) {
                throw new SecurityException("System.exit() is not allowed");
            }
            if (permName.equals("createClassLoader")) {
                throw new SecurityException("Creating class loaders is not allowed");
            }
            if (permName.equals("setSecurityManager")) {
                throw new SecurityException("Changing security manager is not allowed");
            }
            if (permName.startsWith("loadLibrary")) {
                throw new SecurityException("Loading native libraries is not allowed");
            }
            if (!ALLOWED_RUNTIME_PERMISSIONS.contains(permName)) {
                if (!permName.startsWith("accessClassInPackage")) {
                    throw new SecurityException("Permission denied: " + permName);
                }
            }
        }

        if (perm instanceof PropertyPermission) {
            PropertyPermission propPerm = (PropertyPermission) perm;
            if (propPerm.getActions().contains("write")) {
                throw new SecurityException("Writing system properties is not allowed");
            }
            if (!ALLOWED_PROPERTY_READS.contains(permName) && !permName.startsWith("java.")) {
                throw new SecurityException("Reading property not allowed: " + permName);
            }
        }
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        if (isInSandbox()) {
            throw new SecurityException("Executing external processes is not allowed");
        }
    }

    @Override
    public void checkExit(int status) {
        if (isInSandbox()) {
            throw new SecurityException("System.exit() is not allowed");
        }
    }

    @Override
    public void checkRead(String file) {
        if (isInSandbox()) {
            throw new SecurityException("File read is not allowed: " + file);
        }
    }

    @Override
    public void checkWrite(String file) {
        if (isInSandbox()) {
            throw new SecurityException("File write is not allowed: " + file);
        }
    }

    @Override
    public void checkDelete(String file) {
        if (isInSandbox()) {
            throw new SecurityException("File delete is not allowed: " + file);
        }
    }

    @Override
    public void checkConnect(String host, int port) {
        if (isInSandbox()) {
            throw new SecurityException("Network connections are not allowed");
        }
    }

    @Override
    public void checkListen(int port) {
        if (isInSandbox()) {
            throw new SecurityException("Listening on ports is not allowed");
        }
    }

    @Override
    public void checkAccept(String host, int port) {
        if (isInSandbox()) {
            throw new SecurityException("Accepting connections is not allowed");
        }
    }
}
