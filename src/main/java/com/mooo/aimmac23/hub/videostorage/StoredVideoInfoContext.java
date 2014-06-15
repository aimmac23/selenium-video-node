package com.mooo.aimmac23.hub.videostorage;

import java.util.Map;

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
	
	Map<String, Object> additionalInformation();
}
