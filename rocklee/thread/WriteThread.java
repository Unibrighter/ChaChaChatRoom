package rocklee.thread;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class WriteThread extends Thread
{
	//going to be abandoned
	
	Socket socket = null;
	String client = null;

	public WriteThread(Socket socket, String client)
	{
		this.socket = socket;
		this.client = client;
	}

	public void run()
	{
		try
		{
			PrintWriter os = new PrintWriter(socket.getOutputStream());
			Scanner scanner = new Scanner(System.in);
			String readline = scanner.nextLine();
			while (readline != null)
			{
				os.println(readline);
				os.flush();
				readline = scanner.nextLine();
			}
			os.close();
			socket.close();
		} catch (Exception e)
		{
			System.out.println("Error : " + e);
		}
	}
}