package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Vector;

import core.MyFTPClientCore;
import core.exception.FTPClientException;
import core.exception.ServerNotFoundException;

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

    private EditText hostnum;
    private EditText portnum;
    private Button connect;

    private MyFTPClientCore myFTPClientCore;
    private boolean connected = false;//client是否已连接
    private boolean loggedin = false;//用户是否已登录
    private boolean passiveActive = false;//是否确认模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //为了开发方便，禁用掉全部的严格模式限制
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());

        setTitle("FTP Client");

        //找到所有的view，赋值给此类的静态变量
        findAllViews();

//        //日志的绝对路径
//        logPath = getApplicationContext().getExternalFilesDir(null) + File.separator + "log.txt";


        type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            List<Exception> exceptionList = new Vector<>();
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(loggedin){
                    RadioButton r = (RadioButton) findViewById(i);
                    //Toast.makeText(MainActivity.this,"type:"+r.getText(),Toast.LENGTH_SHORT).show();
                    if(r.getText().equals("ascii")){
                        try {
                            myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.ASCII);
                            Toast.makeText(MainActivity.this,"type : ascii",Toast.LENGTH_SHORT).show();
                        } catch (FTPClientException e) {
                            exceptionList.add(e);
                            e.printStackTrace();
                        }
                    }else {
                        try {
                            myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.BINARY);
                            Toast.makeText(MainActivity.this,"type : binary",Toast.LENGTH_SHORT).show();
                        } catch (FTPClientException e) {
                            exceptionList.add(e);
                            e.printStackTrace();
                        }
                    }
                    if (exceptionList.size() != 0) {
                        Toast.makeText(getApplicationContext(), "error occurred in setting type!", Toast.LENGTH_LONG).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this,"尚未登录",Toast.LENGTH_SHORT).show();
                }
            }
        });

        kali.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!loggedin){
                    Toast.makeText(MainActivity.this,"尚未登录",Toast.LENGTH_SHORT).show();
                }else{
                    if(b){
                        try {
                            myFTPClientCore.kali(MyFTPClientCore.KeepAlive.T);
                        } catch (FTPClientException e) {
                            e.printStackTrace();
                        }
                    }else {
                        try {
                            myFTPClientCore.kali(MyFTPClientCore.KeepAlive.F);
                        } catch (FTPClientException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        pasv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!loggedin) {
                    Toast.makeText(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        myFTPClientCore.pasv();
                        passiveActive = true;
                        Toast.makeText(MainActivity.this, "passive mode", Toast.LENGTH_SHORT).show();
                    } catch (FTPClientException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!loggedin) {
                    Toast.makeText(MainActivity.this, "尚未登录", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        myFTPClientCore.port();
                        passiveActive = true;
                        Toast.makeText(MainActivity.this, "active mode", Toast.LENGTH_SHORT).show();
                    } catch (FTPClientException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connected){
                    Toast.makeText(MainActivity.this,"请断开连接后重试",Toast.LENGTH_SHORT).show();
                }else {
                    //根据服务器主机名和端口号进行连接
                    String host = hostnum.getText().toString();
                    String port = portnum.getText().toString();
                    if(host.length() == 0 || port.length() == 0 ){
                        Toast.makeText(MainActivity.this,"请输入主机号和端口名",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(!isNum(port)){
                        Toast.makeText(MainActivity.this,"端口号为2048-65536的数字",Toast.LENGTH_SHORT).show();
                    }else {
                        int port_num = Integer.parseInt(port);
                        if(!(port_num >= 2048 && port_num <= 65536)){
                            Toast.makeText(MainActivity.this, "端口必须在2048和65536之间", Toast.LENGTH_LONG).show();
                            return;
                        }else {
                            Exception exception = null;
                            try {
                                myFTPClientCore = new MyFTPClientCore(host,port_num);
                            } catch (ServerNotFoundException e) {
                                exception = e;
                                e.printStackTrace();
                            }

                            if(exception != null){
                                Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_LONG).show();
                            }else {
                                connected = true;
                                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!connected){
                    Toast.makeText(MainActivity.this,"请先连接服务器",Toast.LENGTH_SHORT).show();
                }else {
                    String user = username.getText().toString();
                    String pass = password.getText().toString();
                    boolean login_success = myFTPClientCore.login(user,pass);
                    if(login_success){
                        loggedin = true;
                        Toast.makeText(MainActivity.this,"登录成功",Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this,"登录失败",Toast.LENGTH_SHORT).show();
                    }
                }
            }
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

        connect = findViewById(R.id.connect);
        portnum = findViewById(R.id.portnum);
        hostnum = findViewById(R.id.hostnum);
        //scrollview
    }

    private static boolean isNum (String str){
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