package rocklee.client;

import java.net.Socket;

import org.apache.log4j.Logger;

import sun.net.util.IPAddressUtil;

public class ChatClient
{
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatClient.class);


	// this records the temporary request new room id to check whether the room
	// creation succeeds or not
	private String request_new_room_id = null;

	private boolean online = true;

	private String client_identity = null;
	private String room_id = null;

	private Socket socket = null;

	public Thread read = null;
	public Thread write = null;

	public ChatClient(String ipAddress, int port_num)
	{
		try
		{
			this.socket = new Socket(ipAddress, port_num);
			this.read = new ReadThread(socket, this);
			this.write = new WriteThread(socket, this);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void startChat()
	{
		this.read.start();
		this.write.start();
		try
		{
			this.write.join();
			this.read.join();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void setOnline(boolean online)
	{
		this.online = online;
	}

	public boolean isOnline()
	{
		return this.online;
	}

	public String getIdentity()
	{
		return this.client_identity;
	}

	public void setIdentity(String identity)
	{
		this.client_identity = identity;
	}

	public String getRoomId()
	{
		return this.room_id;
	}

	public void setRoomId(String room_id)
	{
		this.room_id = room_id;
	}

	public void setRequestNewRoomId(String room_id)
	{
		this.request_new_room_id = room_id;
	}

	public String getRequestNewRoomId()
	{
		return this.request_new_room_id;
	}

	public static void main(String[] args)
	{

		int port = 4444;
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("-p"))
			{
				port = Integer.parseInt(args[i + 1]);
				break;
			}

		}
		ChatClient chat_client = new ChatClient(args[0], port);
		chat_client.startChat();
		
	}
}