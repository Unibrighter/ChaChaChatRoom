package rocklee.utility;

/**
 * We try to represent a unique User profile with this class, which can be
 * easily identified by the server as well as the client
 * 
 * @author Kunliang Wu
 * @version 2015-10-03 22:37
 * 
 * */
public class UserProfile
{
	private long user_num = -1;
	private String user_Identity = "";
	private String user_password_md5 = "";

	private String currentRoomId = "";

	private boolean online = false;

	public UserProfile(long user_num, String user_identity,
			String user_password, String currentRoomId)
	{
		this.user_Identity = user_identity;
		this.user_password_md5 = user_password;
		this.user_num = user_num;
		this.currentRoomId=currentRoomId;
	}

	public boolean isOnline()
	{
		return this.online;

	}

	public void setOnline(boolean online)
	{
		this.online = online;
	}

	public void setUserNum(Long user_num)
	{
		this.user_num = user_num;
	}

	public long getUserNum()
	{
		return this.user_num;
	}

	public void setUserIdentity(String new_identity)
	{
		this.user_Identity = new_identity;
	}

	public String getUserIdentity()
	{
		return this.user_Identity;
	}

	public void setPassword(String password_md5)
	{
		this.user_password_md5 = password_md5;
	}

	public boolean checkPassword(String password_md5)
	{
		return this.user_password_md5.equalsIgnoreCase(password_md5);
	}
	
	public void setCurrentRoomId(String current_roomId)
	{
		this.currentRoomId = current_roomId;
	}

	public String getCurrentRoomId()
	{
		return this.currentRoomId;
	}


}
