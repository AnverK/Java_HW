package ru.ifmo.rain.khusainov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class MyImplementor implements Impler {

    private static final String TAB = "    ";
    private String CLASS_NAME;

    private Path getFilePath(Class<?> token, Path root) throws IOException {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar);
        }
        Files.createDirectories(root);
        return root.resolve(CLASS_NAME + ".java");
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Passed arguments are incorrect");
        }

        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Class token is incorrect");
        }

        CLASS_NAME = token.getSimpleName() + "Impl";

        try (
                Writer writer = Files.newBufferedWriter(getFilePath(token, root), StandardCharsets.UTF_8)
        ) {
            writeClassFile(token, writer);
        } catch (IOException e) {
            throw new ImplerException(e);
        }

    }

    private void writeClassFile(Class<?> token, Writer writer) throws IOException, ImplerException {
        writePackage(token, writer);
        writeClassName(token, writer);
        writeConstructors(token, writer);
        writeMethods(token, writer);
        writer.write("}\n");
    }


    private void writePackage(Class<?> token, Writer writer) throws IOException {
        if (token.getPackage() != null) {
            writer.write("package " + token.getPackage().getName() + ";\n\n");
        }
    }

    private void writeClassName(Class<?> token, Writer writer) throws IOException {
        writer.write(
                Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE)
                        + " class "
                        + CLASS_NAME
                        + (token.isInterface() ? " implements " : " extends ")
                        + token.getSimpleName()
                        + " {\n\n"
        );
    }

    private void writeConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        boolean hasNonPrivateConstructors = false;
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            hasNonPrivateConstructors = true;
            writeExecutable(constructor, writer);
            writeConstructorImpl(constructor, writer);
        }
        if (!hasNonPrivateConstructors && !token.isInterface()) {
            throw new ImplerException("Has no non-private constructor");
        }
    }

    private void writeExecutable(Executable executable, Writer writer) throws IOException {
        writer.write(TAB
                + Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)
                + " "
                + (executable instanceof Constructor ? "" : ((Method) executable).getReturnType().getCanonicalName() + " ")
                + (executable instanceof Constructor ? CLASS_NAME : executable.getName())
                + "("
        );

        Parameter[] parameters = executable.getParameters();
        if (parameters.length == 0) {
            writer.write(")");
        }
        for (int i = 0; i < parameters.length; i++) {
            writer.write(parameters[i].getType().getCanonicalName() + " " + parameters[i].getName());
            writer.write((i == parameters.length - 1 ? ")" : ", "));
        }

        if (executable.getExceptionTypes().length == 0) {
            return;
        }
        writer.write(" throws ");
        for (Class<?> exception : executable.getExceptionTypes()) {
            writer.write(exception.getCanonicalName() + " ");
        }
    }

    private void writeConstructorImpl(Constructor<?> constructor, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("super(");
        Parameter[] parameters = constructor.getParameters();
        if (parameters.length == 0) {
            writer.write(");");
        }
        for (int i = 0; i < parameters.length; i++) {
            writer.write(parameters[i].getName());
            writer.write((i == parameters.length - 1 ? ");" : ", "));
        }
        writer.write("\n" + TAB + "}\n\n");
    }

    private void writeMethods(Class<?> token, Writer writer) throws IOException {
        Set<MethodWrapper> set = new HashSet<>();

        for (Method method : token.getMethods()) {
            set.add(new MethodWrapper(method));
        }

        Class<?> ancestor = token;
        while (ancestor != null && !ancestor.equals(Object.class)) {
            for (Method method : ancestor.getDeclaredMethods()) {
                set.add(new MethodWrapper(method));
            }
            ancestor = ancestor.getSuperclass();
        }

        for (MethodWrapper wrapper : set) {
            if (!Modifier.isAbstract(wrapper.getMethod().getModifiers())) {
                continue;
            }
            writeExecutable(wrapper.getMethod(), writer);
            writeMethodImpl(wrapper.getMethod(), writer);
        }
    }

    private class MethodWrapper {
        private final Method method;

        Method getMethod() {
            return method;
        }

        MethodWrapper(Method method) {
            this.method = method;
        }

        @Override
        public int hashCode() {
            Parameter[] parameters = method.getParameters();
            int hashCode = Integer.hashCode(parameters.length) * 43 + method.getName().hashCode();
            for (Parameter parameter : parameters) {
                hashCode = hashCode * 43 + parameter.toString().hashCode();
            }
            return hashCode;
        }

        private boolean parametersEquals(Parameter[] a, Parameter[] b) {
            if (a.length != b.length) {
                return false;
            }
            for (int i = 0; i < a.length; i++) {
                if (!a[i].toString().equals(b[i].toString())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodWrapper)) {
                return false;
            }
            MethodWrapper that = (MethodWrapper) obj;
            return this.method.getName().equals(that.method.getName()) && parametersEquals(method.getParameters(), that.method.getParameters());
        }
    }


    private void writeMethodImpl(Method method, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("return" + getDefaultReturnType(method.getReturnType()) + ";");
        writer.write("\n" + TAB + "}\n\n");
    }

    private String getDefaultReturnType(Class<?> type) {
        if (type.equals(void.class)) {
            return "";
        } else if (type.equals(boolean.class)) {
            return " true";
        } else if (type.isPrimitive()) {
            return " 0";
        } else {
            return " null";
        }
    }
}
