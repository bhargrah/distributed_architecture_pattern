package com.bhargrah.durablecachems;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {

    private static Random random = new Random();
    public static File tempDir(String prefix) {
        var ioDir = System.getProperty("java.io.tmpdir");
        var f = new File(ioDir, prefix + random.nextInt(1000000));
        f.mkdirs();
        f.deleteOnExit();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
                        // If the root path did not exist, ignore the error; otherwise throw it.
                        if (exc instanceof NoSuchFileException && path.toFile().equals(f))
                            return FileVisitResult.TERMINATE;
                        throw exc;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        Files.delete(path);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }

                        ;
                        List filesToKeep = new ArrayList<>();
                        if (!filesToKeep.contains(path.toFile())) {
                            Files.delete(path);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return f;
    }

}
