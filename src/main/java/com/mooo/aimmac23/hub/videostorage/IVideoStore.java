package com.mooo.aimmac23.hub.videostorage;

import java.io.InputStream;

public interface IVideoStore {
	
	public void storeVideo(InputStream videoStream, String mimeType, String sessionId) throws Exception;
	
	public StoredVideoDownloadContext retrieveVideo(String sessionId) throws Exception;
	
	public StoredVideoInfoContext getVideoInformation(String sessionId) throws Exception;
}
