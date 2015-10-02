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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rocklee.utility.Config;
import rocklee.utility.UserProfile;

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
	private static Logger log = Logger.getLogger(Config.class);

	private static JSONParser json_parser = new JSONParser();

	// TODO replace the usage of identity with an instance of UserDetail
	private UserProfile user = null;
	private String indentity = null;// user's identity

	private ChatRoomManager chat_room_manager = null;
	private ChatServer chat_server = null;

	private Socket socket = null;
	private BufferedReader input = null;
	private PrintWriter output = null;

	private boolean online = true;

	public void setOnline(boolean online)
	{
		this.online = online;
	}

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
		this.chat_server = chat_server;
	}

	// if msg with the json string does not end with character '\n'
	// then we need to attach '\n' to its end
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

			Vector<ChatRoomManager> room_list = this.chat_server
					.getChatRoomList();

			// reset the chat rooms which belonged to this owner
			for (int i = 0; i < room_list.size(); i++)
			{
				if (room_list.get(i).getRoomOwner().equals(this.getIdentity()))
					room_list.get(i).setRoomOwner(null);
			}

			this.chat_room_manager.removeClient(this);

			this.setOnline(false);

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

	// TODO use an instance of UserDetail inside to complete this operation
	// set the new identity for the client at the Server side
	public void switchIdentity(String indentity)
	{
		String former_id = this.indentity;
		this.indentity = indentity;

		if (former_id == null || former_id.equals(""))
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

		// still being banned
		if (targetRoom.BlockedUserByName(this))
			return false;

		ChatRoomManager original_room = this.chat_room_manager;

		// add the client to the new chat room before leave the original
		this.chat_room_manager = targetRoom;

		this.chat_room_manager.addClient(this);

		// delete the client from the original chat room list, if original room
		// is nowhere ,it will be set as null
		if (original_room != null)
		{

			boolean result = original_room.removeClient(this);
			if (original_room.getRoomOwner().equals("")
					&& original_room.getGuestNum() == 0
					&& !original_room.getRoomId().equals("MainHall"))
			{// now the room is empty
				this.chat_server.removeChatRoomById(original_room.getRoomId());
			}
			return result;
		}

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
			while (online)
			{// block and will wait for the input from client

				if ((input_from_client = input.readLine()) != null)
				{
					// see what the client message says and act accordingly
					JSONObject msg_json = null;
					log.warn(this.getIdentity() + ">>>>>" + input_from_client);

					this.handleRequest(input_from_client);

				}

			}

		} catch (SocketException e)
		{

			this.handleRequest("{\"type\":\"quit\"}");

		}

		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private void handleRequest(String raw_input)
	{
		JSONObject msg_json = null;
		try
		{
			msg_json = (JSONObject) ClientWrap.json_parser.parse(raw_input);
		} catch (ParseException e)
		{

			e.printStackTrace();
		}
		String request_type = (String) msg_json.get("type");

		switch (request_type)
		{
		case Config.TYPE_INDENTITY_CHANGE:
			this.handleType_identity_change(msg_json);
			break;

		case Config.TYPE_JOIN:
			this.handleType_join(msg_json);
			break;
		case Config.TYPE_LIST:
			this.handleType_list(msg_json);
			break;
		case Config.TYPE_CREATE_ROOM:
			this.handleType_create_room(msg_json);
			break;
		case Config.TYPE_KICK:
			this.handleType_kick(msg_json);
			break;
		case Config.TYPE_DELETE:
			this.handleType_delete(msg_json);
			break;
		case Config.TYPE_MESSAGE:
			this.handleType_message(msg_json);
			break;
		case Config.TYPE_QUIT:
			this.handleType_quit(msg_json);
			break;
		default:
			System.out.println("Invalid type given from the client's json!!!");
			break;
		}

	}

	private boolean handleType_identity_change(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();

		// apply to change the identity from client

		// since a chat room has been bundled with a client wrap
		// if the identity of a client wrap is changed
		// the owner name will been taken care with automatically

		String msg_identity = (String) msg_json.get("identity");

		response_json.put("type", Config.TYPE_NEW_IDENTITY);
		String former_id = this.getIdentity();
		response_json.put("former", former_id);

		if (!Config.validIdentity(msg_identity)
				|| chat_server.identityExist(msg_identity))
		{// request identity invalid or has been occupied already

			response_json.put("identity", former_id);

			// only the client with the failed request will get response
			this.sendNextMessage(response_json.toJSONString());
			return false;

		} else
		{// successful change

			response_json.put("identity", msg_identity);

			this.switchIdentity(msg_identity);

			this.chat_server.broadcastToAll(response_json.toJSONString());
			return true;
		}
	}

	private boolean handleType_join(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String room_id = (String) msg_json.get("roomid");
		String former = this.chat_room_manager.getRoomId();

		response_json.put("type", Config.TYPE_ROOM_CHANGE);
		response_json.put("identity", this.getIdentity());
		response_json.put("former", former);

		if (!Config.validIdentity(room_id) || !chat_server.roomIdExist(room_id))
		{// request room_id invalid or not exist
			return false;
		} else
		{// successful change attempt

			if (this.switchChatRoom(chat_server.getChatRoomById(room_id)))
			{
				response_json.put("roomid", room_id);
				this.chat_server.broadcastToAll(response_json.toJSONString());

				// if the destination is going to be main hall
				// there would be two extra messages

				if (room_id.equals("MainHall"))
				{
					this.handleRequest("{\"type\":\"list\"}");
					this.handleRequest("{\"type\":\"who\""
							+ ",\"roomid\":\"MainHall\"}");
				}
				return true;
			}

			else
			{
				response_json.put("roomid", former);

				// only the client with the failed request will get response
				this.sendNextMessage(response_json.toJSONString());

				return false;
			}
		}
	}

	private boolean handleType_who(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();

		String room_id = (String) msg_json.get("roomid");

		response_json.put("type", Config.TYPE_ROOM_CONTENTS);

		if (!Config.validIdentity(room_id) || !chat_server.roomIdExist(room_id))
		{// request identity invalid or has been occupied already
			System.out.println("invalid room_id");
			return false;

		} else
		{// successful get the list
			JSONArray identities = new JSONArray();
			Vector<String> members = this.chat_room_manager.getRoomMembers();
			for (int i = 0; i < members.size(); i++)
			{
				identities.add(members.get(i));
			}

			response_json.put("identities", identities);
			response_json.put("roomid", room_id);
			response_json.put("owner", this.chat_room_manager.getRoomOwner());

			this.sendNextMessage(response_json.toJSONString());
			return true;
		}
	}

	private boolean handleType_list(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		response_json = this.chat_server.getRoomListJson();

		response_json.put("type", Config.TYPE_ROOM_LIST);

		this.sendNextMessage(response_json.toJSONString());

		return true;
	}

	private boolean handleType_create_room(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String room_id = (String) msg_json.get("roomid");

		response_json.put("type", Config.TYPE_ROOM_LIST);

		if (!Config.validIdentity(room_id) || chat_server.roomIdExist(room_id))
		{// request room_id invalid or has been occupied already

			return false;

		} else
		{// successful create

			this.chat_server.addChatRoom(room_id, this);
			response_json = this.chat_server.getRoomListJson();
			this.sendNextMessage(response_json.toJSONString());
			return true;
		}
	}

	private boolean handleType_kick(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String room_id = (String) msg_json.get("roomid");
		ChatRoomManager target_room = null;

		// TODO this Authentication needs to use UserProfile
		// authenticate the right to do so
		if ((target_room = this.chat_server.getChatRoomById(room_id)) == null)
			return false;

		// TODO this Authentication needs to use UserProfile
		if (this.getIdentity().equals(target_room.getRoomOwner()))
		{
			Long time = (Long) msg_json.get("time");
			String msg_identity = (String) msg_json.get("identity");

			// get the client who is going to be kicked
			ClientWrap target_client = this.chat_room_manager
					.getClientWarpByName(msg_identity);

			// TODO black list here should use a map of <UserProfile,Long>
			if (target_client == null)
				return false;

			long deadline = System.currentTimeMillis() + time;
			this.chat_room_manager.banIdentity(target_client, deadline);

			// inform everyone of the change
			response_json.put("type", Config.TYPE_ROOM_CHANGE);
			response_json.put("identity", target_client.getIdentity());
			response_json.put("former", this.chat_room_manager.getRoomId());
			response_json.put("roomid", "MainHall");

			this.chat_room_manager.broadCastMessage(response_json
					.toJSONString());

			// chatroom switch
			target_client.switchChatRoom(chat_server.main_hall);

			return true;
		}

		else
			return false;// do nothing
	}

	private boolean handleType_delete(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String room_id = (String) msg_json.get("roomid");
		ChatRoomManager target_room = null;

		// there is no such a room
		if ((target_room = this.chat_server.getChatRoomById(room_id)) == null)
			return false;

		// TODO this Authentication needs to use UserProfile
		if (this.getIdentity().equals(target_room.getRoomOwner()))
		{
			Vector<ClientWrap> people_inside = target_room.getAllClients();

			for (int i = 0; i < people_inside.size(); i++)
			{
				people_inside.get(i).switchChatRoom(this.chat_server.main_hall);
			}

			this.chat_server.removeChatRoomById(room_id);
			return true;
		}

		else
			return false;// do nothing
	}

	private boolean handleType_message(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String content = (String) msg_json.get("content");
		response_json.put("type", Config.TYPE_MESSAGE);
		response_json.put("identity", this.getIdentity());
		response_json.put("content", content);

		this.chat_room_manager.broadCastMessage(response_json.toJSONString());
		return true;
	}

	private boolean handleType_quit(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();

		// inform everyone of the change
		response_json.put("type", Config.TYPE_ROOM_CHANGE);
		response_json.put("identity", this.getIdentity());
		response_json.put("former", this.chat_room_manager.getRoomId());
		response_json.put("roomid", "");

		this.chat_room_manager.broadCastMessage(response_json.toJSONString());

		this.disonnect();
		return true;
	}

}
