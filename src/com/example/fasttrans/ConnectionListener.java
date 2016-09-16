package com.example.fasttrans;

import com.example.fasttrans.AlljoynManager.ConnectionState;

public interface ConnectionListener
{
	/*
	 * Triggered by the AllJoyn Manager when the connection state changes
	 */
	public void ConnectionChanged(ConnectionState connectionState);
}