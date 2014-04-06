package com.mooo.aimmac23.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

public class VideoRecordingControlServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private VideoRecordController controller;
	

	public VideoRecordingControlServlet() {
		super();
		controller = new VideoRecordController();
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
			controller.startRecording();
			resp.getWriter().write("Started Recording");
			return;
		}
		else if(command.equalsIgnoreCase("stop")) {
			try {
				File videoFile = controller.stopRecording();
				resp.getWriter().write("Video file: " + videoFile);
				return;
			} catch (Exception e) {
				resp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				resp.getWriter().write("Internal Server Error: Caught Exception: " + e.getMessage());
				e.printStackTrace();
				return;
			}
			
		}
		else {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			resp.getWriter().write("Bad parameter: 'command', must be either 'start' or 'stop'");
			return;		
		}
	}

}
