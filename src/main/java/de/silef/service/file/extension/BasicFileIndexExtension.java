package de.silef.service.file.extension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static de.silef.service.file.extension.ExtensionType.BASIC_FILE;

/**
 * Created by sebastian on 23.09.16.
 */
public class BasicFileIndexExtension extends StandardIndexExtension {

    private static final int DATA_SIZE = 32;

    private long size;
    private long creationTime;
    private long modifiedTime;
    private long fileKey;

    private boolean initialized = false;

    public BasicFileIndexExtension(byte[] data) {
        super(BASIC_FILE.value, data);
        if (data.length < DATA_SIZE) {
            throw new IndexExtensionInvalidDataException("Invalid data size. Expected " + data.length + " but was " + data.length);
        }
    }

    public static BasicFileIndexExtension createFromPath(Path file) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

        long size = attributes.isDirectory() ? 0 : attributes.size();
        long createdTime = attributes.creationTime().toMillis();
        long modifiedTime = attributes.creationTime().toMillis();
        long fileKey = getInode(attributes);

        return new BasicFileIndexExtension(createData(size, createdTime, modifiedTime, fileKey));
    }

    private static long getInode(BasicFileAttributes attributes) {
        String key = String.valueOf(attributes.fileKey());
        int pos = key.indexOf("ino=");
        if (pos >= 0) {
            pos += 4;
            int end = pos;
            while (key.charAt(end) >= '0' && key.charAt(end) <= '9') {
                end++;
            }
            if (pos < end) {
                return Long.parseLong(key.substring(pos, end));
            }
        }
        return 0;
    }

    public long getSize() {
        if (!initialized) {
            extractData();
        }
        return size;
    }

    public long getCreationTime() {
        if (!initialized) {
            extractData();
        }
        return creationTime;
    }

    public long getModifiedTime() {
        if (!initialized) {
            extractData();
        }
        return modifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicFileIndexExtension that = (BasicFileIndexExtension) o;

        return size == that.size &&
            creationTime == that.creationTime &&
            modifiedTime == that.modifiedTime;
    }

    @Override
    public int hashCode() {
        int result = (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (creationTime ^ (creationTime >>> 32));
        result = 31 * result + (int) (modifiedTime ^ (modifiedTime >>> 32));
        result = 31 * result + (int) (fileKey ^ (fileKey >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if (!initialized) {
            extractData();
        }
        return "BasicFile{" +
                "size=" + size +
                ", creationTime=" + creationTime +
                ", modifiedTime=" + modifiedTime +
                ", fileKey=" + fileKey +
                '}';
    }

    private static byte[] createData(long size, long creationTime, long modifiedTime, long fileKey) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(output)) {
            dataOutput.writeLong(size);
            dataOutput.writeLong(creationTime);
            dataOutput.writeLong(modifiedTime);
            dataOutput.writeLong(fileKey);

            return output.toByteArray();
        } catch (IOException e) {
            throw new IndexExtensionInvalidDataException("Could not write data of Unix File Extension");
        }
    }

    private void extractData() {
        try (ByteArrayInputStream input = new ByteArrayInputStream(getData());
             DataInputStream dataInput = new DataInputStream(input)) {
            size = dataInput.readLong();
            creationTime = dataInput.readLong();
            modifiedTime = dataInput.readLong();
            fileKey = dataInput.readLong();

            initialized = true;
        } catch (IOException e) {
            throw new IndexExtensionInvalidDataException("Could not read data of Unix File Extension");
        }
    }

}
