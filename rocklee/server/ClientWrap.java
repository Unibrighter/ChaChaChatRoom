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

	private ChatRoomManager chat_room_manager = null; // the chatting group

	private Socket socket = null;
	private BufferedReader input = null;
	private PrintWriter output = null;

	public ClientWrap(Socket socket, String name)
	{
		this(socket);
		this.setIdentity(name);

	}

	public ClientWrap(Socket socket)
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

	// Set up the chatRoomManager through which we can get access to other
	// clients
	public void setUpChatRoom(ChatRoomManager chatRommManager)
	{
		this.chat_room_manager = chatRommManager;
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
	public void disonnect()
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

	// get the identity for the client at the Server side
	public String getIdentity()
	{
		return this.indentity;
	}

	// set the new identity for the client at the Server side
	public void setIdentity(String indentity)
	{
		this.indentity = indentity;
	}

	// the thread that deals with a single client Connection
	// provide the service input and output
	public void run()
	{
		// get the input from the input stream
		String input_from_client = null;

		try
		{
			while ((input_from_client = input.readLine()) != null)
			{// block and will wait for the input from client
				// TODO we need to take care of the command and prase the json
				// information here

				this.chat_room_manager.broadCastMessage(input_from_client);

			}
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
