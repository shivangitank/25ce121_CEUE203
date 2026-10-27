package service;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Arrays;

import model.annotation.Audit;
import util.TransactionLog;

/**
 * Reflective audit logger. wrap() returns a dynamic proxy that implements
 * BankOperations: every call goes through invoke(), which runs the real
 * method and, if that method is marked @Audit, writes a line to the audit file.
 */
public final class AuditProxy implements InvocationHandler {
    private final BankOperations target;
    private final Path auditFile;

    private AuditProxy(BankOperations target, Path auditFile) {
        this.target = target;
        this.auditFile = auditFile;
    }

    public static BankOperations wrap(BankOperations target, Path auditFile) {
        return (BankOperations) Proxy.newProxyInstance(BankOperations.class.getClassLoader(),
                new Class<?>[] {BankOperations.class}, new AuditProxy(target, auditFile));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method implementation = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        boolean audited = implementation.isAnnotationPresent(Audit.class);
        long start = System.nanoTime();
        try {
            Object result = method.invoke(target, args);
            if (audited) {
                record(method, args, "OK", start);
            }
            return result;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();          // the exception the real method threw
            if (audited) {
                record(method, args, "FAILED " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), start);
            }
            throw cause;
        }
    }

    private void record(Method method, Object[] args, String outcome, long startNanos) {
        long millis = (System.nanoTime() - startNanos) / 1_000_000;
        String arguments = args == null ? "" : Arrays.toString(args);
        if (arguments.length() > 120) {
            arguments = arguments.substring(0, 117) + "...";   // batches can be long
        }
        String line = TransactionLog.now() + " " + method.getName()
                + arguments + " -> " + outcome + " (" + millis + " ms)";
        try {
            TransactionLog.append(auditFile, line);
        } catch (IOException e) {
            System.err.println("Audit log unavailable: " + e.getMessage());
        }
    }
}
