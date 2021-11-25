package task;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.widget.Toast;

import core.MyFTPClientCore;
import core.exception.FTPClientException;
import monitor.ProgressDialogProgressMonitor;
import utils.ToastUtil;

/**
 * 下载/上传任务，同时承担在UI上和用户交互的过程
 */
public class DownloadUploadTask implements Runnable {


    /**
     * 操作类型，分别是下载文件，下载文件夹，并发下载文件夹，上传文件，上传文件夹，并发上传文件夹！
     */
    public enum OperationType {
        DOWNLOAD_FILE, DOWNLOAD_FOLDER, DOWNLOAD_FOLDER_CONCURRENTLY,
        UPLOAD_FILE, UPLOAD_FOLDER, UPLOAD_FOLDER_CONCURRENTLY
    }

    //ftp客户端的核心
    private volatile MyFTPClientCore myFTPClientCore;

    //下载参数
    private String arg;

    //ProgressDialog所属的owner Activity
    private Activity activity;

    //下载类型
    private OperationType operationType;

    //进度条对话框
    private ProgressDialog downloadProgressDialog;

    public DownloadUploadTask(MyFTPClientCore myFTPClientCore, OperationType operationType, String arg, ProgressDialog downloadProgressDialog) {
        this.myFTPClientCore = myFTPClientCore;
        this.operationType = operationType;
        this.activity = downloadProgressDialog.getOwnerActivity();
        if (this.activity == null) {
            throw new RuntimeException("请先在progressDialog上调用setOwnerActivity设置ownerActivity");
        }
        this.arg = arg;
        this.downloadProgressDialog = downloadProgressDialog;
    }

    @Override
    public void run() {

        //打开下载进度监视
        myFTPClientCore.setMonitorOpen(true);

        //进度对话框
        downloadProgressDialog.setCancelable(false);

        //监视器
        ProgressDialogProgressMonitor progressDialogProgressMonitor = new ProgressDialogProgressMonitor(downloadProgressDialog);
        //监视器注入
        myFTPClientCore.addProgressMonitor(progressDialogProgressMonitor);

        try {
            //在ui线程上打开进度显示的模态框
            activity.runOnUiThread(downloadProgressDialog::show);

            long a = System.nanoTime();

            //在下载线程上下载
            if (operationType == OperationType.DOWNLOAD_FILE) {
                myFTPClientCore.retrieveSingleFile(arg);
            } else if (operationType == OperationType.DOWNLOAD_FOLDER) {
                myFTPClientCore.retrieveFolder(arg);
            } else if (operationType == OperationType.DOWNLOAD_FOLDER_CONCURRENTLY) {
                myFTPClientCore.retrieveFolderConcurrently(arg);
            }

            long b = System.nanoTime();

            //在ui线程上弹出下载成功
            activity.runOnUiThread(() -> {
                AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setMessage(getSuccessfulMessageWithTime((b - a) / 1000.0 / 1000.0))
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                        }).create();
                alertDialog.show();
            });

        } catch (FTPClientException e) {
            activity.runOnUiThread(() -> {
                AlertDialog alertDialog = new AlertDialog.Builder(activity)
                        .setMessage(e.getMessage())
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                        }).create();
                alertDialog.show();
            });
        }

        activity.runOnUiThread(downloadProgressDialog::dismiss);


        myFTPClientCore.setMonitorOpen(false);
        myFTPClientCore.clearProgressMonitor();
    }

    public String getSuccessfulMessage() {
        if (operationType.toString().startsWith("DOWNLOAD")) {
            return String.format("下载%s成功", arg);
        } else if (operationType.toString().startsWith("UPLOAD")) {
            return String.format("上传%s成功", arg);
        }
        return null;
    }

    public String getSuccessfulMessageWithTime(double millis) {
        return getSuccessfulMessage() + " " + String.format("用时%f毫秒", millis);
    }
}
