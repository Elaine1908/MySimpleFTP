package task;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import core.MyFTPClientCore;
import core.exception.FTPClientException;
import monitor.ProgressDialogProgressMonitor;

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

    //参数，如果是下载，就是服务器上的文件名；如果是上传，就是客户端上要上传的文件的路径名
    private String arg;

    //ProgressDialog所属的owner Activity
    private Activity activity;

    //下载类型
    private OperationType operationType;

    //进度条对话框
    private ProgressDialog downloadUploadProgressDialog;

    public DownloadUploadTask(MyFTPClientCore myFTPClientCore, OperationType operationType, String arg, ProgressDialog downloadUploadProgressDialog) {
        this.myFTPClientCore = myFTPClientCore;
        this.operationType = operationType;
        this.activity = downloadUploadProgressDialog.getOwnerActivity();
        if (this.activity == null) {
            throw new RuntimeException("请先在progressDialog上调用setOwnerActivity设置ownerActivity");
        }
        this.arg = arg;
        this.downloadUploadProgressDialog = downloadUploadProgressDialog;
    }

    @Override
    public void run() {

        //打开下载进度监视
        myFTPClientCore.setMonitorOpen(true);

        //进度对话框
        downloadUploadProgressDialog.setCancelable(false);

        //监视器
        ProgressDialogProgressMonitor progressDialogProgressMonitor = new ProgressDialogProgressMonitor(downloadUploadProgressDialog);
        //监视器注入
        myFTPClientCore.addProgressMonitor(progressDialogProgressMonitor);

        try {
            //在ui线程上打开进度显示的模态框
            activity.runOnUiThread(downloadUploadProgressDialog::show);

            long a = System.nanoTime();

            //在下载|上传线程上下载|上传
            if (operationType == OperationType.DOWNLOAD_FILE) {
                myFTPClientCore.retrieveSingleFile(arg);
            } else if (operationType == OperationType.DOWNLOAD_FOLDER) {
                myFTPClientCore.retrieveFolder(arg);
            } else if (operationType == OperationType.DOWNLOAD_FOLDER_CONCURRENTLY) {
                myFTPClientCore.retrieveFolderConcurrently(arg);
            } else if (operationType == OperationType.UPLOAD_FILE) {
                myFTPClientCore.storeSingleFile(arg);
            } else if (operationType == OperationType.UPLOAD_FOLDER) {
                myFTPClientCore.storeFolder(arg);
            } else if (operationType == OperationType.UPLOAD_FOLDER_CONCURRENTLY) {
                myFTPClientCore.storeFolderConcurrently(arg);
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

        activity.runOnUiThread(downloadUploadProgressDialog::dismiss);


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
