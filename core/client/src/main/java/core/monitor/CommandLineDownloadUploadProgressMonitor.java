package core.monitor;

import core.monitor.data.DownloadUploadProgressData;

import java.util.Objects;

public class CommandLineDownloadUploadProgressMonitor implements DownloadUploadProgressMonitor {

    private long lastPrinted = 0;
    private String lastFilename = null;

    @Override
    public void notify(DownloadUploadProgressData data) {
        if (!Objects.equals(data.getFilename(), lastFilename)) {//如果换文件了，把上次下载到哪里置0
            lastFilename = data.getFilename();
            lastPrinted = 0;
        }
        if (data.getDownloaded() - lastPrinted > 1024 * 1024) {
            System.out.println(data);
            lastPrinted = data.getDownloaded();

        }
    }
}
