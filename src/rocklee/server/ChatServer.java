package rocklee.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ChatServer
{

	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatServer.class);

	private ServerSocket serverSocket = null;

	public static final String MAIN_HALL_NAME = "MainHall";
	private ChatRoomManager main_hall = null;
	private Vector<ChatRoomManager> room_list = null;

	// this number indicates that how many users are using names like "guest###"
	private static final int MAX_GUEST_NUM = 1000;

	// this is used to keep track of the next minimum possible index number
	private volatile boolean[] guest_name_flag = null;

	private boolean listenning = true;

	private int port_num = 4444;

	public ChatServer(int port_num)
	{
		this.port_num = port_num;
		this.guest_name_flag = new boolean[MAX_GUEST_NUM];
		for (int i = 0; i < guest_name_flag.length; i++)
		{
			guest_name_flag[i] = false;
		}

		try
		{
			serverSocket = new ServerSocket(port_num);

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		log.debug("Server has been initialized!!!!");

		// set up the main hall,owner is empty
		main_hall = new ChatRoomManager(MAIN_HALL_NAME, null);

		// set up the lists for chat room
		this.room_list = new Vector<ChatRoomManager>();

		// add it to the chat room list
		this.room_list.add(main_hall);
	}

	public void startService()
	{
		while (listenning)
		{
			Socket socket = null;
			try
			{
				socket = serverSocket.accept();
			} catch (IOException e)
			{
				e.printStackTrace();
			}

			ClientWrap new_client = new ClientWrap(socket, "guest"
					+ this.getNextGuestName());

			// inform the client of the new name
			JSONObject connectJson = new JSONObject();
			connectJson.put("type", "newidentity");
			connectJson.put("identity", "guest" + this.getNextGuestName());

			// inform the new client of its guest name
			new_client.sendNextMessage(connectJson.toJSONString());

			// indicates it comes from nowhere
			new_client.setChatRoom(null);
			// set the broadcasting channel for this client
			// and add the new client to the main hall as default
			new_client.switchChatRoom(main_hall);

			new_client.start();// start to serve the client
		}
	}

	public ChatRoomManager getChatRoomById(String room_id)
	{
		for (int i = 0; i < this.room_list.size(); i++)
		{
			if (this.room_list.get(i).getRoomId().equals(room_id))
				return room_list.get(i);
		}
		return null;
	}

	public synchronized ChatRoomManager removeChatRoomById(String room_id)
	{
		for (int i = 0; i < this.room_list.size(); i++)
		{
			if (this.room_list.get(i).getRoomId().equals(room_id))
				return this.room_list.remove(i);
		}
		return null;
	}

	public synchronized boolean addChatRoom(String room_id, ClientWrap client)
	{
		ChatRoomManager tmp = new ChatRoomManager(room_id, client);
		return this.room_list.add(tmp);
	}

	public Vector<ChatRoomManager> getChatRoomList()
	{
		return this.room_list;
	}

	public void broadcastToAll(String msg)
	{
		// broad cast to all chat room channels ,one by one
		for (int i = 0; i < this.room_list.size(); i++)
		{
			this.room_list.get(i).broadCastMessage(msg);
		}
	}

	public String getNextGuestName()
	{
		int index = -1;

		// get the unused smallest flag
		for (int i = 0; i < guest_name_flag.length; i++)
		{
			// found the first index which has been used
			if (!guest_name_flag[i])
			{
				guest_name_flag[i] = true;
				index = i + 1;
				break;
			}

		}
		if (index == -1)
		{

			log.debug("Guests name has been used up!Set a larger flag array! ");
			return "New_Guest";
		}
		return ("guest" + index);

	}

	// check all the users' names online, to see if a given identity already
	// exists
	public boolean identityExist(String identity)
	{
		Vector<String> allIdentities = new Vector<String>();

		for (int i = 0; i < this.room_list.size(); i++)
		{
			allIdentities.addAll(this.room_list.get(i).getRoomMembers());
		}

		return (allIdentities.contains(identity));
	}
	
	public boolean roomIdExist(String room_id)
	{
		for (int i = 0; i < this.room_list.size(); i++)
		{
			if(this.room_list.get(i).getRoomId().equals(room_id))
				return true;			
		}
		return false;
	}
	

	public void releaseGuestIndex(int i)
	{
		this.guest_name_flag[i - 1] = false;
	}
	
	public JSONObject getRoomListJson()
	{
		JSONObject response_json = new JSONObject();

		JSONArray rooms = new JSONArray();

		for (int i = 0; i < room_list.size(); i++)
		{
			rooms.add(this.room_list.get(i).getJsonObject());
		}

		response_json.put("rooms", rooms);


		return response_json;
	}
	

	public static void main(String[] args)
	{
		ChatServer chatServer = new ChatServer(4444);
		chatServer.startService();
	}

}
