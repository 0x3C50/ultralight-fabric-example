package me.x150;

import com.labymedia.ultralight.UltralightJava;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Objects;

public class ExampleMod implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("fuck-it");
    public static Path resources;
    public static Path baseTempDir;
    static Path tempDirectory;
    String nativesBasePath = "native-binaries/";
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

    private File[] findNatives() throws IOException {
        Enumeration<URL> resources = ExampleMod.class.getClassLoader().getResources(nativesBasePath);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            File file = new File(url.getFile());
            if (file.isDirectory()) {
                return file.listFiles();
            }
        }
        return null;
    }

    private File[] findUlResources() throws IOException {
        Enumeration<URL> resources = ExampleMod.class.getClassLoader().getResources(resourcesPath);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            File file = new File(url.getFile());
            if (file.isDirectory()) {
                return file.listFiles();
            }
        }
        return null;
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

        for (File aNative : Objects.requireNonNull(findNatives())) {
            Path path = aNative.toPath();
            String s = path.getFileName().toString();
            Path tf = tempDirectory.resolve(s);
            LOGGER.info(aNative + " -> " + tf);
            try (FileInputStream fis = new FileInputStream(aNative); FileOutputStream fos = new FileOutputStream(tf.toFile())) {
                byte[] buffer = new byte[512];
                int r;
                while ((r = fis.read(buffer, 0, buffer.length)) != -1) {
                    fos.write(buffer, 0, r);
                }
            }
        }

        for (File aNative : Objects.requireNonNull(findUlResources())) {
            Path path = aNative.toPath();
            String s = path.getFileName().toString();
            Path tf = resources.resolve(s);
            LOGGER.info(aNative + " -> " + tf);
            try (FileInputStream fis = new FileInputStream(aNative); FileOutputStream fos = new FileOutputStream(tf.toFile())) {
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