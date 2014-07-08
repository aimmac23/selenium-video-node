package com.aimmac23.hub.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.grid.internal.ExternalSessionKey;

import com.aimmac23.hub.HubVideoRegistry;
import com.aimmac23.hub.videostorage.StoredVideoInfoContext;

/**
 * A servlet to fetch information about videos for a given sessionId.
 * 
 * This servlet exists because some storage schemes can provide a location for a video
 * without the Hub being involved in the download/presentation.
 * 
 * Note that videos are not available until you have closed the Selenium Session
 * (calling driver.quit(), for example).
 * 
 * @author Alasdair Macmillan
 *
 */
public class HubVideoInfoServlet extends AbstractHubVideoServlet {
	
	private static final Logger log = Logger.getLogger(HubVideoInfoServlet.class.getName());

	private static final long serialVersionUID = 1L;
	
	static {
		try {
			// force this class to be initialized, so any errors are thrown at startup instead of first use
			Class.forName(HubVideoRegistry.class.getCanonicalName());
		} 
		catch (ClassNotFoundException e) {
			// Can't happen
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sessionId = req.getParameter("sessionId");
		
		if(sessionId == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'sessionId'");
			return;
		}
		
		if(!checkValidSessionId(sessionId, resp)) {
			// response writing already handled
			return;
		}
		
		StoredVideoInfoContext videoInfoForSession;
		try {
			videoInfoForSession = HubVideoRegistry.getVideoInfoForSession(new ExternalSessionKey(sessionId));
		} catch (Exception e) {
			log.log(Level.WARNING, "Caught exception when fetching video information for " + sessionId, e);
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal error when fetching video information");
			return;
		}
		
		if(!videoInfoForSession.isVideoFound()) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("Video not found for session: " + sessionId);
			videoInfoForSession.close();
			return;
		}
		
		HashMap<String, Object> responseMap = new HashMap<String, Object>();
		if(videoInfoForSession.getContentLengthIfKnown() != null) {
			responseMap.put("fileSize", videoInfoForSession.getContentLengthIfKnown());	
		}
		
		
		responseMap.put("storageType", HubVideoRegistry.getVideoStoreType());
		responseMap.put("additional", videoInfoForSession.additionalInformation());
		
		// we don't need this anymore
		videoInfoForSession.close();
		
		String json;
		try {
			json = new JSONObject(responseMap).toString(4);
		} catch (JSONException e) {
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			log.log(Level.WARNING, "Threw exception when writing JSON", e);
			return;
		}
		resp.setStatus(HttpStatus.SC_OK);
		resp.getWriter().write(json);
	}
}
