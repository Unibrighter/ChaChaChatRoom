package rocklee.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.interfaces.RSAPublicKey;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import rocklee.security.DESUtil;
import rocklee.security.RSAUtil;
import rocklee.utility.Config;

import com.sun.org.apache.bcel.internal.generic.L2D;

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
		try
		{
			this.os = new PrintWriter(socket.getOutputStream());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		this.scanner = new Scanner(System.in);

	}

	public String getPrefix()
	{
		String prefix = "[" + this.chat_client.getRoomId() + "] "
				+ this.chat_client.getIdentity() + ">";
		return prefix;
	}

	public void run()
	{
		try
		{
			this.sleep(2000);

			String readline = scanner.nextLine();
			System.out.println();
			while (this.chat_client.isOnline() && readline != null)
			{
				System.out.println(this.getPrefix());

				this.handleInput(readline);

				readline = scanner.nextLine();
				System.out.println();
			}
			os.close();
			socket.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public String sendRSAAuthenticationRequest()
	{
		JSONObject rsa_verify_json = new JSONObject();

		// get current Time stamp as the random string
		String time_stamp = "" + System.currentTimeMillis();

		rsa_verify_json.put("type", Config.TYPE_RSA_VERIFY);
		rsa_verify_json.put("content", time_stamp);

		log.debug("Plain Text json is :\t"+rsa_verify_json.toJSONString());
		
		
		// encrypt the message
		String json_encrypt = RSAUtil.encryptUsingPublicKey(
				this.chat_client.getRSAPublicKey(),
				rsa_verify_json.toJSONString());

		log.debug("Cipher Text String is:\t"+json_encrypt);
		
		this.sendNextMessage(json_encrypt,false);

		return RSAUtil.stringMD5(time_stamp);

	}

	public void sendLoginRequest()
	{
		JSONObject login_json = new JSONObject();

		// set up the session key
		String session_key_root = DESUtil.randomString(64);
		this.chat_client.setSessionKey(session_key_root);

		login_json.put("type", Config.TYPE_LOGIN);
		login_json.put("identity", this.chat_client.getIdentity());
		login_json.put("password",
				RSAUtil.stringMD5(this.chat_client.getPassword()));
		login_json.put("sessionkey", session_key_root);

		log.debug("Plain Text json is :\t"+login_json.toJSONString());
		
		
		// encrypt the message
		String json_encrypt = RSAUtil.encryptUsingPublicKey(
				this.chat_client.getRSAPublicKey(),
				login_json.toJSONString());

		log.debug("Cipher Text String is:\t"+json_encrypt);
		

		this.sendNextMessage(json_encrypt,false);
	}

	private void handleInput(String raw_input)
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

		if (args[0].equals(Config.TYPE_INDENTITY_CHANGE))
		{
			command_json.put("type", Config.TYPE_INDENTITY_CHANGE);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			if (!this.validIdentity(args[1]))
			{
				System.out.println("Identity " + args[1]
						+ " is invalid or already in use.");
				return;
			}

			command_json.put("identity", args[1]);

			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_JOIN))
		{
			command_json.put("type", Config.TYPE_JOIN);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			command_json.put("roomid", args[1]);

			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_WHO))
		{
			command_json.put("type", Config.TYPE_WHO);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			command_json.put("roomid", args[1]);

			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_CREATE_ROOM))
		{
			command_json.put("type", Config.TYPE_CREATE_ROOM);

			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			if (!this.validRoomId(args[1]))
			{
				System.out.println("Room " + args[1]
						+ " is invalid or already in use.");
				return;
			}

			command_json.put("roomid", args[1]);
			this.chat_client.setRequestNewRoomId(args[1]);

			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_LIST))
		{
			command_json.put("type", Config.TYPE_LIST);
			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_CREATE_ROOM))
		{
			command_json.put("type", Config.TYPE_CREATE_ROOM);
			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_KICK))
		{
			if (args.length < 4)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			if (!this.validIdentity(args[1]))
			{
				System.out.println("Identity " + args[1] + " is invalid.");
				return;
			}

			if (!this.validRoomId(args[2]))
			{
				System.out.println("RoomId " + args[1] + " is invalid.");
				return;
			}

			command_json.put("type", Config.TYPE_KICK);
			command_json.put("identity", args[1]);
			command_json.put("roomid", args[2]);
			command_json.put("time", new Long(Long.parseLong(args[3])));

			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_DELETE))
		{
			if (args.length < 2)
			{
				log.debug("not enough parameters are given! plz Check input format!");
				return;
			}

			if (!this.validRoomId(args[1]))
			{
				System.out.println("Room " + args[1]
						+ " is invalid or already in use.");
				return;
			}

			command_json.put("roomid", args[1]);
			command_json.put("type", Config.TYPE_DELETE);
			this.sendNextMessage(command_json.toString(),true);
			return;
		}

		if (args[0].equals(Config.TYPE_QUIT))
		{
			command_json.put("type", Config.TYPE_QUIT);
			this.sendNextMessage(command_json.toString(),true);// it needs a
															// recognition from
			// server to disconnect
			return;
		}

		log.debug("No command match found!!! check the command input!");
	}

	private void handleMessage(String raw_message)
	{
		JSONObject message_json = new JSONObject();
		message_json.put("type", Config.TYPE_MESSAGE);
		message_json.put("content", raw_message);

		this.sendNextMessage(message_json.toString(),true);

	}

	public void sendNextMessage(String msg, boolean DES_encrypt)
	{
		if (DES_encrypt)
		{
			try
			{
				msg = DESUtil.encrypt(msg, this.chat_client.getSessionKey());
			} catch (Exception e)
			{

				e.printStackTrace();
			}
		}
			
		this.os.println(msg);
		this.os.flush();
	}

	private boolean validIdentity(String identity)
	{
		return Pattern.matches(Config.VALID_IDENTITY_REX, identity);
	}

	private boolean validRoomId(String room_id)
	{
		return Pattern.matches(Config.VALID_ROOM_ID_REX, room_id);
	}

}