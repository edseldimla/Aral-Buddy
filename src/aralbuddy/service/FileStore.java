package aralbuddy.service;

import aralbuddy.model.AppData;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;

/** Owns the local file, process lock, bounded reads, and atomic saves. */
public final class FileStore implements AutoCloseable {
    private static final long MAX_BYTES = 8 * 1024 * 1024;
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock lock;

    /** Locks a data directory so two application windows cannot overwrite each other. */
    public FileStore(Path directory) throws IOException {
        Files.createDirectories(directory);
        file = directory.resolve("aralbuddy.dat");
        lockChannel = FileChannel.open(directory.resolve("aralbuddy.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
        } catch (OverlappingFileLockException exception) {
            acquired = null;
        } catch (IOException exception) {
            lockChannel.close();
            throw exception;
        }
        if (acquired == null) {
            lockChannel.close();
            throw new IOException("Aral Buddy is already using this data folder. Close the other window first.");
        }
        lock = acquired;
    }

    /** Returns the real data location shown in the application and README. */
    public Path getPath() { return file; }

    /** Loads an existing snapshot; a damaged file is never silently replaced with demo data. */
    public AppData load() throws IOException {
        if (!Files.exists(file)) {
            if (Files.exists(file.resolveSibling("aralbuddy.dat.bak")))
                throw new IOException("The main saved file is missing, but a backup exists. See README.md for recovery.");
            return null;
        }
        if (Files.size(file) > MAX_BYTES) throw new IOException("The saved file exceeds the 8 MB limit.");
        try {
            return decode(Files.readAllBytes(file));
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Cannot read " + file + ". Your file has been kept. See README.md for backup recovery.", exception);
        }
    }

    /** Saves a complete snapshot before reporting success to the user. */
    public void save(AppData data) throws IOException {
        byte[] bytes = encode(data);
        if (bytes.length > MAX_BYTES) throw new IOException("The saved file would exceed the 8 MB limit.");
        Path temporary = Files.createTempFile(file.getParent(), "aralbuddy-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            if (Files.exists(file)) {
                if (!Files.isRegularFile(file)) throw new IOException("The saved-file path is not a regular file. Your changes were not saved.");
                Path backupTemporary = Files.createTempFile(file.getParent(), "backup-", ".tmp");
                try {
                    Files.copy(file, backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                    moveCompleteFile(backupTemporary, file.resolveSibling("aralbuddy.dat.bak"));
                } finally { Files.deleteIfExists(backupTemporary); }
            }
            moveCompleteFile(temporary, file);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void moveCompleteFile(Path from, Path to) throws IOException {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(from, to, StandardCopyOption.REPLACE_EXISTING); }
    }

    /** Serializes only the application's own data snapshot. */
    public static byte[] encode(AppData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(data); }
        return bytes.toByteArray();
    }

    /** Reads a snapshot with class, depth, reference, and array limits. */
    public static AppData decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Saved data is too large.");
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 20 || info.references() > 150_000 || info.arrayLength() > 100_000)
                    return ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                if (type.isArray()) return type == Object[].class ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
                String name = type.getName();
                return name.startsWith("aralbuddy.model.") || name.equals("java.util.ArrayList") || name.equals("java.lang.String")
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            Object result = input.readObject();
            if (!(result instanceof AppData) || ((AppData) result).version != 1)
                throw new IOException("Unsupported saved data version.");
            if (input.read() != -1) throw new IOException("Unexpected trailing data.");
            return (AppData) result;
        } catch (ClassNotFoundException exception) {
            throw new IOException("The saved file needs a different application version.", exception);
        }
    }

    /** Releases the folder lock when the main window closes. */
    @Override public void close() throws IOException {
        try { lock.release(); } finally { lockChannel.close(); }
    }
}
