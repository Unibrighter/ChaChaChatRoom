package rocklee.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReadThread extends Thread
{
	
	//going to be abandoned
	Socket socket = null;
	String client = null;

	public ReadThread(Socket socket, String client)
	{
		this.socket = socket;
		this.client = client;
	}

	public void run()
	{
		try
		{
			String line = "";
			BufferedReader is = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			line = is.readLine();
			while (line != null)
			{
				System.out.println(client + " : " + line);
				line = is.readLine();
			}
			is.close();
			socket.close();
		} catch (Exception e)
		{
			System.out.println("Error : " + e);
		}
	}
}