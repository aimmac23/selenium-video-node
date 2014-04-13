package com.mooo.aimmac23.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpStatus;
import org.openqa.grid.internal.ExternalSessionKey;

import com.mooo.aimmac23.hub.HubVideoRegistry;

public class HubVideoDownloadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String sessionId = req.getParameter("sessionId");
		
		if(sessionId == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'sessionId'");
			return;
		}
		
		File video = HubVideoRegistry.getVideoForSession(new ExternalSessionKey(sessionId));
		
		if(video == null || !video.exists()) {
			resp.setStatus(HttpStatus.SC_NO_CONTENT);
			resp.getWriter().write("Video content not found for sessionId: " + sessionId);
			return;
		}
		
		FileInputStream fileStream = new FileInputStream(video);
		try {
			resp.setContentType("video/webm");
			resp.setContentLength((int)video.length());
			new StreamPumper(fileStream, resp.getOutputStream()).run();
			return;
		}
		finally {
			fileStream.close();
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
		
		File video = HubVideoRegistry.getVideoForSession(new ExternalSessionKey(sessionId));
		
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
