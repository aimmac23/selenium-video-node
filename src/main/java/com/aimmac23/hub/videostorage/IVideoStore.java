package com.aimmac23.hub.videostorage;

import java.io.InputStream;
import java.util.Map;

import org.openqa.grid.internal.TestSession;

import com.aimmac23.hub.servlet.HubVideoInfoServlet;

/**
 * An interface to describe a plugin which handles how we store videos.
 * 
 * @author Alasdair Macmillan
 *
 */
public interface IVideoStore {
	
	/**
	 * Store a video using this plugin.
	 * 
	 * @param videoStream - an input stream for the video being streamed from the node.
	 * @param mimeType - a mimetype for the video stream.
	 * @param sessionId - the Selenium session ID which this video recorded.
	 * @param sessionInfo - A bean representing information about the session that just ran.
	 * @throws Exception if anything went wrong when trying to store the video.
	 */
	public void storeVideo(InputStream videoStream, String mimeType, String sessionId, 
			SessionInfoBean sessionInfo) throws Exception;
	
	/**
	 * Attempts to retrieve the video using this plugin. 
	 * 
	 * @param sessionId - The Selenium sessionId that the video recorded
	 * @return a {@link StoredVideoDownloadContext} representing the video, even if it was not found.
	 * @throws Exception if anything went wrong when trying to download the video.
	 */
	public StoredVideoDownloadContext retrieveVideo(String sessionId) throws Exception;
	
	/**
	 * Retrieves abstract information about the video, which could include where the video is stored.
	 * 
	 * @param sessionId - The Selenium sessionId that the video recorded
	 * @return a {@link StoredVideoInfoContext} representing the video, even if it was not found.
	 * @throws Exception if anything went wrong when trying to get information about the video.
	 */
	public StoredVideoInfoContext getVideoInformation(String sessionId) throws Exception;
	
	/**
	 * Returns some sort of machine-readable string to help identify the storage mechanism
	 * being used. This is used in the {@link HubVideoInfoServlet}
	 * @return
	 */
	public String getVideoStoreTypeIdentifier();
}
