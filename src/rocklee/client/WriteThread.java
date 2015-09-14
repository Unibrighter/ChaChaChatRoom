package rocklee.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * This thread deals with the user's command or message
 * 
 * 
 * */

public class WriteThread extends Thread
{
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(WriteThread.class);

	private Socket socket = null;
	private ChatClient chat_client = null;

	private PrintWriter os = null;
	private Scanner scanner = null;

	public WriteThread(Socket socket, ChatClient client)
	{
		this.socket = socket;
		this.chat_client = client;

	}

	public String getPrefix()
	{
		String prefix="["+this.chat_client.getRoomId()+"] "+this.chat_client.getIdentity()+">";
		return prefix;
	}

	
	public void run()
	{
		try
		{
			this.os = new PrintWriter(socket.getOutputStream());
			this.scanner = new Scanner(System.in);
			String readline = scanner.nextLine();
			while (this.chat_client.isOnline() && readline != null)
			{
				this.handInput(readline);
				
				//TODO how to make sure that this prefix always show itself at the buttom line before input?
				System.out.print(this.getPrefix());
				
				readline = scanner.nextLine();
			}
			os.close();
			socket.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void handInput(String raw_input)
	{
		if (raw_input.length() < 1)
		{
			log.debug("not a valid input line");
			return;
		}

		if (raw_input.length() >= 2 && raw_input.charAt(0) == '#')
		{
			this.handleCommand(raw_input.substring(1));
		}

		else
		{
			this.handleMessage(raw_input);
		}
	}

	private void handleCommand(String raw_command)
	{// join, identitychange, who and quit

		String args[] = raw_command.split(" ");

		JSONObject command_json = new JSONObject();

		if (args[0].equals(ChatClient.TYPE_INDENTITY_CHANGE))
		{
			command_json.put("type", ChatClient.TYPE_INDENTITY_CHANGE);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}

			if (!this.validIdentity(args[1]))
			{
				System.out.println("Identity " + args[1]
						+ " is invalid or already in use.");
				return;
			}

			command_json.put("identity", args[1]);

			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_JOIN))
		{
			command_json.put("type", ChatClient.TYPE_JOIN);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}

			command_json.put("roomid", args[1]);

			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_WHO))
		{
			command_json.put("type", ChatClient.TYPE_WHO);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}

			command_json.put("roomid", args[1]);

			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_CREATE_ROOM))
		{
			command_json.put("type", ChatClient.TYPE_CREATE_ROOM);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}

			if (!this.validRoomId(args[1]))
			{
				System.out.println("Room " + args[1]
						+ " is invalid or already in use.");
				return;
			}

			command_json.put("roomid", args[1]);
			this.chat_client.setRequestNewRoomId(args[1]);

			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_LIST))
		{
			command_json.put("type", ChatClient.TYPE_LIST);
			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_CREATE_ROOM))
		{
			command_json.put("type", ChatClient.TYPE_CREATE_ROOM);
			this.sendNextJson(command_json);
			return;
		}

		if (args[0].equals(ChatClient.TYPE_QUIT))
		{
			command_json.put("type", ChatClient.TYPE_QUIT);
			this.sendNextJson(command_json);// it needs a recognition from
											// server to disconnect
			return;
		}

		log.debug("No command match found!!! check the command input!");
	}

	private void handleMessage(String raw_message)
	{
		JSONObject message_json = new JSONObject();
		message_json.put("type", ChatClient.TYPE_MESSAGE);
		message_json.put("content", raw_message);

		this.sendNextJson(message_json);

	}

	private void sendNextJson(JSONObject json_obj)
	{
		this.os.println(json_obj.toJSONString());
		this.os.flush();
	}

	private boolean validIdentity(String identity)
	{
		return Pattern.matches(ChatClient.VALID_IDENTITY_REX, identity);
	}

	private boolean validRoomId(String room_id)
	{
		return Pattern.matches(ChatClient.VALID_ROOM_ID_REX, room_id);
	}

}