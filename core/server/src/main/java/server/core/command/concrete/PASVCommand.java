package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.AbstractResponse;
import server.core.response.concrete.EnteringPassiveModeResponse;
import server.core.response.concrete.NotLoginResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.Enumeration;

public class PASVCommand extends AbstractCommand {
    public PASVCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {

        //如果没有成功登录，就写入没有登录成功的响应
        if (!handleUserRequestThread.isLoginSuccessful()) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
            return;
        }

        SecureRandom random = new SecureRandom();

        boolean openServerSocketSuccess = false;

        //尝试随机在服务器上打开一个ServerSocket
        do {
            //被动模式，随机在服务器上的一个端口打开一个监听
            //先随机生成两个0~255的整数
            int p1 = random.nextInt(256);
            int p2 = random.nextInt(256);

            //获得端口
            int port = p1 * 256 + p2;


            //设置ServerSocket监听用户的连接
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {//由于可能端口冲突之类的问题，打开ServerSocket可能会抛出异常，因此如果抛出了异常就再试试一次
                continue;
            }

            //设置用来跳出循环的变量
            openServerSocketSuccess = true;

            //获取本机的地址
            InetAddress localAddress = PASVUtils.getLocalHostExactAddress();

            assert localAddress != null;

            //返回给用户的response
            AbstractResponse rsp = new EnteringPassiveModeResponse(localAddress.getHostAddress(), p1, p2);

            //如果原来有打开被动模式的数据连接监听端口，就把他关掉
            if (handleUserRequestThread.getPassiveModeServerSocket() != null) {
                handleUserRequestThread.getPassiveModeServerSocket().close();
            }

            //将传输模式设置成被动模式，并将serverSocket传给线程对象
            handleUserRequestThread.setPassiveActive(HandleUserRequestThread.PassiveActive.PASSIVE);
            handleUserRequestThread.setPassiveModeServerSocket(serverSocket);

            //把请求写给用户
            handleUserRequestThread.writeLine(rsp.toString());

        } while (!openServerSocketSuccess);
    }
}

class PASVUtils {
    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();

                    //排除ipv6地址
                    if (!(inetAddr instanceof Inet4Address)) {
                        continue;
                    }

                    // 排除loopback回环类型地址
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
