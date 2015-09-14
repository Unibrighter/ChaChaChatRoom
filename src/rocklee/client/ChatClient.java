package rocklee.client;

import java.net.Socket;

import org.apache.log4j.Logger;

public class ChatClient
{
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatClient.class);

	public static final String TYPE_NEW_IDENTITY = "newidentity";
	public static final String TYPE_INDENTITY_CHANGE = "identitychange";
	public static final String TYPE_JOIN = "join";
	public static final String TYPE_ROOM_CHANGE = "roomchange";
	public static final String TYPE_ROOM_CONTENTS = "roomcontents";
	public static final String TYPE_WHO = "who";
	public static final String TYPE_LIST = "list";
	public static final String TYPE_ROOM_LIST = "roomlist";
	public static final String TYPE_CREATE_ROOM = "createroom";
	public static final String TYPE_KICK = "kick";
	public static final String TYPE_DELETE = "delete";
	public static final String TYPE_MESSAGE = "message";
	public static final String TYPE_QUIT = "quit";
	
	private boolean online = true;

	private String client_identity = null;

	private Socket socket = null;
	private int port_num = -1;

	private Thread read = null;
	private Thread write = null;

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

	public static void main(String[] args)
	{
		ChatClient chat_client = new ChatClient("127.0.0.1", 4444);
		chat_client.startChat();
	}
}