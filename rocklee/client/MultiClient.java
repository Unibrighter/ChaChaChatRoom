package rocklee.client;
import java.net.Socket;
import java.util.Scanner;

import rocklee.thread.ReadThread;
import rocklee.thread.WriteThread;

public class MultiClient 
{
    public static void main(String[] args) //throws Exception
    {
    	System.out.println("client starts");
        try {
            Socket socket = new Socket("127.0.0.1",4444);
            Thread read = new ReadThread(socket,"test");
            Thread write = new WriteThread(socket,"test");
            read.start();
            write.start();
        } catch (Exception e) {
            System.out.println("Error :" + e);
        }
    }
}