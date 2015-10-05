package rocklee.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rocklee.security.DESUtil;
import rocklee.security.RSAUtil;
import rocklee.utility.Config;

/**
 * This thread get the input from the socket, analyze it , and then gives proper
 * responses, like printing some info to the Standard output for example.
 * 
 * Also , it carries the responsibility to indicate which chat room the user is
 * currently in and something to do with the output format
 * 
 * @author Kunliang WU
 * @version 2015-09-14 21:08
 * */

public class ReadThread extends Thread
{

	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ReadThread.class);

	private Socket socket = null;
	private ChatClient chat_client = null;

	private BufferedReader is = null;
	private JSONParser json_parser = null;

	public ReadThread(Socket socket, ChatClient client)
	{
		this.socket = socket;
		this.chat_client = client;
		this.json_parser = new JSONParser();
		try
		{
			this.is = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public String getMD5HashSignature()
	{
		JSONObject response_json = null;
		try
		{

			String cipher_input = is.readLine();

			response_json = (JSONObject) json_parser.parse(RSAUtil
					.decryptUsingPublicKey(this.chat_client.getRSAPublicKey(),
							cipher_input));

		} catch (ParseException e)
		{
			log.error("Some thing wrong with the responding signature!!!");
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e)
		{
			log.error("Something wrong with the connection!!");
			e.printStackTrace();
			System.exit(-2);
		}

		String type = (String) response_json.get("type");

		if (!type.equals(Config.TYPE_SIGNATURE))
		{
			log.error("Not correct responding with a signature!!!");
			return null;
		} else

			return (String) response_json.get("content");

	}

	public boolean getLoginResult()
	{
		JSONObject response_json = null;
		String cipher_input = null;
		try
		{
			cipher_input = is.readLine();
			String plain_input = DESUtil.decrypt(cipher_input,
					this.chat_client.getSessionKey());

			response_json = (JSONObject) json_parser.parse(plain_input);
			

		} catch (ParseException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		String type = (String) response_json.get("type");
		String identity = (String) response_json.get("identity");
		
		if (type.equals(Config.TYPE_LOGIN_SUCCESS))
		{
			this.chat_client.setIdentity(identity);
			log.debug("Connected to localhost as " + identity);
			System.out.println("Connected to localhost as " + identity);			
			return true;
		} else if (type.equals(Config.TYPE_LOGIN_FAILURE))
			return false;
		else
		{
			log.error("Not correct responding with a login attempt!!!");
			return false;
		}
	}

	public String getNextLineAsPlainText(String cipherText) throws Exception
	{
		return DESUtil.decrypt(cipherText, this.chat_client.getSessionKey());
	}

	public void run()
	{
		try
		{
			String line = "";

			line = this.getNextLineAsPlainText(is.readLine());
			while (this.chat_client.isOnline() && line != null)
			{

				this.handleResponse(line);
				line = this.getNextLineAsPlainText(is.readLine());
			}

			is.close();
			socket.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void handleResponse(String raw_input)
	{
		JSONObject response_json = null;

		try
		{
			response_json = (JSONObject) json_parser.parse(raw_input);

		} catch (ParseException e)
		{
			e.printStackTrace();
		}

		String type = (String) response_json.get("type");

		switch (type)
		{
		case Config.TYPE_NEW_IDENTITY:
			this.handleType_new_identity(response_json);
			break;

		case Config.TYPE_ROOM_CHANGE:
			this.handleType_room_change(response_json);
			break;
		case Config.TYPE_ROOM_CONTENTS:
			this.handleType_room_contents(response_json);
			break;

		case Config.TYPE_ROOM_LIST:
			this.handleType_room_list(response_json);
			break;

		case Config.TYPE_MESSAGE:
			this.handleType_message(response_json);
			break;

		default:
			System.out.println("Something went wrong with the TYPE given!!!");
		}

	}

	private boolean handleType_new_identity(JSONObject response_json)
	{
		String former = (String) response_json.get("former");
		String identity = (String) response_json.get("identity");

		if (former.equals(identity)
				&& former.equals(this.chat_client.getIdentity()))
		{// identity remains the same
			log.debug("Requested identity invalid or in use");
			System.out.println("Requested identity invalid or in use");
			return false;
		}
		log.debug(former + " now is " + identity);
		System.out.println(former + " now is " + identity);

		this.chat_client.setIdentity(identity);
		return true;
	}

	private boolean handleType_room_change(JSONObject response_json)
	{
		String identity = (String) response_json.get("identity");
		String former = (String) response_json.get("former");
		String room_id = (String) response_json.get("roomid");
		if ((former == null || former.equals("")) && room_id.equals("MainHall"))
		{
			// just connect to server, move from "nowhere" to main hall
			log.debug(identity + " moves to MainHall");
			System.out.println(identity + " moves to MainHall");
			this.chat_client.setRoomId("MainHall");
			return true;
		}

		if (room_id == null || room_id.equals(""))
		{
			// the destination is empty, indicates that user is going to
			// disconnect
			log.debug(identity + " leaves MainHall");
			System.out.println(identity + " leaves MainHall");

			if (this.chat_client.getIdentity().equals(identity))
			{// this client is going to disconnect itself
				this.chat_client.setOnline(false);
			}

			return true;
		}

		if (former.equals(identity)
				&& former.equals(this.chat_client.getIdentity()))
		{// room id remains the same
			log.debug("The requested room is invalid or non existent.");
			System.out
					.println("The requested room is invalid or non existent.");
			return false;
		}

		// basic case, move from one room to another
		System.out.println(identity + " moves from " + former + " to "
				+ room_id);
		this.chat_client.setRoomId(room_id);
		return true;
	}

	private boolean handleType_room_contents(JSONObject response_json)
	{
		String room_id = (String) response_json.get("roomid");
		JSONArray identities = (JSONArray) response_json.get("identities");
		String owner = (String) response_json.get("owner");

		String name_list = room_id + " contains";

		// print the information of this room
		for (int i = 0; i < identities.size(); i++)
		{
			String tmp_name = (String) identities.get(i);
			if (tmp_name.equals(owner))
				tmp_name += "*";
			name_list += (" " + tmp_name);
		}
		System.out.println(name_list);
		return true;
	}

	private boolean handleType_room_list(JSONObject response_json)
	{
		boolean found = false;
		JSONArray rooms = (JSONArray) response_json.get("rooms");

		String request_room_name = this.chat_client.getRequestNewRoomId();

		// we are not request any room to be created
		// so we just print the existing room id and number of people inside
		if (!(request_room_name == null || request_room_name.equals("")))
		{
			for (int i = 0; i < rooms.size(); i++)
			{
				JSONObject room_json_obj = (JSONObject) rooms.get(i);
				System.out.println((String) room_json_obj.get("roomid") + ": "
						+ (Long) room_json_obj.get("count"));
			}
			return true;
		} else
		{
			// we have sent our "creating request"
			for (int i = 0; i < rooms.size(); i++)
			{
				JSONObject room = (JSONObject) rooms.get(i);
				if (((String) room.get("roomid")).equals(request_room_name))
				{
					// the room we request to create now appears on the list
					// we can assume that the operation is a success
					found = true;
					log.debug("Room " + request_room_name + " created");
					System.out
							.println("Room " + request_room_name + " created");
					this.chat_client.setRequestNewRoomId("");
					break;
				}
			}

			if (!found)
			{
				// code reaches here , the request has not successully created a
				// new room
				log.debug("Room " + this.chat_client.getRequestNewRoomId()
						+ " is invalid or already in use.");
				System.out.println("Room "
						+ this.chat_client.getRequestNewRoomId()
						+ " is invalid or already in use.");

			}
			this.chat_client.setRequestNewRoomId("");
			return found;
		}

	}

	private boolean handleType_message(JSONObject response_json)
	{
		String identity = (String) response_json.get("identity");
		String content = (String) response_json.get("content");
		System.out.println(identity + ": " + content);
		return true;
	}

}