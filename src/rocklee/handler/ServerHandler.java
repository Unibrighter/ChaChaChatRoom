package rocklee.handler;

import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rocklee.server.ChatServer;
import rocklee.server.ClientWrap;

/**
 * This class takes the string read from the sockets as input and then parse it
 * into a json object. Then it performs action according to the "type" attribute
 * in the json object.
 * 
 * This class handles the commands and messages based on the ClientWrap and
 * ChatServer
 * 
 * @author Kunliang WU
 * @version 2015-09-14 11:40
 * */

public class ServerHandler
{

	// TODO if we want to implement the server in distributed system , making it
	// serve the clients
	// from multiple places, then we shall change this into a private non-static
	// attribute, or even organize them with a Collection ,such as a array list
	private static ChatServer chat_server = null;
	private static JSONParser json_parser = null;

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





	/**
	 * This method takes msg as the input and response with a String in the
	 * format of a json
	 * 
	 * */
	public static void handleMessage(String msg, ClientWrap client)
	{
		JSONObject msg_json=ServerHandler.parserGiveString(msg);
		JSONObject response_json=new JSONObject();
		
		String msg_type=(String) msg_json.get("type");
		
		if(msg_type.equals(TYPE_INDENTITY_CHANGE))
		{//apply to change the identity from client
			
			
			//since a chat room has been bundled with a client wrap
			//if the identity of a client wrap is changed
			//the owner name will been taken care with automatically
			
			String msg_identity=(String) msg_json.get("identity");//request new identity

			response_json.put("type", TYPE_NEW_IDENTITY);
			String former_id=client.getIdentity();
			response_json.put("formor", former_id);
			
			
			//check if the new name has been used
			if(chat_server.checkIdentityOccupied(msg_identity))
			{
				response_json.put("identity",former_id);

				
				//only the client with the failed request will get response
				client.sendNextMessage(response_json.toJSONString());
				
			}
			else
			{//successful change
					
				response_json.put("identity",msg_identity);
				
				client.setIdentity(msg_identity);
				
				//check if it's a "guest name"
				if(Pattern.matches("^guest[1-9]\\d*$", former_id))
				{
					int index=Integer.parseInt(former_id.substring(5));//length of "guest" is 5
					ServerHandler.chat_server.releaseGuestIndex(index);
				}
				
				ServerHandler.chat_server.broadcastToAll(response_json.toJSONString());
				
				
				
			}
			
		}
			
	}
		
}
