package rocklee.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	}
	

	public void run()
	{
		try
		{
			String line = "";
			this.is = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			line = is.readLine();
			while (this.chat_client.isOnline() && line != null)
			{
				
				// System.out.println(chat_client.getIdentity() + " : " + line);
				this.handleResponse(line);
				line = is.readLine();
				
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
			System.out.println(response_json);
		} catch (ParseException e)
		{
			e.printStackTrace();
		}

		String type = (String) response_json.get("type");

		// =========================================================
		// new identity
		if (type.equals(ChatClient.TYPE_NEW_IDENTITY))
		{
			String former = (String) response_json.get("former");
			String identity = (String) response_json.get("identity");
			if (former == null || former.equals(""))
			{
				// first welcome message from server, get the new name from
				// server as
				// guest#
				System.out.println("Connected to localhost as " + identity);
			}

			else
			{
				if (former.equals(identity)
						&& former.equals(this.chat_client.getIdentity()))
				{// identity remains the same
					log.info("Requested identity invalid or in use");
					return;
				}
				System.out.println(former + " is now " + identity);
			}
			this.chat_client.setIdentity(identity);
			return;
		}

		// ==========================================================
		// room change
		if (type.equals(ChatClient.TYPE_ROOM_CHANGE))
		{
			String identity = (String) response_json.get("identity");
			String former = (String) response_json.get("former");
			String room_id = (String) response_json.get("roomid");
			if ((former == null || former.equals(""))
					&& room_id.equals("MainHall"))
			{
				// just connect to server, move from "nowhere" to main hall
				System.out.println(identity + " moves to MainHall");
				return;
			}

			if (room_id == null || room_id.equals(""))
			{
				// the destination is empty, indicates that user is going to
				// disconnect
				System.out.println(identity + " leaves MainHall");
				System.out.println(response_json);
				
				if(this.chat_client.getIdentity().equals(identity))
				{//this client is going to disconnect itself
					this.chat_client.setOnline(false);
				}
				
				return;
			}

			if (former.equals(identity)
					&& former.equals(this.chat_client.getIdentity()))
			{// room id remains the same
				log.info("The requested room is invalid or non existent.");
				return;
			}
			System.out.println(identity + " moves from " + former + " to "
					+ room_id);
			this.chat_client.setRoomId(room_id);
			return;
		}

		// ==========================================================
		// room contents
		if (type.equals(ChatClient.TYPE_ROOM_CONTENTS))
		{

			String room_id = (String) response_json.get("roomid");
			JSONArray identities = (JSONArray) response_json.get("identities");
			String owner = (String) response_json.get("owner");
			
			Boolean success=(Boolean) response_json.get("success");
			
			
			//if request failed,print the failing message
			if(success==null||!success)
			{
				log.info("The requested room is invalid or non existent.");
				return;
			}
			
			String name_list = room_id + " contains";
			for (int i = 0; i < identities.size(); i++)
			{
				String tmp_name = (String) identities.get(i);
				if (tmp_name.equals(owner))
					tmp_name += "*";
				name_list += (" " + tmp_name);
			}
			System.out.println(name_list);
			return;
		}

		// ==========================================================
		// room list
		if (type.equals(ChatClient.TYPE_ROOM_LIST))
		{

			JSONArray rooms = (JSONArray) response_json.get("rooms");
			Boolean success = (Boolean) response_json.get("success");
			// fuck this stupid protocol , i need a indicator to show if the
			// operation is successful or not
			if (success == null)
			{
				for (int i = 0; i < rooms.size(); i++)
				{
					JSONObject room_json_obj = (JSONObject) rooms.get(i);
					System.out.println((String) room_json_obj.get("roomid")
							+ ": " + (Long) room_json_obj.get("count"));
				}

				return;
			} else
			{
				if (success)
				{
					System.out.println("Room "
							+ this.chat_client.getRequestNewRoomId()
							+ " created");
					return;
				} else
					System.out.println("Room "
							+ this.chat_client.getRequestNewRoomId()
							+ " is invalid or already in use.");
				return;
			}

		}

		// ==========================================================
		// message
		if (type.equals(ChatClient.TYPE_MESSAGE))
		{
			String identity = (String) response_json.get("identity");
			String content = (String) response_json.get("content");
			System.out.println(identity+": "+content);
		}

	}
}