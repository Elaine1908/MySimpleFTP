package com.huajuan.androidftpserver.mylogger;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.FileWriter;
import java.io.IOException;

import server.core.logger.FTPServerLogger;

public class MyFTPServerLoggerImpl implements FTPServerLogger {
    private final ScrollView logScrollView;
    private final LinearLayout logLinearLayout;
    private final Activity activity;
    private FileWriter logFileWriter;
    private final int MAX_INFO_CNT = 10000;

    public MyFTPServerLoggerImpl(String logPath, ScrollView logScrollView, LinearLayout logLinearLayout, Activity activity) {
        try {
            this.logFileWriter = new FileWriter(logPath, true);
        } catch (IOException ignored) {
            this.logFileWriter = null;
        }
        this.logLinearLayout = logLinearLayout;
        this.logScrollView = logScrollView;
        this.activity = activity;
    }

    @Override
    public void info(String s) {
        activity.runOnUiThread(() -> {
            if (logLinearLayout.getChildCount() >= MAX_INFO_CNT) {//如果信息的数量超过了，就清空
                logLinearLayout.removeAllViews();
            }
            TextView textView = new TextView(activity.getApplicationContext());
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setText(s);
            logLinearLayout.addView(textView);
            logScrollView.fullScroll(View.FOCUS_DOWN);
        });

        if (logFileWriter != null) {
            try {
                logFileWriter.write(s);
            } catch (IOException ignored) {
            }
        }
    }
}
