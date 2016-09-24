package de.silef.service.file.extension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static de.silef.service.file.extension.ExtensionType.UNIX_FILE;

/**
 * Created by sebastian on 23.09.16.
 */
public class UnixFileIndexExtension extends StandardIndexExtension {

    private static final int DATA_SIZE = 24;

    private int mode;
    private int userId;
    private int groupId;
    private long inode;
    private int linkCount;

    private boolean initialized = false;

    public UnixFileIndexExtension(byte[] data) {
        super(UNIX_FILE.value, data);
        if (data.length < DATA_SIZE) {
            throw new IndexExtensionInvalidDataException("Invalid data size. Expected " + data.length + " but was " + data.length);
        }
    }

    public static UnixFileIndexExtension createFromPath(Path file) throws IOException {
        Map<String, Object> attributes = Files.readAttributes(file, "unix:*");

        Integer mode = (Integer) attributes.get("mode");
        Integer userId = (Integer) attributes.get("uid");
        Integer groupId = (Integer) attributes.get("gid");
        Long inode = (Long) attributes.get("ino");
        Integer linkCount = (Integer) attributes.get("nlink");

        return new UnixFileIndexExtension(createData(mode, userId, groupId, inode, linkCount));
    }

    public int getMode() {
        if (!initialized) {
            extractData();
        }
        return mode;
    }

    public int getUserId() {
        if (!initialized) {
            extractData();
        }
        return userId;
    }

    public int getGroupId() {
        if (!initialized) {
            extractData();
        }
        return groupId;
    }

    public long getInode() {
        if (!initialized) {
            extractData();
        }
        return inode;
    }

    public int getLinkCount() {
        if (!initialized) {
            extractData();
        }
        return linkCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnixFileIndexExtension that = (UnixFileIndexExtension) o;

        return inode == that.inode &&
            mode == that.mode &&
            userId == that.userId &&
            groupId == that.groupId;
    }

    @Override
    public int hashCode() {
        int result = mode;
        result = 31 * result + userId;
        result = 31 * result + groupId;
        result = 31 * result + (int) (inode ^ (inode >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if (!initialized) {
            extractData();
        }
        return "UnixFileIndexExtension{" +
                "mode=" + mode +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", inode=" + inode +
                '}';
    }

    private static byte[] createData(int mode, int userId, int groupId, long inode, int linkCount) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(output)) {
            dataOutput.writeInt(mode);
            dataOutput.writeInt(userId);
            dataOutput.writeInt(groupId);
            dataOutput.writeLong(inode);
            dataOutput.writeInt(linkCount);

            return output.toByteArray();
        } catch (IOException e) {
            throw new IndexExtensionInvalidDataException("Could not write data of Unix File Extension");
        }
    }

    private void extractData() {
        try (ByteArrayInputStream input = new ByteArrayInputStream(getData());
             DataInputStream dataInput = new DataInputStream(input)) {
            mode = dataInput.readInt();
            userId = dataInput.readInt();
            groupId = dataInput.readInt();
            inode = dataInput.readLong();
            linkCount = dataInput.readInt();

            initialized = true;
        } catch (IOException e) {
            throw new IndexExtensionInvalidDataException("Could not read data of Unix File Extension");
        }
    }

}
