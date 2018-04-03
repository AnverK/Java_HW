package ru.ifmo.rain.khusainov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * A class that implements classes and interfaces.
 */
public class Implementor implements Impler, JarImpler {


    /**
     * Default gap called 'tab' represented by four spaces
     */
    private static final String TAB = "    ";

    /**
     * Create instance of {@link Implementor}
     */
    public Implementor() {

    }

    /**
     * Returns the {@link java.nio.file.Path} to the implementation of <tt>token</tt> relative from the <tt>root</tt>.
     * Implementation of <tt>token</tt> has suffix <tt>Impl</tt>. Also the path has suffix dependent on <tt>end</tt>.
     *
     * @param token type token to create the implementation's path to
     * @param root  root directory
     * @param end   if true then add <tt>.java</tt> to the suffix of path
     *              if false then add <tt>.class</tt> to the suffix of path
     * @return path to the implementation of <tt>token</tt> starting from <tt>root</tt>
     */
    private static Path getFilePath(Class<?> token, Path root, boolean end) {
        root = getFileDirectory(token, root);
        return root.resolve(getImplClassName(token) + (end ? ".java" : ".class"));
    }

    /**
     * Creates the {@link Path} to the <tt>token</tt> relative from the <tt>root</tt>.
     * The return value is same as call <tt>getFilePath(token, root, true)</tt> but if the path does not exist
     * the directories in it are created.
     *
     * @param token type token to create the path to
     * @param root  root directory
     * @return path to the implementation of <tt>token</tt> starting from <tt>root</tt>.
     * @throws IOException if an I/O error occurs during the creating directories
     */
    private static Path createJavaDirectory(Class<?> token, Path root) throws IOException {
        root = getFileDirectory(token, root);
        Files.createDirectories(root);
        return root.resolve(getImplClassName(token) + ".java");
    }

