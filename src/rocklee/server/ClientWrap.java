package rocklee.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ClientWrap.class);
	
	
	private static JSONParser json_parser = new JSONParser();
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

	private String indentity = null;// user's identity

	private ChatRoomManager chat_room_manager = null;
	private ChatServer chat_server=null;							

	private Socket socket = null;
	private BufferedReader input = null;
	private PrintWriter output = null;

	public ClientWrap(Socket socket, String name)
	{
		this(socket);
		this.switchIdentity(name);

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
	
	public void setChatSever(ChatServer chat_server)
	{
		this.chat_server=chat_server;
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
			
			Vector<ChatRoomManager> room_list=this.chat_server.getChatRoomList();
			
			//reset the chat rooms which belonged to this owner
			for (int i = 0; i < room_list.size(); i++)
			{
				if(room_list.get(i).getRoomOwner().equals(this.getIdentity()))
					room_list.get(i).setRoomOwner(null);
			}
			
			this.chat_room_manager.removeClient(this);
						
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
	public void switchIdentity(String indentity)
	{
		String former_id=this.indentity;
		this.indentity = indentity;
		
		if (former_id==null||former_id.equals(""))
		{
			return;
		}
		
		// check if it's a "guest name"
		if (Pattern.matches("^guest[1-9]\\d*$", former_id))
		{
			int index = Integer.parseInt(former_id.substring(5));
			this.chat_server.releaseGuestIndex(index);
		}

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

		this.chat_room_manager.addClient(this);
		
		// delete the client from the original chat room list,
		if (original_room != null)
			return original_room.removeClient(this);
		else
			// this means that this user moves to the target chat room from
			// "nowhere"
			return true;

	}

	// support method ,extract json info for further usage
	private static JSONObject parserGiveString(String msg)
	{
		JSONObject json_obj = null;
		try
		{
			json_obj = (JSONObject) ClientWrap.json_parser.parse(msg);
		} catch (ParseException e)
		{
			e.printStackTrace();
		}
		return json_obj;
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

				// see what the client message says and act accordingly


				this.handleRequest(input_from_client);

				this.chat_room_manager.broadCastMessage(input_from_client);

			}

		} 
//		catch (SocketException e)
//		{
//			log.debug("TCP connection is forced to terminate");
//			this.disonnect();
//		} 
		
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private void handleRequest(String input_from_client)
	{
		JSONObject msg_json = ClientWrap
				.parserGiveString(input_from_client);
		JSONObject response_json=new JSONObject();
		String request_type=(String) msg_json.get("type");
		
		//=============================================================
		//TYPE_INDENTITY_CHANGE
		
		if (request_type.equals(ClientWrap.TYPE_INDENTITY_CHANGE))
		{
			// apply to change the identity from client

			// since a chat room has been bundled with a client wrap
			// if the identity of a client wrap is changed
			// the owner name will been taken care with automatically

			String msg_identity = (String) msg_json.get("identity");

			response_json.put("type", TYPE_NEW_IDENTITY);
			String former_id = this.getIdentity();
			response_json.put("formor", former_id);

			if (chat_server.checkIdentityOccupied(msg_identity))
			{
				response_json.put("identity", former_id);

				// only the client with the failed request will get response
				this.sendNextMessage(response_json.toJSONString());

			} else
			{// successful change

				response_json.put("identity", msg_identity);

				this.switchIdentity(msg_identity);

				this.chat_server.broadcastToAll(response_json
						.toJSONString());
			}
		}

		
		if (request_type.equals(ClientWrap.TYPE_MESSAGE))
		{
			
		}
		
		// TODO other response if and method
	}

	
	
}
