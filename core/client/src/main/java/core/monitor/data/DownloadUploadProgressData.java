package core.monitor.data;

import java.security.SecureRandom;

/**
 * 关于下载进度的数据
 */
public class DownloadUploadProgressData {
    private final long total;//总共多少字节
    private final long downloaded;//下载了多少字节
    private final long remain;//还剩多少字节
    private final String filename;


    public enum Type {
        DOWNLOAD, UPLOAD
    }

    private final Type type;

    public DownloadUploadProgressData(String filename, long total, long downloaded, Type type) {
        this.total = total;
        this.downloaded = downloaded;
        remain = total - downloaded;
        this.filename = filename;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s,文件:%s,已经:%d,总共:%d,还剩:%d", type, filename, downloaded, total, remain);
    }

    public long getTotal() {
        return total;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getRemain() {
        return remain;
    }

    public String getFilename() {
        return filename;
    }

    public Type getType() {
        return type;
    }
}
