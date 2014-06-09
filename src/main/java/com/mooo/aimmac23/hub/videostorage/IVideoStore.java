package com.mooo.aimmac23.hub.videostorage;

import java.io.InputStream;

public interface IVideoStore {
	
	public void storeVideo(InputStream videoStream, String mimeType, String sessionId) throws Exception;
	
	public InputStream retrieveVideo(String sessionId) throws Exception;
}
