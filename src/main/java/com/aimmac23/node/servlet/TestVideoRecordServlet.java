package com.aimmac23.node.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.exec.StreamPumper;
import org.apache.http.HttpStatus;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.server.ActiveSession;
import org.openqa.selenium.remote.server.ActiveSessionListener;
import org.openqa.selenium.remote.server.ActiveSessions;
import org.openqa.selenium.remote.server.WebDriverServlet;

import com.aimmac23.node.DriverScreenshotSource;
import com.aimmac23.node.ScreenshotSource;
import com.aimmac23.node.VideoRecordController;
import com.aimmac23.node.args.SystemPropertyRecordArgs;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class TestVideoRecordServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(TestVideoRecordServlet.class.getName());

	private static final long serialVersionUID = 1L;
		
	Map<SessionId, VideoRecordController> activeRecordings = new ConcurrentHashMap<>();

	Cache<SessionId, File> availableVideos;

	// use the getter, to make sure the field has been set
	private ActiveSessions activeSessions;

	static {
		// make sure we can load the JNI libraries
		JnaLibraryLoader.init();
	}
	@Override
	public void init() throws ServletException {
		super.init();
		
		availableVideos = CacheBuilder.newBuilder().maximumSize(5).removalListener(new RemovalListener<SessionId, File>() {
			@Override
			public void onRemoval(RemovalNotification<SessionId, File> arg0) {
				if(arg0.getValue().delete()) {
					if(arg0.wasEvicted()) {
						log.info("Deleted recording due to excess videos: " + arg0.getKey());	
					}
				}
			}
		}).build();
	}
	
	private synchronized ActiveSessions getActiveSessions() {
		if(this.activeSessions == null) {
			ActiveSessions activeSessions = (ActiveSessions) getServletContext().getAttribute(WebDriverServlet.ACTIVE_SESSIONS_KEY);

			if(activeSessions != null) {
				processActiveSessionsObject(activeSessions);
			}
			else {
				new IllegalStateException("ActiveSessions object not found in ServletContext attributes! Has the WebDriverServlet been intialized?");
			}
		}
		return this.activeSessions;
	}
	
	private void processActiveSessionsObject(ActiveSessions activeSessions) {
		this.activeSessions = activeSessions;
		activeSessions.addListener(new SessionListener());
		
		log.info("Started up " + this.getClass());
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// FIXME: Mostly a copy-pasta from previous implementation - improve this!
		String command = req.getParameter("command");
		Optional<SessionId> sessionId = Optional.ofNullable(req.getParameter("sessionId")).map(SessionId::new);
		if(command == null) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'command'");
			return;
		}
		if(!sessionId.isPresent()) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Missing parameter: 'sessionId'");
			return;
		}
		
		
		if(command.equalsIgnoreCase("start")) {
			startRecording(sessionId.get());
			return;
		}
		else if(command.equalsIgnoreCase("stop")) {
			stopRecording(sessionId.get());
		}
		else if(command.equalsIgnoreCase("download")) {
			handleDownload(sessionId.get(), resp);
		}
		else {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Bad parameter: 'command', must be either 'start', 'stop', or 'download'");
			return;		
		}
	}
	
	public void startRecording(SessionId sessionId) {
		
		ActiveSession activeSession = this.getActiveSessions().get(sessionId);
		if(activeSession == null) {
			throw new IllegalStateException("Cannot start recording - session not found: " + sessionId);
		}
		
		log.info("Starting recording for session: " + sessionId);
		

		// TODO: Clean this API up a bit
		VideoRecordController controller = new VideoRecordController(new SystemPropertyRecordArgs() {
			@Override
			public ScreenshotSource getNewScreenshotSource() {
				return new DriverScreenshotSource(activeSession.getWrappedDriver());
			}
		}, 
				JnaLibraryLoader.getLibVPX(), JnaLibraryLoader.getEncoder());
		
		try {
			controller.startRecording();
			activeRecordings.put(sessionId, controller);
				
		} catch(Exception e) {
			// TODO: Improve error handling
			log.log(Level.SEVERE, "Caught exception while starting recording", e);
			Throwables.propagate(e);
		}
		
		
	}
	
	public void stopRecording(SessionId sessionId) {
		VideoRecordController recordController = activeRecordings.get(sessionId);
		if(recordController == null) {
			log.log(Level.WARNING, "Can't stop recording - not recording for sessionId: " + sessionId);
			return;
		}
		
		try {
			File videoResult = recordController.stopRecording();
			recordController.close();
			activeRecordings.remove(sessionId);
			availableVideos.put(sessionId, videoResult);
		} catch(Exception e) {
			// TODO: Improve error handling
			log.log(Level.SEVERE, "Caught exception while stopping recording: ", e.toString());
			Throwables.propagate(e);
		}
		
	}
	
	private void handleDownload(SessionId sessionId, HttpServletResponse resp) throws IOException {
		
		
		
		File video = availableVideos.getIfPresent(sessionId);
		if(video == null) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("No video found for sessionId: " + sessionId);
			return;
		}
		
		if(!video.exists()) {
			resp.setStatus(HttpStatus.SC_NOT_FOUND);
			resp.getWriter().write("Video file deleted for sessionId: " + sessionId);
			return;
		}
		
		resp.setStatus(HttpStatus.SC_OK);
		resp.setContentType("video/webm");
		resp.setContentLength((int)video.length());
		FileInputStream videoStream = new FileInputStream(video);
		new StreamPumper(videoStream, resp.getOutputStream()).run();
		log.info("Retrieved video for sessionId: " + sessionId);
	}
	
	private class SessionListener extends ActiveSessionListener {
		@Override
		public void onStop(ActiveSession session) {
			stopRecording(session.getId());
		}
	}
}
