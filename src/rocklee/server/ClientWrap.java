package rocklee.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

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
	public void setChatRoom(ChatRoomManager chatRommManager)
	{
		this.chat_room_manager = chatRommManager;
	}

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

	// if msg with the json string does not end with character '\n'
	// then we need to attach '\n' to its end
	public void sendNextMessage(String msg)
	{
		if (!msg.endsWith("\n"))
			msg += "\n";

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

	// join one given chat room ,if already in other room ,then return false;
	public boolean switchChatRoom(ChatRoomManager targetRoom)
	{
		// if the targetRoom does not exist ,return false
		if (targetRoom == null)
			return false;

		ChatRoomManager original_room = this.chat_room_manager;

		// add the client to the new chat room before leave the original
		this.chat_room_manager = targetRoom;

		// delete the client from the original chat room list,
		// TODO put a notice here before leave? or make this notice at where
		// this method is called

		if (original_room != null)
			return original_room.removeClient(this);
		else
			// this means that this user moves to the target chat room from
			// "nowhere"
			return true;

	}

	// overwrite this to support vector.contains(obj)
	// if the identity are the same, then we consider the two instances of
	// ClientWrap are the same
	@Override
	public boolean equals(Object obj)
	{
		return this.getIdentity().equals(((ClientWrap) obj).getIdentity());
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

		} catch (SocketException e)
		{
			// TODO ,tcp connection is forced to terminate
			// do sth to deal with the situation
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}

}
