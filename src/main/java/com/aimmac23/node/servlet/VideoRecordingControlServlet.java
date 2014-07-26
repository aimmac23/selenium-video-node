package com.aimmac23.node.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.aimmac23.node.RecordVideoCallable;
import com.aimmac23.node.VideoRecordController;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class VideoRecordingControlServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(VideoRecordingControlServlet.class.getName());

	private static final long serialVersionUID = 1L;
	private static VideoRecordController controller;
	Cache<String, File> availableVideos;

	// make sure we initialise the RecordVideoCallable class, because that initialises
	// the native code dependencies. If we can't load the native code, we should blow
	// up now
	static {
		try {
			Class.forName(RecordVideoCallable.class.getName());
		} catch (ClassNotFoundException e) {
			// not possible
		}
		
		// this class contains things which should be checked at startup, not when the servlet is 
		// initialised
		controller = new VideoRecordController();
	}
	public VideoRecordingControlServlet() {
		super();
		log.info("Constructor called");
		
		
		availableVideos = CacheBuilder.newBuilder().maximumSize(10).removalListener(new RemovalListener<String, File>() {
			@Override
			public void onRemoval(RemovalNotification<String, File> arg0) {
				if(arg0.getValue().delete()) {
					log.info("Deleted recording due to excess videos: " + arg0.getKey());
				}
			}
		}).build();
		
		// I suspect that simply mentioning these variables will throw a rather rude
		// throwable if the dependencies cannot be found - still, best check
			
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		service(req, resp);
	}
	
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String command = req.getParameter("command");
		if(command == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'command'");
			return;
		}
		
		if(command.equalsIgnoreCase("start")) {
			handleStartRecording(resp);
			return;
		}
		else if(command.equalsIgnoreCase("stop")) {
			handleDoStopRecording(resp);			
		}
		else if(command.equalsIgnoreCase("reset")) {
			handleReset(resp);
		}
		else if(command.equalsIgnoreCase("download")) {
			handleDownload(req, resp);
		}
		else {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Bad parameter: 'command', must be either 'start' or 'stop'");
			return;		
		}
	}
	
	
	private void handleStartRecording(HttpServletResponse resp) throws IOException {
		try {
			controller.startRecording();
		} catch (Exception e) {
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal server error while trying to start recording: " + e.getMessage());
			log.log(Level.SEVERE, "Caught exception while trying to start recording", e);
			return;
		}
		resp.setStatus(HttpStatus.SC_OK);
		resp.getWriter().write("Started Recording");
		
	}
	private void handleReset(HttpServletResponse resp) throws IOException {
		controller.resetRecording();
		
		resp.setStatus(HttpStatus.SC_OK);
		resp.getWriter().write("OK");
		
	}

	private void handleDoStopRecording(HttpServletResponse resp) throws IOException {
		try {
			File videoFile = controller.stopRecording();
			
			String fileKey = videoFile.getName();
			availableVideos.put(fileKey, videoFile);
			HashMap<String, Object> result = new HashMap<String, Object>();
			result.put("filepath", videoFile.getCanonicalPath());
			result.put("filekey", fileKey);
			resp.getWriter().write(new JSONObject(result).toString());
			return;
		} catch (Exception e) {
			resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().write("Internal Server Error: Caught Exception: " + e.getMessage());
			log.log(Level.SEVERE, "Caught exception while trying to stop recording", e);
			return;
		}
	}
	
	private void handleDownload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String filekey = req.getParameter("filekey");
		
		if(filekey == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'filekey'");
			return;
		}
		
		File video = availableVideos.getIfPresent(filekey);
		if(video == null) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("No video found for key: " + filekey);
			return;
		}
		
		if(!video.exists()) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("Video file deleted for key: " + filekey);
			return;
		}
		
		resp.setStatus(HttpStatus.SC_OK);
		resp.setContentType("video/webm");
		resp.setContentLength((int)video.length());
		FileInputStream videoStream = new FileInputStream(video);
		new StreamPumper(videoStream, resp.getOutputStream()).run();
		log.info("Retrieved video for key: " + filekey);
	}

}
