package rocklee.server;

import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * This class uses a vector to manage a chatroom and the users within this room.
 * Basically it supports the broadcast and some group functions regarding to the
 * people in this room
 * 
 * Also, some of the methods have been defined as synchronized methods,which
 * means they can't be used by two or more threads simultaneously ,like
 * broadcasting message to all the clients thread.
 * 
 * @author Kunlaing WU
 * 
 * @version 2015-09-13 13:58
 * */
public class ChatRoomManager
{

	// for debug and info, since log4j is thread safe, it can also be used to
	// record the result and output
	private static Logger log = Logger.getLogger(ChatRoomManager.class);

	private String room_id = null;
	private String room_owner = null;

	// the collection of the clients in this chat room
	private Vector<ClientWrap> client_vector = null;

	public ChatRoomManager(String id, String owner)
	{

		this.room_id = id;
		this.room_owner = owner;
		client_vector = new Vector<ClientWrap>();
		log.debug("New Chat Room Established!! roomid: " + id + "\townerid:"
				+ owner);
	}

	// this method needs to be set as synchronized so that the case two thread
	// use this method simultaneously won't happen
	public synchronized void broadCastMessage(String msg)
	{
		// for each print writer in each client wrap, print this message
		for (int i = 0; i < client_vector.size(); i++)
		{
			client_vector.get(i).sendNextMessage(msg);
		}
	}

	public synchronized boolean addClient(ClientWrap client)
	{
		if (client == null)
			return false;

		return this.client_vector.add(client);
	}

	/**
	 * if the client is successfully deleted from the list then return it as an
	 * instance if the target is not found, then return null
	 */
	public synchronized boolean removeClient(ClientWrap client)
	{
		return this.client_vector.remove(client);

	}

	// tell whether this room has a user with a given identity or not
	public boolean contains(String client_identity)
	{
		ClientWrap dummy = new ClientWrap(null, client_identity);
		return this.client_vector.contains(dummy);
	}

	public String getRoomId()
	{
		return this.room_id;
	}

	public String getRoomOwner()
	{
		return this.room_owner;
	}
}
