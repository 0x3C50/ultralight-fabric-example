package me.x150.ul.mgr;

import com.labymedia.ultralight.plugin.filesystem.UltralightFileSystem;
import me.x150.ExampleMod;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class ExampleFileSystem implements UltralightFileSystem {
    // Map from handle to file channel, see class description for more details.
    private final Map<Long, FileChannel> openFiles = new HashMap<>();
    // Dumb implementation of a counter, but this will probably always be enough...
    // unless you have 9,223,372,036,854,775,807 files open. Please reconsider your application then!
    private long nextFileHandle;

    /**
     * This is called by Ultralight to check if a given file exists.
     * <p>
     * Note that Ultralight might pass invalid paths, so check for them!
     *
     * @param path The path to check for a file at
     *
     * @return {@code true} if the file exists, {@code false} otherwise
     */
    @Override
    public boolean fileExists(String path) {
        log(false, "Checking if %s exists", path);
        Path realPath = getPath(path);
        boolean exists = realPath != null && Files.exists(realPath);
        log(false, "%s %s", path, exists ? "exists" : "does not exist");
        return exists;
    }

    /**
     * Retrieves the file size for a given handle. Return -1 if the size can't be retrieved.
     *
     * @param handle The handle of the file to get the size of
     *
     * @return The size of the opened handle, or {@code -1}, if the size could not be determined
     */
    @Override
    public long getFileSize(long handle) {
        log(false, "Retrieving file size of handle %d", handle);
        FileChannel channel = openFiles.get(handle);
        if (channel == null) {
            // Should technically never occur unless Ultralight messed up
            log(true, "Failed to retrieve file size of handle %d, it was invalid", handle);
            return -1;
        } else {
            try {
                long size = channel.size();
                log(false, "File size of handle %d is %d", handle, size);
                return size;
            } catch (IOException e) {
                log(true, "Exception while retrieving size of handle %d", handle);
                e.printStackTrace();
                return -1;
            }
        }
    }

    /**
     * Retrieves the mime type of a given file. Ultralight needs this in order to determine how to load content.
     *
     * @param path The path to check the mime type for
     *
     * @return The mime type of the file at the given path, or {@code null}, if the mime type could not be determined
     */
    @Override
    public String getFileMimeType(String path) {
        log(false, "Retrieving mime type of %s", path);
        Path realPath = getPath(path);
        if (realPath == null) {
            // Ultralight requested an invalid path
            log(true, "Failed to retrieve mime type of %s, path was invalid", path);
            return null;
        }

        try {
            // Retrieve the mime type and log it
            String mimeType = Files.probeContentType(realPath);
            log(false, "Mime type of %s is %s", path, mimeType);
            return mimeType;
        } catch (IOException e) {
            log(true, "Exception while retrieving mime type of %s", path);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Opens a file at the given location. Ultralight calls this when it needs to read files. Currently the parameter
     * {@code openForWriting} is always {@code false}, and a {@code write} method is missing from Ultralight as well.
     *
     * @param path           The path of the file to open
     * @param openForWriting Whether the file should be opened for writing
     *
     * @return A handle to the opened file, or {@link #INVALID_FILE_HANDLE} if the file could not be opened
     */
    @Override
    public long openFile(String path, boolean openForWriting) {
        log(false, "Opening file %s for %s", path, openForWriting ? "writing" : "reading");
        Path realPath = getPath(path);
        if (realPath == null) {
            log(true, "Failed to open %s, the path is invalid", path);
            return INVALID_FILE_HANDLE;
        }

        FileChannel channel;
        try {
            // Actual open operation
            channel = FileChannel.open(realPath, openForWriting ? StandardOpenOption.WRITE : StandardOpenOption.READ);
        } catch (IOException e) {
            log(true, "Exception while opening %s", path);
            e.printStackTrace();
            return INVALID_FILE_HANDLE;
        }

        if (nextFileHandle == INVALID_FILE_HANDLE) {
            // Increment the handle number
            nextFileHandle = INVALID_FILE_HANDLE + 1;
        }

        // Map the give handle
        long handle = nextFileHandle++;
        openFiles.put(handle, channel);
        log(false, "Opened %s as handle %d", path, handle);
        return handle;
    }

    /**
     * Closes the given handle. This is called by Ultralight when a file is no longer needed and its resources can be
     * disposed.
     *
     * @param handle The handle of the file to close
     */
    @Override
    public void closeFile(long handle) {
        log(false, "Closing handle %d", handle);
        FileChannel channel = openFiles.remove(handle);
        if (channel != null) {
            try {
                channel.close();
                log(false, "Handle %d has been closed", handle);
            } catch (IOException e) {
                log(true, "Exception while closing handle %d", handle);
                e.printStackTrace();
            }
        } else {
            log(false, "Failed to close handle %d, it was invalid", handle);
        }
    }

    /**
     * Called by Ultralight when a chunk of data needs to be read from the file. Note that this may be called
     * multiple times on the same handle. When called on the same handle, the reader position needs to be kept, as
     * Ultralight expects the read to continue from the position where it was left of.
     * <p>
     * It currently is not possible to read files which sizes are greater than the integer limit because a
     * {@link ByteBuffer} is used as output. This is a bug in Ultralight Java and not Ultralight, however, due to the
     * low chances of that ever becoming an issue and the complexity of figuring out a proper solution, this is marked
     * as TODO.
     *
     * @param handle The handle of the file to read
     * @param data   Buffer to write read data into
     * @param length The amount of bytes to read from the file
     *
     * @return The amount of bytes read from the file
     */
    @Override
    public long readFromFile(long handle, ByteBuffer data, long length) {
        log(false, "Trying to read %d bytes from handle %d", length, handle);
        FileChannel channel = openFiles.get(handle);
        if (channel == null) {
            log(true, "Failed to read %d bytes from handle %d, it was invalid", length, handle);
            return -1;
        }

        if (length > Integer.MAX_VALUE) {
            log(true, "Failed to read %d bytes from handle %d, the size exceeded the limit", length, handle);
            // Not supported yet, marked as TODO
            // You should not throw Java exceptions into native code, so use it for getting a stacktrace and return -1
            new UnsupportedOperationException().printStackTrace();
            return -1;
        }

        try {
            long read = channel.read(data.slice().limit((int) length));
            log(false, "Read %d bytes out of %d requested from handle %d", read, length, handle);
            return read;
        } catch (IOException e) {
            log(true, "Exception occurred while reading %d bytes from handle %d", length, handle);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Helper method to scratch malformed paths
     *
     * @param path The path to convert to an NIO path
     *
     * @return The converted path, or {@code null}, if the path failed to convert
     */
    private Path getPath(String path) {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * Logs a message to the console.
     *
     * @param error Whether this is an error message
     * @param fmt   The format string
     * @param args  Arguments to format the string with
     */
    private void log(boolean error, String fmt, Object... args) {
        String message = String.format(fmt, args);
        if (error) {
            ExampleMod.LOGGER.error("(FileSystem) " + message);
        } else {
            ExampleMod.LOGGER.info("(FileSystem) " + message);
        }
    }
}
