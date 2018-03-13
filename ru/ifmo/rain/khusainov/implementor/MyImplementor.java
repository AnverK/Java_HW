package ru.ifmo.rain.khusainov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.Tester;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MyImplementor implements Impler {

    private static final String TAB = "    ";
    private static String CLASS_NAME;

    private static Path getFilePath(Class<?> token, Path root) throws IOException {
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

        if (token.isPrimitive() || token.isArray() || token == Enum.class || token.isAnnotation() || Modifier.isFinal(token.getModifiers())) {
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

    private static void writeClassFile(Class<?> token, Writer writer) throws IOException, ImplerException {
        writePackage(token, writer);
        writeClassName(token, writer);
        writeConstructors(token, writer);
        writeMethods(token, writer);
        writer.write("}\n");
    }


    private static void writePackage(Class<?> token, Writer writer) throws IOException {
//        writer.write(token.getPackage().toString() + ";\n\n");
        if (token.getPackage() != null) {
            writer.write("package " + token.getPackage().getName() + ";\n\n");
        }
    }

    private static void writeClassName(Class<?> token, Writer writer) throws IOException {
        writer.write(
                Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE)
                        + " class "
                        + CLASS_NAME
                        + (token.isInterface() ? " implements " : " extends ")
                        + token.getSimpleName()
                        + " {\n\n"
        );
//        System.out.println(Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE)
//                + " class "
//                + CLASS_NAME
//                + (token.isInterface() ? " implements " : " extends ")
//                + token.getSimpleName()
//                + " {\n\n"
//        );
    }

    private static void writeConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        boolean hasNonPrivateConstructors = false;
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            hasNonPrivateConstructors = true;
            writeAnnotations(constructor.getDeclaredAnnotations(), writer);
            writeExecutable(constructor, writer);
            writeConstructorImpl(constructor, writer);
        }
        if (!hasNonPrivateConstructors && !token.isInterface()) {
            throw new ImplerException("Has no any non-private constructors");
        }
    }

    private static void writeAnnotations(Annotation[] annotations, Writer writer) throws IOException {
        for (Annotation annotation : annotations) {
            writer.write(annotation.toString() + "\n");
        }
    }

    private static void writeExecutable(Executable executable, Writer writer) throws IOException {
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

    private static void writeConstructorImpl(Constructor<?> constructor, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("super(");
        Parameter[] parameters = constructor.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            writer.write(parameters[i].getName());
            writer.write((i == parameters.length - 1 ? ");" : ", "));
        }
        writer.write("\n" + TAB + "}\n\n");
    }

    private static void writeMethods(Class<?> token, Writer writer) throws IOException {
        for (Method method : token.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                continue;
            }
            writeAnnotations(method.getDeclaredAnnotations(), writer);
            writeExecutable(method, writer);
            writeMethodImpl(method, writer);
        }
    }

    private static void writeMethodImpl(Method method, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("return" + getDefaultReturnType(method.getReturnType()) + ";");
        writer.write("\n" + TAB + "}\n\n");
    }

    private static String getDefaultReturnType(Class<?> type) {
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

    public static class Test {
        void f(int a, Integer b, Tester[] c) {
            System.out.println("kek");
        }

        public static void main(String[] args) {
            System.out.println(Test.class.getDeclaredConstructors().length);
            for (Method method : Test.class.getDeclaredMethods()) {
                System.out.println(method.getName() + ":\n");
                //            for(Parameter parameter: method.getParameters()){
                //                System.out.println(parameter.getType().getCanonicalName() +" " + parameter);
                //            }
            }
        }
    }
}
