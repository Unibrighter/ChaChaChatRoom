package rocklee.client;

import java.net.Socket;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import org.apache.log4j.Logger;

import rocklee.security.RSAUtil;
import sun.net.util.IPAddressUtil;

public class ChatClient
{
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatClient.class);

	private String DES_sessionKeyRoot = null;
	private RSAPublicKey rsa_public_key=null;
	private String password = null;

	private boolean verify_success = false;
	private boolean login_success = false;

	// this records the temporary request new room id to check whether the room
	// creation succeeds or not
	private String request_new_room_id = null;

	private boolean online = false;

	private String client_identity = null;
	private String room_id = null;

	private Socket socket = null;

	public ReadThread read = null;
	public WriteThread write = null;

	public ChatClient(String ipAddress, int port_num, String identity,
			String org_password)
	{
		try
		{
			this.socket = new Socket(ipAddress, port_num);
			this.read = new ReadThread(socket, this);
			this.write = new WriteThread(socket, this);

			this.client_identity = identity;
			this.password = org_password;

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public RSAPublicKey getRSAPublicKey()
	{
		// get RSA public Key from file
		if(this.rsa_public_key==null)
				rsa_public_key = RSAUtil.getPublicKeyFromFile("rsa.pub");
		return this.rsa_public_key;
	}
	
	public String getPassword()
	{
		return this.password;
	}

	public boolean initialAuthentication()
	{
		this.rsa_public_key = RSAUtil.getPublicKeyFromFile("rsa.pub");
		
		String hash1 = this.write.sendRSAAuthenticationRequest();

		String hash2 = this.read.getMD5HashSignature();

		if (hash1.equalsIgnoreCase(hash2))
		{
			return true;
		} else
			return false;
	}

	public boolean login()
	{
		this.write.sendLoginRequest();
		boolean login_success = this.read.getLoginResult();

		if (login_success)
		{
			this.online = true;
			return true;
		} else
		{
			this.online = false;
			return false;
		}
		
		//secure session key has been settled, now we can use it to 
		//en/decrypt communication

	}

	public void startChat()
	{
		this.read.start();
		this.write.start();
		try
		{
			this.write.join();
			this.read.join();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void setOnline(boolean online)
	{
		this.online = online;
	}

	public boolean isOnline()
	{
		return this.online;
	}

	public String getIdentity()
	{
		return this.client_identity;
	}

	public void setIdentity(String identity)
	{
		this.client_identity = identity;
	}

	public String getRoomId()
	{
		return this.room_id;
	}

	public void setRoomId(String room_id)
	{
		this.room_id = room_id;
	}

	public void setRequestNewRoomId(String room_id)
	{
		this.request_new_room_id = room_id;
	}

	public String getRequestNewRoomId()
	{
		return this.request_new_room_id;
	}
	
	public String getSessionKey()
	{
		return this.DES_sessionKeyRoot;
	}

	public void setSessionKey(String DES_sessionKey)
	{
		this.DES_sessionKeyRoot = DES_sessionKey;
	}

	public String getPrefix()
	{
		String prefix = "[" + this.getRoomId() + "] "
				+ this.getIdentity() + ">";
		return prefix;
	}

	public static void main(String[] args)
	{

		int port = 4444;
		
		//if id is not given by args, then we consider it as an empty string""
		String id = "";
		
		String org_password = "";

		// --------------port arg------------------
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("-p"))
			{
				port = Integer.parseInt(args[i + 1]);
				break;
			}

		}

		// --------------id arg--------------------
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("-id"))
			{
				id = args[i + 1];
				break;
			}

		}

		// --------------password arg---------------
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("-password"))
			{
				org_password = args[i + 1];
				break;
			}

		}

		if (org_password.equalsIgnoreCase(""))
		{// this indicates that no password is given
			System.err.println("You have to give the password bro!");
			System.exit(-1);
		}


		ChatClient chat_client = new ChatClient(args[0], port, id, org_password);

		if (!chat_client.initialAuthentication())
		{
			System.err.println("Can't verify the server!");
			System.exit(-2);
		}

		if (!chat_client.login())
		{
			System.err.println("Login attempt failed!");
			System.exit(-3);
		} else
		{
			log.warn("login in successfully!");
			chat_client.startChat();
		}

	}
}