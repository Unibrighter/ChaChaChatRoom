package rocklee.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * This thread get the input from the socket, analyze it , and then gives proper
 * responses, like printing some info to the Standard output for example.
 * 
 * @author Kunliang WU
 * @version 2015-09-14 21:08
 * */

public class ReadThread extends Thread
{

	private Socket socket = null;
	private ChatClient chat_client = null;



	public ReadThread(Socket socket, ChatClient client)
	{
		this.socket = socket;
		this.chat_client = client;
	}

	public void run()
	{
		try
		{
			String line = "";
			BufferedReader is = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			line = is.readLine();
			while (this.chat_client.isOnline() && line != null)
			{
				System.out.println(chat_client.getIdentity() + " : " + line);
				line = is.readLine();
			}

			is.close();
			socket.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}