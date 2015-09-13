package rocklee.server;

import java.io.*;
import java.net.Socket;

/**
 * This class is the wrap for the connection of a single client , and includes
 * socket and input and output stream.
 *
 * @author Kunliang WU
 * 
 * @version 2015-09-13 14:10
 * 
 * */

public class ClientWrap extends Thread
{

	private String indentity = null;// user's identity
	private Object channel_lock = null;//the lock to control concurrent access to the broadcasting channel

	
	
	
	private Socket socket = null;
	private BufferedReader input = null;
	private PrintWriter output = null;

	ClientWrap(Socket socket, String name, Object channel_lock)
	{
		this(socket);
		this.setIdentity(name);

	}

	ClientWrap(Socket socket)
	{
		this.socket = socket;

		// initialize the input and output stream (With BufferReader and
		// PrintWriter)
		try
		{
			this.input = new BufferedReader(new InputStreamReader(
					this.socket.getInputStream()));

			this.output = new PrintWriter(socket.getOutputStream());

		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// TODO 需要将这里的String替换成 json
	public String getNextMessage()
	{
		String msg = null;

		try
		{
			msg = this.input.readLine();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return msg;

	}

	// TODO 需要将这里的String替换成 json
	public void sendNextMessage(String msg)
	{
		this.output.println(msg);
		this.output.flush();
	}

	// end the connection and shut the streams up
	public void endConnection()
	{
		try
		{
			this.input.close();
			this.output.close();
			this.socket.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public String getIdentity()
	{
		return this.indentity;
	}

	public void setIdentity(String indentity)
	{
		this.indentity = indentity;
	}
	
	public void setChannelLock(Object lock_dummy)
	{
		this.channel_lock=lock_dummy;
	}
	

	public void run()
	{

	}

}
