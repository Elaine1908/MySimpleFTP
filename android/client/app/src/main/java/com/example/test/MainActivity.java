package com.example.test;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import core.MyFTPClientCore;
import core.exception.FTPClientException;
import core.exception.ServerNotFoundException;
import utils.DialogUtil;
import utils.ToastUtil;

public class MainActivity extends AppCompatActivity {
    private Button login;
    private EditText username;
    private EditText password;


    private Button port;
    private Button pasv;
    private TextView status;
    private Switch kali;

    private RadioGroup type;
    private RadioButton ascii;
    private RadioButton binary;

    private RadioGroup mode;
    private RadioGroup structure;

    private EditText hostnum;
    private EditText portnum;
    private Button connect;
    private Button quit;

    private Button retr_file;
    private Button retr_folder;
    private Button multi_retr;
    private Button stor_file;
    private Button stor_folder;
    private Button multi_stor;

    private MyFTPClientCore myFTPClientCore;
    private boolean connected = false;//client是否已连接
    private boolean loggedin = false;//用户是否已登录
    private boolean passiveActive = false;//是否确认模式


    private String downloadPath;//文件下载到哪个目录

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //为了开发方便，禁用掉全部的严格模式限制
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());

        setTitle("FTP客户端" + getLocalHostExactAddress());

        //找到所有的view，赋值给此类的静态变量
        findAllViews();

        //获取下载目录
        downloadPath = getApplicationContext().getExternalFilesDir("ftp_download").getAbsolutePath();
        DialogUtil.simpleAlert(this, "下载目录", downloadPath);

        type.setOnCheckedChangeListener((radioGroup, i) -> {

            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }

            RadioButton r = (RadioButton) findViewById(i);

            if (r == ascii) {
                try {
                    myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.ASCII);
                    ToastUtil.showToast(MainActivity.this, "TYPE : ASCII", Toast.LENGTH_SHORT);
                } catch (FTPClientException e) {
                    ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            } else if (r == binary) {
                try {
                    myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.BINARY);
                    ToastUtil.showToast(MainActivity.this, "TYPE : BINARY", Toast.LENGTH_SHORT);
                } catch (FTPClientException e) {
                    ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }
        });

        mode.setOnCheckedChangeListener((radioGroup, i)->{
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            RadioButton r = (RadioButton) findViewById(i);
            ToastUtil.showToast(MainActivity.this,"MODE : "+r.getText().toString(),Toast.LENGTH_SHORT);
        });

        structure.setOnCheckedChangeListener(((radioGroup, i) -> {
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            RadioButton r = (RadioButton) findViewById(i);
            ToastUtil.showToast(MainActivity.this,"STRUCTURE : "+r.getText().toString(),Toast.LENGTH_SHORT);
        }));


        kali.setOnCheckedChangeListener((compoundButton, b) -> {
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                //把checked状态换回去！
                compoundButton.setChecked(!b);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                compoundButton.setChecked(!b);
                return;
            }
            if (b) {
                try {
                    myFTPClientCore.kali(MyFTPClientCore.KeepAlive.T);
                } catch (FTPClientException e) {
                    ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            } else {
                try {
                    myFTPClientCore.kali(MyFTPClientCore.KeepAlive.F);
                } catch (FTPClientException e) {
                    ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                    e.printStackTrace();
                }
            }

        });

        pasv.setOnClickListener(v -> {
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }

            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            try {
                myFTPClientCore.pasv();
                passiveActive = true;
                ToastUtil.showToast(MainActivity.this, "成功进入被动模式", Toast.LENGTH_SHORT);
            } catch (FTPClientException e) {
                ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
        });

        port.setOnClickListener(v -> {

            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }

            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            try {
                myFTPClientCore.port();
                passiveActive = true;
                ToastUtil.showToast(MainActivity.this, "成功进入主动模式", Toast.LENGTH_SHORT);
            } catch (FTPClientException e) {
                ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                e.printStackTrace();
            }
        });

        connect.setOnClickListener(v -> {

            if (myFTPClientCore != null) {//如果原先还有客户端对象，就关闭掉旧的客户端对象
                myFTPClientCore.close();
            }

            //根据服务器主机名和端口号进行连接
            String host = hostnum.getText().toString();
            String port = portnum.getText().toString();

            //检测主机号和端口名是否为null
            if (host.length() == 0 || port.length() == 0) {
                ToastUtil.showToast(MainActivity.this, "请输入主机号和端口名", Toast.LENGTH_SHORT);
                return;
            }

            if (!isNum(port)) {
                ToastUtil.showToast(MainActivity.this, "端口号为2048-65536的数字", Toast.LENGTH_SHORT);
                return;
            }

            int port_num = Integer.parseInt(port);
            if (!(port_num >= 2048 && port_num <= 65536)) {
                ToastUtil.showToast(MainActivity.this, "端口必须在2048和65536之间", Toast.LENGTH_LONG);
                return;
            }


            try {
                myFTPClientCore = new MyFTPClientCore(host, port_num);
                connected = true;
                ToastUtil.showToast(MainActivity.this, "连接成功", Toast.LENGTH_LONG);
            } catch (ServerNotFoundException e) {
                connected = false;
                ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG);

            }
        });

        quit.setOnClickListener(v -> {
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接服务器", Toast.LENGTH_SHORT);
                return;
            }
            if(!loggedin){
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            myFTPClientCore.quit();
            ToastUtil.showToast(MainActivity.this, "退出成功", Toast.LENGTH_SHORT);
            loggedin = false;
            connected = false;
            passiveActive = false;
        });

        login.setOnClickListener(v -> {

            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "请先连接服务器", Toast.LENGTH_SHORT);
                return;
            }

            String user = username.getText().toString();
            String pass = password.getText().toString();
            boolean login_success = myFTPClientCore.login(user, pass);
            if (login_success) {
                loggedin = true;
                ToastUtil.showToast(MainActivity.this, "登录成功", Toast.LENGTH_SHORT);
            } else {
                loggedin = false;
                ToastUtil.showToast(MainActivity.this, "登录失败", Toast.LENGTH_SHORT);
            }

        });

        retr_file.setOnClickListener(v ->{
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            if(!passiveActive){
                ToastUtil.showToast(MainActivity.this, "未选择主动/被动模式", Toast.LENGTH_SHORT);
                return;
            }

            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请输入文件所在位置").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String args = inputServer.getText().toString();
                    try {
                        myFTPClientCore.retrieveSingleFile(args);
                    } catch (FTPClientException e) {
                        ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                        e.printStackTrace();
                    }
                }
            });
            builder.show();
        });

        retr_folder.setOnClickListener(v ->{
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            if(!passiveActive){
                ToastUtil.showToast(MainActivity.this, "未选择主动/被动模式", Toast.LENGTH_SHORT);
                return;
            }

            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请输入文件夹所在位置").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String args = inputServer.getText().toString();
                    try {
                        myFTPClientCore.retrieveFolder(args);
                    } catch (FTPClientException e) {
                        ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                        e.printStackTrace();
                    }
                }
            });
            builder.show();
        });

        multi_retr.setOnClickListener(v ->{
            if (!connected) {
                ToastUtil.showToast(MainActivity.this, "尚未连接", Toast.LENGTH_SHORT);
                return;
            }
            if (!loggedin) {
                ToastUtil.showToast(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT);
                return;
            }
            if(!passiveActive){
                ToastUtil.showToast(MainActivity.this, "未选择主动/被动模式", Toast.LENGTH_SHORT);
                return;
            }

            final EditText inputServer = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请输入文件夹所在位置").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                    .setNegativeButton("取消", null);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String args = inputServer.getText().toString();
                    try {
                        myFTPClientCore.retrieveFolderConcurrently(args);
                    } catch (FTPClientException e) {
                        ToastUtil.showToast(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                        e.printStackTrace();
                    }
                }
            });
            builder.show();
        });


    }

    private void findAllViews() {


        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);

        port = findViewById(R.id.port);
        pasv = findViewById(R.id.pasv);

        ascii = findViewById(R.id.ascii);
        binary = findViewById(R.id.binary);
        kali = findViewById(R.id.kali);
        type = findViewById(R.id.type);
        mode = findViewById(R.id.mode);
        structure = findViewById(R.id.structure);

        connect = findViewById(R.id.connect);
        portnum = findViewById(R.id.portnum);
        hostnum = findViewById(R.id.hostnum);

        quit = findViewById(R.id.quit);

        retr_file = findViewById(R.id.retr_file);
        retr_folder = findViewById(R.id.retr_folder);
        multi_retr = findViewById(R.id.multithread_retr);
        stor_file = findViewById(R.id.stor_file);
        stor_folder = findViewById(R.id.stor_folder);
        multi_stor = findViewById(R.id.multithread_stor);
        //scrollview
    }

    private static boolean isNum(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }


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