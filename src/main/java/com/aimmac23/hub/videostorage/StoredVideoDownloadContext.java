package com.aimmac23.hub.videostorage;

import java.io.IOException;
import java.io.InputStream;

/**
 * An object providing a means by which we can download a video from a plugin
 * in an implementation-independent way.
 * 
 * @author Alasdair Macmillan
 *
 */
public interface StoredVideoDownloadContext {
	
	/**
	 * Was the requested video found?
	 * 
	 * @return
	 */
	boolean isVideoFound();
	
	/**
	 * If the video was found, return an Input stream for the content, or null otherwise.
	 * 
	 * @return
	 */
	InputStream getStream() throws IOException;
	
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
	
}