    /**
     * Return the {@link Path} to the directory of implementation of <tt>token</tt> starting in <tt>root</tt>.
     *
     * @param token type token to create the path of directory to
     * @param root  root directory
     * @return the {@link Path} to the directory of implementation of <tt>token</tt> starting in <tt>root</tt>.
     */
    private static Path getFileDirectory(Class<?> token, Path root) {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar);
        }
        return root;
    }

    /**
     * Returns name of class which is an implementation of <tt>token</tt>.
     * This method returns <tt>token.getSimpleName()</tt> with suffix <tt>Impl</tt>
     *
     * @param token type token to create implementation for
     * @return the name of implementation of <tt>token</tt>
     */
    private static String getImplClassName(Class<?> token) {
        return toUnicode(token.getSimpleName()) + "Impl";
    }

    /**
     * Produces code implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code is placed in the correct subdirectory of the specified
     * <tt>root</tt> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} goes to <tt>$root/java/util/ListImpl.java</tt>
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be generated
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Passed arguments are incorrect");
        }

        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Class token is incorrect");
        }

        try (
                Writer writer = Files.newBufferedWriter(createJavaDirectory(token, root))
        ) {
            writeClassFile(token, writer);
        } catch (IOException e) {
            throw new ImplerException(e);
        }

    }

    /**
     * Writes implementation of <tt>token</tt> by <tt>writer</tt>
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException     if an I/O error occurs
     * @throws ImplerException if implementation cannot be generated
     */
    private void writeClassFile(Class<?> token, Writer writer) throws IOException, ImplerException {
        writePackage(token, writer);
        writeClassName(token, writer);
        writeConstructors(token, writer);
        writeMethods(token, writer);
        writer.write("}\n");
    }

    /**
     * Writes information about <tt>token</tt>'s implementation package by <tt>writer</tt>.
     * If <tt>token.getPackage()</tt> is <tt>null</tt> then this method does nothing.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writePackage(Class<?> token, Writer writer) throws IOException {
        if (token.getPackage() != null) {
            writer.write("package " + toUnicode(token.getPackage().getName()) + ";\n\n");
        }
    }

    /**
     * Represent <tt>in</tt>'s chars to <tt>unicode escape</tt>.
     * If char code is not less than <tt>128</tt>. Otherwise char is not changed.
     *
     * @param in input {@link String}
     * @return string with <tt>unicode escapes</tt>
     */
    private static String toUnicode(String in) {
        StringBuilder b = new StringBuilder();

        for (char c : in.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else
                b.append(c);
        }

        return b.toString();
    }

    /**
     * Writes information about <tt>token</tt>'s implementation name by <tt>writer</tt>.
     * Implementation class have same modifiers as <tt>token</tt> except <tt>abstract</tt> and <tt>interface</tt>
     * (implementation does not have these modifiers).
     * Also it writes the <tt>'{'</tt> and two <tt>'\n'</tt> symbols in the end.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeClassName(Class<?> token, Writer writer) throws IOException {
        writer.write(
                Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE)
                        + " class "
                        + getImplClassName(token)
                        + (token.isInterface() ? " implements " : " extends ")
                        + toUnicode(token.getSimpleName())
                        + " {\n\n"
        );
    }

    /**
     * Writes implementation of constructors of <tt>token</tt>'s implementation by <tt>writer</tt>.
     * <tt>Private</tt> constructors are ignored. Also <tt>token</tt> have to have at least one <tt>non-private</tt>
     * <tt>declared constructor</tt> or it has to be an interface. Otherwise, this method throws an <tt>ImplerException</tt>.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException     if an I/O error occurs
     * @throws ImplerException if <tt>token</tt> is not an interface and it has only <tt>private</tt> <tt>declared constructors</tt>
     */
    private void writeConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        boolean hasNonPrivateConstructors = false;
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            hasNonPrivateConstructors = true;
            writeExecutable(constructor, writer, token);
            writeConstructorImpl(constructor, writer);
        }
        if (!hasNonPrivateConstructors && !token.isInterface()) {
            throw new ImplerException("Has no non-private constructor");
        }
    }

    /**
     * Writes signature of implementation of <tt>executable</tt> by <tt>writer</tt>.
     * Signature includes also what <tt>executable</tt> throws. This method does not write '{' in the end however it
     * writes space after list of exceptions. Modifiers in the result signature are same with <tt>executable</tt>'s modifiers
     * except <tt>abstract</tt> and <tt>transient</tt> (implementation does not have these modifiers).     *
     *
     * @param executable {@link Executable} of <tt>token</tt> which this method declares
     * @param writer     a writer
     * @param token      type token to create implementation for
     * @throws IOException if an I/O error occurs
     */
    private void writeExecutable(Executable executable, Writer writer, Class<?> token) throws IOException {
        writer.write(TAB
                + Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)
                + " "
                + (executable instanceof Constructor ? "" : toUnicode(((Method) executable).getReturnType().getCanonicalName()) + " ")
                + (executable instanceof Constructor ? getImplClassName(token) : toUnicode(executable.getName()))
                + "("
        );

        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            writer.write((i == 0 ? "" : ", ") + toUnicode(parameters[i].getType().getCanonicalName()) + " " + toUnicode(parameters[i].getName()));
        }
        writer.write(")");

        Class<?> exceptions[] = executable.getExceptionTypes();
        for (int i = 0; i < exceptions.length; i++) {
            writer.write((i == 0 ? " throws " : ", ") + toUnicode(exceptions[i].getCanonicalName()));
        }
    }

    /**
     * Writes body of <tt>constructor</tt> by <tt>writer</tt>.
     * The body starts with '{' and ends with '}'. The implementation of constructor is calling <tt>super()</tt> with
     * arguments of the <tt>constructor</tt>.
     *
     * @param constructor {@link Constructor} which this method defines
     * @param writer      a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeConstructorImpl(Constructor<?> constructor, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("super(");
        Parameter[] parameters = constructor.getParameters();
        if (parameters.length == 0) {
            writer.write(");");
        }
        for (int i = 0; i < parameters.length; i++) {
            writer.write(toUnicode(parameters[i].getName()));
            writer.write((i == parameters.length - 1 ? ");" : ", "));
        }
        writer.write("\n" + TAB + "}\n\n");
    }

    /**
     * Writes implementation of methods of <tt>token</tt>'s implementation by <tt>writer</tt>.
     * This method implements only abstract method of the <tt>token</tt> and its ancestors if they were not implemented earlier.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeMethods(Class<?> token, Writer writer) throws IOException {
        Set<MethodWrapper> set = new HashSet<>();
        getAbstractMethods(token.getMethods(), set);
        Class<?> ancestor = token;
        while (ancestor != null) {
            getAbstractMethods(token.getDeclaredMethods(), set);
            ancestor = ancestor.getSuperclass();
        }
        for (MethodWrapper wrapper : set) {
            writeExecutable(wrapper.getMethod(), writer, token);
            writeMethodImpl(wrapper.getMethod(), writer);
        }
    }

    /**
     * Put <tt>abstract</tt> methods from <tt>methods</tt> in <tt>set</tt>.
     *
     * @param methods array of methods
     * @param set     contains all abstract methods of <tt>methods</tt> after calling this method
     */
    private void getAbstractMethods(Method[] methods, Set<MethodWrapper> set) {
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                set.add(new MethodWrapper(method));
            }
        }
    }

    /**
     * Wrapper of {@link Method}
     */
    private static class MethodWrapper {
        /**
         * instance of {@link Method} wrapped method
         */
        private final Method method;

        /**
         * Returns wrapped method.
         *
         * @return wrapped method
         */
        Method getMethod() {
            return method;
        }

        /**
         * Construct {@link MethodWrapper} which wraps <tt>method</tt>.
         *
         * @param method wtapped method
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Returns hash code of wrapped method. It depends only on its name and parameters.
         *
         * @return hash code of wrapped method
         */
        @Override
        public int hashCode() {
            Parameter[] parameters = method.getParameters();
            int hashCode = Integer.hashCode(parameters.length) * 43 + method.getName().hashCode();
            for (Parameter parameter : parameters) {
                hashCode = hashCode * 43 + parameter.toString().hashCode();
            }
            return hashCode;
        }

        /**
         * Check if {@link Object} <tt>obj</tt> is equal to <tt>this</tt>. If obj is null or it is not instance of MethodWrapper
         * then <tt>obj</tt> is not equal to <tt>this</tt>. Otherwise, if <tt>obj</tt> and <tt>this</tt> have equal <tt>name</tt>,
         * <tt>parameter types</tt> and <tt>return type</tt> then they are equal. Otherwise, they are not.
         *
         * @param obj {@link Object} comparing to <tt>this</tt>
         * @return true if <tt>obj</tt> and <tt>this</tt> are equal and false otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof MethodWrapper) {
                MethodWrapper other = (MethodWrapper) obj;
                return method.getName().equals(other.method.getName())
                        && Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                        && method.getReturnType().equals(other.method.getReturnType());
            }
            return false;
        }
    }

    /**
     * Writes body of <tt>method</tt> by <tt>writer</tt>.
     * The body starts with '{' and ends with '}'. The implementation of method is returning <tt>defaultReturnType</tt>.
     *
     * @param method {@link Method} which this method defines
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     * @see #getDefaultReturnType(Class)
     */
    private void writeMethodImpl(Method method, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("return" + getDefaultReturnType(method.getReturnType()) + ";");
        writer.write("\n" + TAB + "}\n\n");
    }

    /**
     * Returns string with default value of <tt>type</tt>. If <tt>type</tt> is <tt>void</tt> it returns empty string.
     * Otherwise, if it is <tt>boolean</tt> then it returns <tt>" true"</tt>. Otherwise, if it is <tt>primitive</tt>
     * method returns <tt>" 0"</tt>. Otherwise, it returns <tt>" null"</tt>.
     *
     * @param type type which of default value method returns
     * @return string with default value of <tt>type</tt> with space in beginning (if <tt>type</tt> is not <tt>void</tt>)
     */
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

    /**
     * Compile class which has {@link Path} <tt>javaFileName</tt> relatively to <tt>root</tt>.
     *
     * @param root         root directory
     * @param javaFileName name of <tt>.java</tt> file for compiling
     * @throws ImplerException if compilation can not be done
     */
    private void compileClass(Path root, Path javaFileName) throws ImplerException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("Exception: compiler not found");
        }
        int returnCode = javaCompiler.run(null, null, null,
                javaFileName.toString(),
                "-cp", root + File.pathSeparator + System.getProperty("java.class.path")
        );
        if (returnCode != 0) {
            throw new ImplerException("Exception when compiling");
        }
    }

    /**
     * Creates and writes <tt>.jar</tt> file by {@link Path} of <tt>jarFile</tt> relatively to <tt>root</tt>.
     * Resulting <tt>.jar</tt> contains {@link Manifest} with attribute <tt>MANIFEST_VERSION</tt>. Also it contains compiled
     * <tt>.class</tt> file of <tt>token</tt>.
     *
     * @param token   type token which compiled class zipping in <tt>.jar</tt>
     * @param root    root directory
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException if creation .jar can not be done
     */
    private void writeJarFile(Class<?> token, Path root, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            Files.copy(getFilePath(token, root, false), writer);
        } catch (IOException e) {
            throw new ImplerException("Unable to create JAR file", e);
        }
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name is same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tmpDirectory = Paths.get("tmp");
        implement(token, tmpDirectory);
        compileClass(tmpDirectory, getFilePath(token, tmpDirectory, true));
        writeJarFile(token, tmpDirectory, jarFile);
        try {
            cleanTmp(tmpDirectory);
        } catch (IOException e) {
            throw new ImplerException("Unable to delete temporary directory", e);
        }
    }

    /**
     * Recursively deletes file or directory by <tt>path</tt>.
     *
     * @param path path to file or directory for deleting
     * @throws IOException if an I/O error occurs
     */
    private static void cleanTmp(Path path) throws IOException {
        Files.walkFileTree(path, new Deleter());
    }

    /**
     * Class for deleting directories recursively
     */
    private static class Deleter extends SimpleFileVisitor<Path> {

        /**
         * Initializes a new instance of this class.
         */
        public Deleter() {
        }

        /**
         * Deletes file by {@link Path}
         *
         * @param file       current file in file tree which should be deleted
         * @param attributes attributes of <tt>file</tt>
         * @return next file in file tree
         * @throws IOException if an I/O error occurs
         * @see FileVisitResult#CONTINUE
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Deletes directory by {@link Path}
         *
         * @param dir       current directory in file tree which should be deleted
         * @param exception {@code null} if the iteration of the directory completes without
         *                  an error; otherwise the I/O exception that caused the iteration
         *                  of the directory to complete prematurely
         * @return {@link FileVisitResult#CONTINUE
         * CONTINUE} if the directory iteration completes without an I/O exception;
         * otherwise this method re-throws the I/O exception that caused the iteration
         * of the directory to terminate prematurely.
         * @throws IOException if an I/O error occurs
         * @see FileVisitResult#CONTINUE
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }


    /**
     * Entry point of the program.
     * <p>
     * Usage:
     * <ul>
     * <li>{@code java -jar Implementor.jar -jar class-to-implement path-to-jar}</li>
     * <li>{@code java -jar Implementor.jar class-to-implement path-to-class}</li>
     * </ul>
     *
     * @param args command line arguments.
     * @see Implementor
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Wrong amount of arguments. Expected 2 or 3.");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Not-null arguments were expected");
                return;
            }
        }

        try {
            Implementor implementor = new Implementor();
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (InvalidPathException e) {
            System.out.println("Incorrect path to root in input: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Incorrect class name in input: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("Exception was thrown during the implementation: " + e.getMessage());
        }
    }
}
