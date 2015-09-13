package rocklee.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

public class ChatServer
{
	
	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatServer.class);
	
	private ServerSocket serverSocket = null;
	private ChatRoomManager main_hall = null;
	private Vector<ChatRoomManager> room_list = null;
	
	//this number indicates that how many users are using names like "guest###"
	//TODO we need to implement a generator to generate a guest name according to the requirements
	private int tmp_guest_number=-1;
	
	
	public static final String MAIN_HALL_NAME="MainHall";

	private boolean listenning = true;
	private int port_num = 4444;

	public ChatServer(int port_num)
	{
		this.port_num = port_num;
		this.tmp_guest_number=0;
		
		try
		{
			serverSocket = new ServerSocket(port_num);

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		log.debug("Server has been initialized!!!!");
		
		//set up the main hall,owner is empty
		main_hall=new ChatRoomManager(MAIN_HALL_NAME, "");
		
		//set up the lists for chat room
		this.room_list=new Vector<ChatRoomManager>();		
		
		//add it to the chat room list
		this.room_list.add(main_hall);
	}


	
	public void startService()
	{
		while(listenning)
		{
            Socket socket=null;
			try
			{
				socket = serverSocket.accept();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
            
            ClientWrap new_client=new ClientWrap(socket, "guest"+(tmp_guest_number++));
            
            //add the new client to the main hall as default
            main_hall.addClient(new_client);
            
            //set the broadcasting channel for this client
            //TODO default here is main hall, going to modify it later into something according to the user's command
            new_client.switchChatRoom(main_hall);
            
            new_client.start();//start to serve the client
		}
	}
	
	public boolean addNewChatRoom()
	{
		
	}
	
	
	public static void main(String[] args)
	{
		ChatServer chatServer=new ChatServer(4444);
		chatServer.startService();
	}
	

}
