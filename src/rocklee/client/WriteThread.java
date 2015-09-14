package rocklee.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

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
		
		String args[]=raw_command.split(" ");
		
		JSONObject command_json=new JSONObject();
		
		if(args[0].equals(ChatClient.TYPE_INDENTITY_CHANGE))
		{
			command_json.put("type", ChatClient.TYPE_INDENTITY_CHANGE);
			
			if(args.length<2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}
			
			command_json.put("identity", args[1]);
			
			this.sendNextJson(command_json);
			
		}
		
		if(args[0].equals(ChatClient.TYPE_JOIN))
		{
			command_json.put("type", ChatClient.TYPE_JOIN);
			
			if(args.length<2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}
			
			command_json.put("roomid", args[1]);
			
			this.sendNextJson(command_json);
		}
		
		if(args[0].equals(ChatClient.TYPE_WHO))
		{
			command_json.put("type", ChatClient.TYPE_WHO);
			
			if(args.length<2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
			}
			
			command_json.put("roomid", args[1]);
			
			this.sendNextJson(command_json);
		}
		
		if(args[0].equals(ChatClient.TYPE_QUIT))
		{
			command_json.put("type", ChatClient.TYPE_QUIT);
			this.sendNextJson(command_json);//it needs a recognition from server to disconnect
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

}