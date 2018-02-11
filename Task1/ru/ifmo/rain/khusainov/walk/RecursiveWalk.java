package ru.ifmo.rain.khusainov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;

public class RecursiveWalk extends Walk {
//    private static void getNextFile(Path path, FileWriter writer) throws IOException {
//        File file = new File(path);
//        if (!file.isDirectory()) {
//            read_one_file(file.getPath(), writer);
//        }
//        if (file.list() == null) {
//            return;
//        }
//        for (int i = 0; i < file.list().length; i++) {
//            getNextFile(path + '/' + file.list()[i], writer);
//        }
//    }

    public static class MyFileVisitor extends SimpleFileVisitor<Path> {
        FileWriter writer;

        MyFileVisitor(FileWriter w) {
            writer = w;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            read_one_file(file.toString(), writer);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            read_one_file(file.toString(), writer);
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Wrong number of arguments. Expected 2, got " + args.length);
            return;
        }
        try (
                InputStream reader = new FileInputStream(args[0]);
                FileWriter writer = new FileWriter(args[1])
        ) {
            Scanner sc = new Scanner(reader, "UTF-8");
            while (sc.hasNext()) {
                String directory = sc.next();
                Files.walkFileTree(Paths.get(directory), new MyFileVisitor(writer));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
