package com.mooo.aimmac23.hub.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpStatus;
import org.openqa.grid.internal.ExternalSessionKey;

import com.mooo.aimmac23.hub.HubVideoRegistry;

public class HubVideoDownloadServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(HubVideoDownloadServlet.class.getName());

	private static final long serialVersionUID = 1L;
	
	static {
		try {
			// force this class to be initialized, so any errors are thrown at startup instead of first use
			Class.forName(HubVideoRegistry.class.getCanonicalName());
		} catch (ClassNotFoundException e) {
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
		
		InputStream stream;
		try {
			stream = HubVideoRegistry.getVideoForSession(new ExternalSessionKey(sessionId));
		} catch (Exception e) {
			log.log(Level.WARNING, "Caught exception when fetching video for " + sessionId, e);
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal error when fetching video");
			return;
		}
		
		if(stream == null) {
			resp.setStatus(HttpStatus.SC_NO_CONTENT);
			resp.getWriter().write("Video content not found for sessionId: " + sessionId);
			return;
		}
		
		try {
			resp.setContentType("video/webm");
			// resp.setContentLength((int)video.length());
			new StreamPumper(stream, resp.getOutputStream()).run();
			return;
		}
		finally {
			stream.close();
		}
	}
	
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sessionId = req.getParameter("sessionId");
		
		if(sessionId == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
		File video = null; //HubVideoRegistry.getVideoForSession(new ExternalSessionKey(sessionId));
		
		if(video == null || !video.exists()) {
			resp.setStatus(HttpStatus.SC_NO_CONTENT);
			return;
		}
		
		resp.setStatus(HttpStatus.SC_OK);
		resp.setContentType("video/mp4");
		resp.setContentLength((int)video.length());
		return;
	}
}
