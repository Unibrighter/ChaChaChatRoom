package rocklee.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.jws.soap.SOAPBinding.Use;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rocklee.security.DESUtil;
import rocklee.security.RSAUtil;
import rocklee.utility.Config;
import rocklee.utility.UserProfile;
import sun.security.krb5.internal.crypto.RsaMd5CksumType;

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

	private String DES_sessionKeyRoot = null;
	private UserProfile user = null;

	private ChatRoomManager chat_room_manager = null;
	private ChatServer chat_server = null;

	// socket and stream
	private Socket socket = null;
	private BufferedReader input = null;
	private PrintWriter output = null;

	private boolean online = true;

	public boolean isOnline()
	{
		return this.online;
	}

	public void setOnline(boolean online)
	{
		this.online = online;
	}

	public ClientWrap(Socket socket, UserProfile user)
	{
		this(socket);
		this.user = user;
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
	public void sendNextMessage(String msg, boolean DES_encrypt)
	{
		if (DES_encrypt)
		{
			try
			{
				msg = DESUtil.encrypt(msg, this.DES_sessionKeyRoot);
			} catch (Exception e)
			{

				e.printStackTrace();
			}
		}
			
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

	public void setUserProfile(UserProfile user_profile)
	{
		this.user = user_profile;
	}

	// get the identity for the client at the Server side
	public UserProfile getUserProfile()
	{
		return this.user;
	}

	// set the new identity for the client at the Server side
	public void switchIdentity(String identity)
	{
		String former_id = this.user.getUserIdentity();
		this.user.setUserIdentity(identity);

		if (former_id == null || former_id.equals(""))
		{
			return;
		}

		// check if the older identity was a "guest name"
		if (Pattern.matches("^guest[1-9]\\d*$", former_id))
		{
			int index = Integer.parseInt(former_id.substring(5));
			this.chat_server.releaseGuestIndex(index);
		}

	}

	// join one given chat room, and then leave the original one
	public boolean switchChatRoom(ChatRoomManager targetRoom)
	{
		// if the targetRoom does not exist ,return false
		if (targetRoom == null)
			return false;

		// still being banned
		if (targetRoom.hasBannedUserNum(this.user.getUserNum()))
			return false;

		ChatRoomManager original_room = this.chat_room_manager;

		// add the client to the new chat room before leaving the original
		this.chat_room_manager = targetRoom;

		this.chat_room_manager.addClient(this);

		// change the room tag of the user
		this.user.setCurrentRoomId(targetRoom.getRoomId());

		// delete the client from the original chat room list
		if (original_room != null)
		{
			return original_room.removeClient(this);
		}

		else
			// this means that this user moves to the target chat room from
			// "nowhere"
			return true;

	}

	// if the two instances of ClientWrap have the same UserProfile
	// then we consider it's the same clientWrap
	// this method is override for Vector<ClientWrap>.contain
	@Override
	public boolean equals(Object obj)
	{
		return this.user.getUserNum() == ((UserProfile) obj).getUserNum();
	}

	private void phaseOne()
	{
		log.warn("=====================1.>PHase I. to verify the server's identity.=====================");

		String cipher_input = null;
		try
		{
			cipher_input = this.input.readLine();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		JSONObject request_obj = this.string2Json(RSAUtil
				.decryptUsingPrivateKey(this.chat_server.getRSAPrivateKey(),
						cipher_input));
		JSONObject response_obj = new JSONObject();

		String type = (String) request_obj.get("type");
		if (type.equals(Config.TYPE_RSA_VERIFY))
		{// the client asked to check the server's identity

			String content = (String) request_obj.get("content");
			response_obj.put("type", Config.TYPE_SIGNATURE);
			response_obj.put("content", RSAUtil.stringMD5(content));

			String cipher_output = RSAUtil.encryptUsingPrivateKey(
					this.chat_server.getRSAPrivateKey(),
					response_obj.toJSONString());

			this.sendNextMessage(cipher_output,false);
		}

		else
		{// not the json we expect
			this.disonnect();
		}
	}

	private void phaseTwo()
	{
		log.warn("=====================2.>PHase II. to negotiate the session key.=====================");

		String cipher_input = null;
		try
		{
			cipher_input = this.input.readLine();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		JSONObject request_obj = this.string2Json(RSAUtil
				.decryptUsingPrivateKey(this.chat_server.getRSAPrivateKey(),
						cipher_input));
		JSONObject response_obj = new JSONObject();

		String type = (String) request_obj.get("type");

		// now we shall deal with the confidential
		if (type.equals(Config.TYPE_LOGIN))
		{
			String identity = (String) request_obj.get("identity");
			String password_hash = (String) request_obj.get("password");

			if (this.loginOrRegisterConfidential(identity, password_hash))
			{// successfully logged in
				response_obj.put("type", Config.TYPE_LOGIN_SUCCESS);
				response_obj.put("identity", this.user.getUserIdentity());

				// use the session key sent from the client
				this.DES_sessionKeyRoot = (String) request_obj
						.get("sessionkey");
			}

			else
			{
				response_obj.put("type", Config.TYPE_LOGIN_FAILURE);
			}

			String cipher_output = null;
			try
			{
				cipher_output = DESUtil.encrypt(response_obj.toJSONString(),
						this.DES_sessionKeyRoot);
			} catch (Exception e)
			{

				e.printStackTrace();
			}

			this.sendNextMessage(cipher_output,false);
		}
	}

	// This method starts even before the thread start
	// We use it to initial the info for a secure communication
	// and bind a socket with a new or an existing UserProfile
	public void prepareSecureChannel()
	{
		this.phaseOne();
		this.phaseTwo();
	}

	private boolean loginOrRegisterConfidential(String identity,
			String password_MD5Hash)
	{

		UserProfile user = null;

		// register
		if (identity == null || identity.equals(""))
		{// no identity is given ,we will assign it with a new guest id

			user = new UserProfile(this.chat_server.registered_user_count++,
					this.chat_server.getNextGuestName(), password_MD5Hash, "");

		}

		// login
		else
		{
			user = this.chat_server.getUserByIdentity(identity);
			user.setCurrentRoomId("");
			if (user == null || user.isOnline())
			{// if the user does not exist or is currently online
				return false;
			}
		}

		user.setOnline(true);
		this.chat_server.bindUserProfileAndClient(user, this);

		log.warn("A new Client has connected ! And logged in as : "
				+ user.getUserIdentity() + " . client number is : "
				+ user.getUserNum());

		return true;
	}

	// the thread that deals with a single client Connection
	// provide the service input and output
	public void run()
	{
		// get the input from the input stream
		String input_from_client = null;
		String plaintext_msg=null;
		try
		{
			while (online)
			{// block and will wait for the input from client

				input_from_client = input.readLine();
				plaintext_msg=DESUtil.decrypt(input_from_client, this.DES_sessionKeyRoot);
				
				if (input_from_client != null)
				{

					log.warn(this.user.getUserIdentity() + ">>>>>"
							+ plaintext_msg);

					this.handleRequest(plaintext_msg);
				}

			}

		} catch (SocketException e)
		{

			log.warn("The connection has been cut because of the SocketException!!!");
			this.handleRequest("{\"type\":\"quit\"}");

		}

		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	private JSONObject string2Json(String raw_input)
	{
		JSONObject msg_json = null;
		try
		{
			msg_json = (JSONObject) ClientWrap.json_parser.parse(raw_input);
		} catch (ParseException e)
		{

			e.printStackTrace();
		}
		return msg_json;
	}

	private void handleRequest(String raw_input)
	{
		JSONObject msg_json = string2Json(raw_input);
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
		String former_id = this.user.getUserIdentity();
		response_json.put("former", former_id);

		if (!Config.validIdentity(msg_identity)
				|| chat_server.getUserByIdentity(msg_identity) != null)
		{// request identity invalid or has been occupied already

			response_json.put("identity", former_id);

			// only the client with the failed request will get response
			this.sendNextMessage(response_json.toJSONString(),true);
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
		response_json.put("identity", this.user.getUserIdentity());
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
				this.sendNextMessage(response_json.toJSONString(),true);

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

			this.sendNextMessage(response_json.toJSONString(),true);
			return true;
		}
	}

	private boolean handleType_list(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		response_json = this.chat_server.getRoomListJson();

		response_json.put("type", Config.TYPE_ROOM_LIST);

		this.sendNextMessage(response_json.toJSONString(),true);

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
		{// successful created a new room
			this.chat_server.addChatRoom(room_id, this.user);
			response_json = this.chat_server.getRoomListJson();
			this.sendNextMessage(response_json.toJSONString(),true);
			return true;
		}
	}

	private boolean handleType_kick(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();
		String room_id = (String) msg_json.get("roomid");
		ChatRoomManager target_room = null;

		// no such a room under the given name
		if ((target_room = this.chat_server.getChatRoomById(room_id)) == null)
			return false;

		// the user who sends the command has the owner's number, it's the right
		// person
		if (this.user.getUserNum() == (target_room.getRoomOwner().getUserNum()))
		{
			Long time = (Long) msg_json.get("time");
			String msg_identity = (String) msg_json.get("identity");

			// get the client who is going to be kicked
			UserProfile user_to_be_kicked = this.chat_server
					.getUserByIdentity(msg_identity);

			// this user does not exist or not in the right room
			if (user_to_be_kicked == null
					|| !user_to_be_kicked.getCurrentRoomId()
							.equals(target_room))
				return false;

			long deadline = System.currentTimeMillis() + time;
			this.chat_room_manager.banUserByNum(user_to_be_kicked.getUserNum(),
					deadline);

			// inform everyone of the change
			response_json.put("type", Config.TYPE_ROOM_CHANGE);
			response_json.put("identity", user_to_be_kicked.getUserIdentity());
			response_json.put("former", this.chat_room_manager.getRoomId());
			response_json.put("roomid", "MainHall");

			this.chat_room_manager.broadCastMessage(response_json
					.toJSONString());

			// chatroom switch
			ClientWrap clientWarp = this.chat_room_manager
					.getClientWrapByUserProfile(user_to_be_kicked);
			clientWarp.switchChatRoom(chat_server.main_hall);

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

		if (this.user.getUserNum() == (target_room.getRoomOwner().getUserNum()))
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
		response_json.put("identity", this.user.getUserIdentity());
		response_json.put("content", content);

		this.chat_room_manager.broadCastMessage(response_json.toJSONString());
		return true;
	}

	private boolean handleType_quit(JSONObject msg_json)
	{
		JSONObject response_json = new JSONObject();

		// inform everyone of the change
		response_json.put("type", Config.TYPE_ROOM_CHANGE);
		response_json.put("identity", this.user.getUserIdentity());
		response_json.put("former", this.chat_room_manager.getRoomId());
		response_json.put("roomid", "");

		this.chat_room_manager.broadCastMessage(response_json.toJSONString());

		//since the profile will be left at the user_list at chat_server
		//what we have to do is change its status
		UserProfile profile=this.chat_server.getUserByNum(this.user.getUserNum());
		//change the status
		profile.setOnline(false);
		
		this.disonnect();
		return true;
	}

}
