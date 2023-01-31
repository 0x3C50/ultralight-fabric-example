package me.x150;

import com.labymedia.ultralight.UltralightJava;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class ExampleMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("ultralight-fabric");
    public static Path resources;
    public static Path baseTempDir;
    static Path tempDirectory;
    String nativesBasePath = "natives";
    String resourcesPath = "ul-resources";

    public static Path requestTempDir(String name) {
        Path resolve = baseTempDir.resolve(name);
		if (!resolve.toFile().mkdirs()) {
			throw new IllegalStateException("Failed to make dir");
		}
        return resolve;
    }

    static void recDelete(Path p) throws IOException {
        Files.walkFileTree(p, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toFile().delete()) {
                    throw new IOException("Failed to delete file " + file.toAbsolutePath());
                }
                System.out.println("Deleted " + file.toAbsolutePath());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                exc.printStackTrace();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.toFile().delete()) {
                    throw new IOException("Failed to delete directory " + dir.toAbsolutePath());
                }
                System.out.println("Deleted " + dir.toAbsolutePath());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static void onStop() {
        try {
            recDelete(baseTempDir);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        try {
            baseTempDir = Files.createTempDirectory("ultralight-fabric");
            Runtime.getRuntime().addShutdownHook(new Thread(ExampleMod::onStop));
            initUL();
        } catch (Throwable e) {
            LOGGER.error("Failed to init UL", e);
            System.exit(1);
        }
    }
    private Stream<Path> findNatives() throws IOException, URISyntaxException {
        URI uri = ExampleMod.class.getClassLoader().getResource(nativesBasePath).toURI();
        Path myPath;
        FileSystem c = null;
        if (uri.getScheme().equals("jar")) {
            try {
                c = FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (FileSystemAlreadyExistsException e) {
                c = FileSystems.getFileSystem(uri);
            }
            myPath = c.getPath(nativesBasePath);
        } else {
            myPath = Paths.get(uri);
        }

        return Files.walk(myPath);
    }

    private Stream<Path> findUlResources() throws IOException, URISyntaxException {
        URI uri = ExampleMod.class.getClassLoader().getResource(resourcesPath).toURI();
        Path myPath;
        FileSystem c = null;
        if (uri.getScheme().equals("jar")) {
            try {
                c = FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (FileSystemAlreadyExistsException e) {
                c = FileSystems.getFileSystem(uri);
            }
            myPath = c.getPath(resourcesPath);
        } else {
            myPath = Paths.get(uri);
        }

        return Files.walk(myPath);
    }

    void initUL() throws Throwable {
        LOGGER.info("Initializing ultralight");
        tempDirectory = requestTempDir("natives");
        resources = requestTempDir("resources");
        String libpath = System.getProperty("java.library.path");
        if (libpath != null) {
            libpath += File.pathSeparator + tempDirectory.toAbsolutePath();
        } else {
            libpath = tempDirectory.toAbsolutePath().toString();
        }
        System.setProperty("java.library.path", libpath);

        Iterator<Path> iterator = findNatives().iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (Files.isDirectory(path)) continue; // dont need
            String s = path.getFileName().toString();
            Path tf = tempDirectory.resolve(s);
            LOGGER.info(path + " -> " + tf);
            try (InputStream fis = Files.newInputStream(path); OutputStream fos = Files.newOutputStream(tf)) {
                byte[] buffer = new byte[512];
                int r;
                while ((r = fis.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, r);
                }
            }
        }

        iterator = findUlResources().iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (Files.isDirectory(path)) continue; // dont need
            String s = path.getFileName().toString();
            Path tf = resources.resolve(s);
            LOGGER.info(path + " -> " + tf);
            try (InputStream fis = Files.newInputStream(path); OutputStream fos = Files.newOutputStream(tf)) {
                byte[] buffer = new byte[512];
                int r;
                while ((r = fis.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, r);
                }
            }
        }

        LOGGER.info("Extracting UltralightJava");
        UltralightJava.extractNativeLibrary(tempDirectory);

        LOGGER.info("Loading");
        UltralightJava.load(tempDirectory);

        LOGGER.info("OK");
    }
}