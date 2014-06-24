package com.aimmac23.hub.videostorage;

import java.util.Map;

import org.openqa.grid.internal.TestSession;

/**
 * A bean to wrap up information about the {@link TestSession}, to
 * prevent the object being modified, and to present a clean and extensible
 * API in {@link IVideoStore}
 * 
 * @author Alasdair Macmillan
 *
 */
public class SessionInfoBean {
	private Map<String, Object> requestedCapabilities;
	private Map<String, Object> nodeCapabilities;

	public SessionInfoBean(TestSession session) {
		requestedCapabilities = session.getRequestedCapabilities();
		nodeCapabilities = session.getSlot().getCapabilities();
	}
	
	public Map<String, Object> getRequestedCapabilities() {
		return requestedCapabilities;
	}
	
	public Map<String, Object> getNodeCapabilities() {
		return nodeCapabilities;
	}

}
