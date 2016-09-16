package com.example.fasttrans;
import java.util.ArrayList;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

import com.example.fasttrans.ConnectionListener;
import com.example.fasttrans.AlljoynManager.ConnectionState;
import org.alljoyn.cops.filetransfer.utility.Logger;
public class AlljoynManager {
	/* Load the native alljoyn_java library. */
	static 
	{
	    System.loadLibrary("alljoyn_java");
	}
	
	/* Connection State Enum */
	public enum ConnectionState { CONNECTED, HOSTING, JOINING, DISCONNECTED };
	
	/* File Transfer Session Advertised Name */
	//设置会话广播名字
	private static final String SERVICE_NAME = "com.example.fasttrans";	
	/* AllJoyn Communication Port */
	//设置会话端口
	private static final short CONTACT_PORT=42;
	
	/* Private Variables */
	//注册总线附件 会话id 连接状态 连接监听 端数组
	private BusAttachment bus;
	private int sessionId;
	private ConnectionState connectionState;
	private ConnectionListener connectionListener;
	private ArrayList<String> peers;
	
	/*
	 * The AllJoyn Manager facilitates establishing and maintaining an AllJoyn Connection	   
	 */
	public AlljoynManager()
	{
		bus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);
		peers = new ArrayList<String>();
	}
	
	/*
	 * Sets the connection listener, allowing the app to be notified when a
	 * connection is established
	 */
	public void setConnectionListener(ConnectionListener listener)
	{
		connectionListener = listener;
	}
	
	/*
	 * Starts hosting a session, allowing another device to connect 
	 */
	public void createSession()
	{		
		if (connectionState == ConnectionState.JOINING)
		{
			bus.disconnect();
			bus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);
		}
		connectionState = ConnectionState.HOSTING;
		
		bus.connect();
		
		Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
		
		SessionOpts sessionOpts = new SessionOpts();
		sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
		sessionOpts.isMultipoint = true;
		sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
		sessionOpts.transports = SessionOpts.TRANSPORT_ANY;
		
		bus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() 
		{
		    @Override
		    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts)
		    {		    	
		    	return true;
		    }
		    
		    @Override
			public void sessionJoined(short sessionPort, int id, String joiner) 
		    {		    	
		    	sessionId = id;
		    	
		    	peers.add(joiner);
		    	
		    	connectionState = ConnectionState.CONNECTED;
		    	if (connectionListener != null)
		    	{
		    		connectionListener.ConnectionChanged(connectionState);
		    	}
		    }
		});
		
		int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
		bus.requestName(SERVICE_NAME, flag);
		bus.advertiseName(SERVICE_NAME, SessionOpts.TRANSPORT_ANY);		
	}
	
	/*
	 * Joins a session another device is hosting.
	 */
	public void joinSession()
	{		
		if (connectionState == ConnectionState.HOSTING)
		{			
			bus.disconnect();
			bus = new BusAttachment("FileTransfer", BusAttachment.RemoteMessage.Receive);
		}
		connectionState = ConnectionState.JOINING;
		
		bus.registerBusListener(new BusListener() 
		{
			@Override
			public void foundAdvertisedName(final String name, short transport, String namePrefix) 
			{
				//a session cannot be joined on the same thread as the found advertised named listener
				new Thread()
				{
					public void run()
					{
						doJoinSession(name);         			           			
					}
				}.start();       	           	
			}
		});

		bus.connect();
		bus.findAdvertisedName(SERVICE_NAME);
	}
	
	/*
	 * Called when the session host has been found and finalizes joining 
	 * the session
	 */
	private void doJoinSession(final String name)
	{
		SessionOpts sessionOpts = new SessionOpts();
		Mutable.IntegerValue mutableSessionId = new Mutable.IntegerValue();

		Status status = bus.joinSession(name, CONTACT_PORT, mutableSessionId, sessionOpts, new SessionListener()
		{
			@Override
			public void sessionMemberAdded(int sessionId, String busId)
			{
				peers.add(busId);
			}
		});     	  
		Logger.log("join returned: " + status.toString());

		sessionId = mutableSessionId.value;
		Logger.log("sessionId = " + sessionId);
		
		connectionState = ConnectionState.CONNECTED;
		if (connectionListener != null)
    	{
    		connectionListener.ConnectionChanged(connectionState);
    	}
	}
	
	/*
	 * Gets the BusAttachment. Required for the File Transfer Module
	 */
	public BusAttachment getBusAttachment()
	{
		return bus;
	}
	
	/*
	 * Gets the Session Id. Required for the File Transfer Module
	 */
	public int getSessionId()
	{
		return sessionId;
	}

	/*
	 * Tears down the AllJoyn connection
	 */	
	public void disconnect()
	{
		if (connectionState == ConnectionState.CONNECTED)
		{
			bus.leaveSession(sessionId);
			bus.disconnect();
			connectionState = ConnectionState.DISCONNECTED;
		}
	}
	
	/*
	 * Returns the list of known peers/devices
	 */
	public String[] getPeers()
	{
		String[] strArray = new String[peers.size()];
		
		return peers.toArray(strArray);
	}
}
