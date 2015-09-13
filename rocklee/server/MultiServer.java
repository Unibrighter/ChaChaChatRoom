package rocklee.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import rocklee.thread.ReadThread;

public class MultiServer 
{
    ServerSocket serverSocket = null;
    boolean listening = true;
    int cliennum = 0;
    Vector<Socket> vector = new Vector<Socket>();
    public MultiServer()
    {
        try {
            serverSocket = new ServerSocket(5200);
            System.out.println("服务端启动");
            ServerWriteThread write = new ServerWriteThread(vector);    //向所有客户端发送同一消息
            write.start();
            while (listening)
            {
                Socket socket = serverSocket.accept();
                cliennum++;
                vector.addElement(socket);  //将收到的socket加入到集合中
                System.out.println("接收到 : "+cliennum+" 客户端");
                String client = "从客户端["+cliennum+"]:"+"读取";
                Thread read = new ReadThread(socket, client);
                read.start();
            }
        } catch (IOException e) {
            System.out.println("无法监听到端口");
            System.exit(-1);
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    public static void main(String[] args)
    {
        new MultiServer();
    }
}