package rocklee.server;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class ServerWriteThread  extends Thread
{
	// TODO going to be abandoned
    Vector<Socket> vector = null;
    Vector<PrintWriter> oosVector = null;
    public ServerWriteThread(Vector<Socket> vector)
    {
        this.vector = vector;
        oosVector = new Vector<PrintWriter>();
    }
    public void run()
    {
        try {
            while(true)
            {
                String line;
                Scanner scanner = new Scanner(System.in);
                line = scanner.nextLine();
                if (vector != null)
                {
                    oosVector.clear();
                    for (int i=0;i<vector.size();i++)
                    {
                        try {
                            Socket socket = vector.elementAt(i);
                            PrintWriter os = new PrintWriter(socket.getOutputStream());
                            oosVector.addElement(os);
                        } catch (Exception e) {
                            vector.removeElement(i);
                        }
                    }
                    for (int i=0;i<oosVector.size();i++)
                    {
                        PrintWriter oos = oosVector.elementAt(i);
                        oos.println(line);
                        oos.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error :"+e);
        }finally{
            //关闭各个连接
            if (oosVector != null)
            {
                for (int i=0;i<oosVector.size();i++)
                {
                    PrintWriter obj = oosVector.remove(i);
                    obj.close();
                }
            }
            if (vector != null)
            {
                for (int i=0;i<vector.size();i++)
                {
                    Socket obj = vector.remove(i);
                    try {
                        obj.close();
                    } catch (Exception e2) {
                        System.out.println("socket关闭出现错误 " + e2);
                    }
                }
            }
        }
    }
}