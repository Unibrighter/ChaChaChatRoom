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
	private ClientWrap room_owner = null;

	// the collection of the clients in this chat room
	private Vector<ClientWrap> client_list = null;

	public ChatRoomManager(String id, ClientWrap owner)
	{

		this.room_id = id;
		this.room_owner = owner;
		client_list = new Vector<ClientWrap>();
		log.debug("New Chat Room Established!! roomid: " + id + "\townerid:"
				+ (owner!=null?owner.getIdentity():""));
	}

	// this method needs to be set as synchronized so that the case two thread
	// use this method simultaneously won't happen
	public synchronized void broadCastMessage(String msg)
	{
		// for each print writer in each client wrap, print this message
		for (int i = 0; i < client_list.size(); i++)
		{
			client_list.get(i).sendNextMessage(msg);
		}
	}

	public synchronized boolean addClient(ClientWrap client)
	{
		if (client == null)
			return false;

		return this.client_list.add(client);
	}

	/**
	 * if the client is successfully deleted from the list then return it as an
	 * instance if the target is not found, then return null
	 */
	public synchronized boolean removeClient(ClientWrap client)
	{
		
		return this.client_list.remove(client);

	}

	// tell whether this room has a user with a given identity or not
	public boolean contains(String client_identity)
	{
		ClientWrap dummy = new ClientWrap(null, client_identity);
		return this.client_list.contains(dummy);
	}

	public String getRoomId()
	{
		return this.room_id;
	}

	// if there is no client who takes charge of this room ,then return ""
	// indicating that there is no owner
	public String getRoomOwner()
	{
		return this.room_owner == null ? "" : this.room_owner.getIdentity();
	}

	public void setRoomOwner(ClientWrap client)
	{
		this.room_owner=client;
	}
	
	
	// return the collection of room member ,including the room owner
	public Vector<String> getRoomMembers()
	{
		Vector<String> result=new Vector<String>();
		
		for (int i = 0; i < this.client_list.size(); i++)
		{
			result.add(this.client_list.get(i).getIdentity());
		}
		return result.isEmpty()?null:result;
	}
	
	public int getGuestNum()
	{
		return this.client_list.size();
	}
	
}
