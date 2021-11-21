package core.monitor;

import core.monitor.data.DownloadUploadProgressData;

/**
 * 下载进度监视器接口，调用其notify方法，在某处（比如控制台）显示下载进度
 */
public interface DownloadUploadProgressMonitor {

    public abstract void notify(DownloadUploadProgressData data);
}
