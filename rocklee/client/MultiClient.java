package rocklee.client;
import java.net.Socket;
import java.util.Scanner;

import rocklee.thread.ReadThread;
import rocklee.thread.WriteThread;

public class MultiClient 
{
    public static void main(String[] args) //throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入客户端名称");
        String ClientName = scanner.next();
        try {
            Socket socket = new Socket("127.0.0.1",5200);
            Thread read = new ReadThread(socket,ClientName);
            Thread write = new WriteThread(socket,ClientName);
            read.start();
            write.start();
        } catch (Exception e) {
            System.out.println("Error :" + e);
        }
    }
}