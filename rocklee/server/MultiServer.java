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
            System.out.println("���������");
            ServerWriteThread write = new ServerWriteThread(vector);    //�����пͻ��˷���ͬһ��Ϣ
            write.start();
            while (listening)
            {
                Socket socket = serverSocket.accept();
                cliennum++;
                vector.addElement(socket);  //���յ���socket���뵽������
                System.out.println("���յ� : "+cliennum+" �ͻ���");
                String client = "�ӿͻ���["+cliennum+"]:"+"��ȡ";
                Thread read = new ReadThread(socket, client);
                read.start();
            }
        } catch (IOException e) {
            System.out.println("�޷��������˿�");
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