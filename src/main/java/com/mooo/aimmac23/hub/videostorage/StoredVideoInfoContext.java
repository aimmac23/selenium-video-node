package com.mooo.aimmac23.hub.videostorage;

import java.util.Map;

/**
 * An object representing information about a video that a {@link IVideoStore} has
 * stored.
 * 
 * @author Alasdair Macmillan
 *
 */
public interface StoredVideoInfoContext {

	/**
	 * Was the requested video found?
	 * 
	 * @return
	 */
	boolean isVideoFound();
	
	/**
	 * 
	 * @return the content length in bytes of the video, or null if we are using a storage
	 * mechanism that doesn't tell us this.
	 */
	Long getContentLengthIfKnown();
	
	/**
	 * Frees any resources allocated to this storage request. Should always be called, even
	 * if the video is not found.
	 * 
	 */
	void close();
	
	/**
	 * Gets additional information about the video. This is an opportunity for the plugin to
	 * return any information it likes to the caller.
	 * 
	 * This feature is currently intended to provide implementation-dependent information about where
	 * a video is stored. This can be used for integration with other systems, so they can serve up 
	 * videos independent of the Selenium Hub.
	 *   
	 * @return a map of key-value pairs to be used in the JSON response
	 */
	Map<String, Object> additionalInformation();
}
