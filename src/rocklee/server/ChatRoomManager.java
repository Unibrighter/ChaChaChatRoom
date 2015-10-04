package rocklee.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import rocklee.utility.UserProfile;
import sun.rmi.server.UnicastServerRef;

import com.sun.security.ntlm.Client;

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
	private UserProfile room_owner = null;

	// the collection of the clients in this chat room
	private Vector<ClientWrap> client_list = null;

	private Map<Long, Long> black_list = new HashMap<Long, Long>();
	
	public ChatRoomManager(String id, UserProfile owner)
	{
		this.room_id = id;
		this.room_owner = owner;
		client_list = new Vector<ClientWrap>();
		log.debug("New Chat Room Established!! roomid: " + id + "\townerid:"
				+ (owner!=null?owner.getUserIdentity():""));
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


	public String getRoomId()
	{
		return this.room_id;
	}

	// if there is no client who takes charge of this room ,then return ""
	// indicating that there is no owner
	public UserProfile getRoomOwner()
	{
		return this.room_owner;
	}

	public void setRoomOwner(UserProfile owner)
	{
		this.room_owner=owner;
	}
		
	// return the collection of room member ,including the room owner
	public Vector<String> getRoomMembers()
	{
		Vector<String> result=new Vector<String>();
		
		for (int i = 0; i < this.client_list.size(); i++)
		{
			result.add(this.client_list.get(i).getUserProfile().getUserIdentity());
		}
		return result.isEmpty()?null:result;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject getJsonObject()
	{
		JSONObject json_obj=new JSONObject();
		json_obj.put("roomid", this.room_id);
		json_obj.put("count", this.getGuestNum());
		return json_obj;
	}
	
	
	public int getGuestNum()
	{
		return this.client_list.size();
	}
	
	public boolean onBlackList(String name)
	{
		return black_list.containsKey(name);
	}
	
	public void banUserByNum(Long user_num,long deadline)
	{
		black_list.put(user_num, deadline);
	}
	
	public boolean hasBannedUserNum(Long user_num)
	{
		if(this.black_list.containsKey(user_num))
		{
			return black_list.get(user_num)<=System.currentTimeMillis();
		}
		else return false;
	}
	
	public Vector<ClientWrap> getAllClients()
	{
		return this.client_list;
	}
	
	public ClientWrap getClientWrapByUserProfile(UserProfile user)
	{
		long user_num=user.getUserNum();
		for (int i = 0; i < client_list.size(); i++)
		{
			ClientWrap tmpUser=client_list.get(i);
			if(tmpUser.getUserProfile().getUserNum()==user_num)
				return tmpUser;
		}
		return null;
	}

}
