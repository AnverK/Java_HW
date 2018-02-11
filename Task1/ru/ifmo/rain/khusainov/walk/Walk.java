package ru.ifmo.rain.khusainov.walk;

import java.io.*;
import java.util.Scanner;

public class Walk {

    private static final class FNVHash {
        private static final int FNV_32_INIT = 0x811c9dc5;
        private static final int FNV_32_PRIME = 0x01000193;
        private int res;

        FNVHash() {
            res = FNV_32_INIT;
        }

        int hash32(final byte[] k, final int end) {
            for (int i = 0; i < end; i++) {
                res = (res * FNV_32_PRIME) ^ (k[i] & 0xff);
            }
            return res;
        }
    }

    private static void write_line(int hash, String name, FileWriter writer) throws IOException {
        writer.write(String.format("%08x %s", hash, name));
        writer.append(System.lineSeparator());
    }

    static void read_one_file(String file_name, FileWriter writer) {
        int hash = FNVHash.FNV_32_INIT;
        try (InputStream hash_reader = new FileInputStream(file_name)) {
            byte[] buf = new byte[1024];
            int c = 0;
            FNVHash fnv = new FNVHash();
            while ((c = hash_reader.read(buf)) >= 0) {
                hash = fnv.hash32(buf, c);
            }
        } catch (IOException e) {
            hash = 0;
        } finally {
            try {
                write_line(hash, file_name, writer);
            } catch (IOException e) {
                e.printStackTrace();
                // if we can not write in this file, probably we should throw the exception but we will just skip the file
                // and notify user about missing but not very clear. May be it's good idea to make own Exception class but not yet
            }
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
                String name = sc.next();
                read_one_file(name, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}