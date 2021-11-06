# 注意

每个命令的命令名（比如USER, PASV）必须全大写

命令和命令参数之间，响应码和响应信息之间，都用**一个空格**分开，比如

```
USER huajuan
530 没有登录成功
```

客户端向服务器发送命令，服务器向客户端发送响应，都是一行一行的，比如

```
USER huajuan
530 没有登录成功
```

每个都是一行。注意每行必须使用**<CRLF>**结尾，而不是<CR>或者<LF>。



**在一个控制连接上应该同时只能有一个命令执行，即在一个命令完全结束以后才能执行下一个命令。如果需要并发上传或下载，请使用多个控制连接。**



# 几个对于所有command都具有的response

## 500

语法错误，命令不能被识别

## 530

没有登录成功







# 命令：USER <username>

## 参数

username：用于登录的用户名

## 响应

### 230

用户登录成功，继续（即此时这个用户是没有密码的）

### 331

用户名OK，需要密码。



# 命令：PASS <pwd>

## 参数

pwd：用户登录的密码

## 响应

### 230

用户登录成功，继续



# 命令：PASV

## 参数

无

## 响应

### 227

227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)

h1.h2.h3.h4 **是服务器的ip**

p1*256+p2 **是服务器打开的端口，客户端应该建立socket连接这个端口，作为数据连接**



# 命令：QUIT

## 参数

无

## 响应

### 221

服务器关闭控制连接，已注销



# 命令：PORT h1,h2,h3,h4,p1,p2

## 参数

h1,h2,h3,h4,p1,p2

## 解释

h1,h2,h3,h4 **客户端的ip**

p1*256+p2 **客户端应打开这个端口，由服务端来连接它**

## 响应

### 200

命令OK





# 命令：TYPE <type>

## 参数

type=A **ASCII模式**

type=B **Binary模式**

## 响应

### 200

命令OK



# 命令：MODE <mode>

暂时不实现。默认流模式。



# 命令：STRU <structure>

暂时不实现。默认文件结构。



# 命令：RETR <pathname>

## 参数

pathname：要取回文件的路径名，比如

```
RETR /softwares/office365.iso
```

## 服务端的行为

1.先在数据连接上以JSON形式，UTF-8编码的形式发送一个FileMeta，以<CRLF>结尾。

```
{"filename":"hello.java","size":1024,"type":"FILE"}
```

2.在数据连接的InputStream上按字节写文件

3.如果是目录，循环执行1和2

4.全部发送完成后，发送一个<CRLF>

**我的想法：如果是目录，也许可以在服务端用压缩工具先把目录压缩成一个文件，然后当成单个文件发送，再在客户端解压。所以在FileMeta中预留了type字段，如果type=“DIRECTORY”，则客户端应该解压收到的文件。**

## 客户端的行为

按一定方法读取服务端传来的数据，写入磁盘。

## 响应

### 150->250，150->425，150->426

150 File status okay; about to open data connection. （文件状态 OK，将打开数据连接）

250 Requested file action okay, completed. （请求文件动作 OK，完成）

425 Can't open data connection. （不能打开数据连接）

426 Connection closed; transfer aborted. （连接关闭，放弃传输）



# 命令：STOR <pathname>

## 参数

pathname：要上传文件的路径名，比如

```
RETR /softwares/office365.iso
```

## 客户端的行为

1.先在数据连接上以JSON形式，UTF-8编码的形式发送一个FileMeta，以<CRLF>结尾。

```
{"filename":"hello.java","size":1024,"type":"FILE"}
```

2.在数据连接的InputStream上按字节写文件

3.如果是目录，循环执行1和2

4.全部发送完成后，发送一个<CRLF>

## 服务端的行为

按一定方法读取服务端传来的数据，写入磁盘。

## 响应

### 150->250，150->425，150->426

150 File status okay; about to open data connection. （文件状态 OK，将打开数据连接）

250 Requested file action okay, completed. （请求文件动作 OK，完成）

425 Can't open data connection. （不能打开数据连接）

426 Connection closed; transfer aborted. （连接关闭，放弃传输）



# 命令：NOOP

## 参数

无

## 响应

200 Command okay. （命令 OK）

