package monitor;

import android.app.Activity;
import android.app.ProgressDialog;

import core.monitor.DownloadUploadProgressMonitor;
import core.monitor.data.DownloadUploadProgressData;

/**
 * 进度条的下载|上传程度监视器
 */
public class ProgressDialogProgressMonitor implements DownloadUploadProgressMonitor {

    //用来显示下载|上传进度的对话框
    private final ProgressDialog progressDialog;


    /**
     * 根据进度对话框，创建进度监视器
     *
     * @param progressDialog 进度对话框
     */
    public ProgressDialogProgressMonitor(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
        this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.setTitle("请稍后");
        this.progressDialog.setMessage("");
    }

    @Override
    public void notify(DownloadUploadProgressData data) {

        //接到通知，更新对话框上的进度！
        progressDialog.getOwnerActivity().runOnUiThread(() -> {
            progressDialog.setTitle(data.getType().toString());
            progressDialog.setMessage(data.getFilename());

            double percentage = ((double) data.getDownloaded() / data.getTotal()) * 100;

            progressDialog.setProgress((int) percentage);
        });
    }
}
