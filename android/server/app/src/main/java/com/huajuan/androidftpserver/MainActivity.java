package com.huajuan.androidftpserver;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Application;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.huajuan.androidftpserver.mylogger.MyFTPServerLoggerImpl;

import java.io.File;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import server.core.MyFTPServerCore;

public class MainActivity extends AppCompatActivity {

    private TextView ftpRootPathTextView;
    private TextView runningStatusTextView;
    private Button launchFTPServerButton;//启动ftp服务器的按钮
    private EditText portEditText;//输入端口的编辑区
    private ScrollView logScrollView;
    private LinearLayout logLinearLayout;
    private CheckBox logCheckBox;
    boolean serverStarted = false;//服务器是否已经启动

    //服务器核心
    private volatile MyFTPServerCore ftpServerCore;


    private String ftpRootPath;
    private String logPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //申请文件访问权限
        requestPermissions(new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        //为了开发方便，禁用掉全部的严格模式限制
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());

        //获得本机的ip，设置在title上
        setTitle("FTP服务器" + getLocalHostExactAddress());

        //找到所有的view，赋值给此类的静态变量
        findAllViews();

        //设置ftp服务器的根目录
        ftpRootPath = getApplicationContext().getExternalFilesDir("ftp_server").getAbsolutePath();
        ftpRootPathTextView.setText(ftpRootPath);

        //日志的绝对路径
        logPath = getApplicationContext().getExternalFilesDir(null) + File.separator + "log.txt";

        //设置按下按钮后启动服务器！
        launchFTPServerButton.setOnClickListener((view) -> {
            if (!serverStarted) {//如果未启动

                //端口号
                String portNumberStr = portEditText.getText().toString();
                //检查端口号是否符合规格
                if (portNumberStr.length() == 0 || !(2048 <= Integer.parseInt(portNumberStr) && Integer.parseInt(portNumberStr) <= 65535)) {
                    Toast.makeText(getApplicationContext(), "服务器端口必须在2048和65536之间", Toast.LENGTH_LONG).show();
                    return;
                }

                List<Exception> exceptionList = new Vector<>();

                //开一个新线程用来运行服务器
                Thread t = new Thread(() -> {
                    try {
                        if (logCheckBox.isChecked()) {
                            ftpServerCore = new MyFTPServerCore(Integer.parseInt(portNumberStr), ftpRootPath, new MyFTPServerLoggerImpl(logPath,logScrollView, logLinearLayout, this));
                        } else {
                            ftpServerCore = new MyFTPServerCore(Integer.parseInt(portNumberStr), ftpRootPath, null);
                        }
                        ftpServerCore.start();
                    } catch (Exception e) {
                        exceptionList.add(e);
                        e.printStackTrace();
                    }
                });

                t.start();

                try {
                    t.join();
                } catch (InterruptedException ignored) {
                }

                if (exceptionList.size() == 0) {
                    serverStarted = true;
                    Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_LONG).show();
                    runningStatusTextView.setText("Running!");
                } else {
                    Toast.makeText(getApplicationContext(), "启动失败", Toast.LENGTH_LONG).show();

                }

            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "已经启动过服务器了", Toast.LENGTH_LONG);
                toast.show();
            }

        });
    }


    /**
     * 找到所有的view，赋值给此类的静态变量
     */
    private void findAllViews() {
        ftpRootPathTextView = findViewById(R.id.ftp_root_path_textview);
        runningStatusTextView = findViewById(R.id.running_status_textview);
        launchFTPServerButton = findViewById(R.id.launch_ftp_server_button);
        portEditText = findViewById(R.id.port_edittext);
        logLinearLayout = findViewById(R.id.log_linear_layout);
        logScrollView = findViewById(R.id.log_scrollview);
        logCheckBox = findViewById(R.id.log_checkbox);
    }


    /**
     * 获得本机的精确ipv4地址
     *
     * @return 地址
     */
    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();

                    if (inetAddr instanceof Inet6Address) {
                        continue;
                    }
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            return null;
        }
    }
}