package com.aimmac23.hub.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

public abstract class AbstractHubVideoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// characters than can be used to get to parent directories
	private static final Set<String> FORBIDDEN_CHARACTERS = new HashSet<String>(Arrays.asList("/", "\\", "."));
	
	protected boolean checkValidSessionId(String sessionId, HttpServletResponse resp) throws IOException {

		// check to make sure that the user isn't abusing the backend storage (for security)
		for(String forbiddenCharacter : FORBIDDEN_CHARACTERS) {
			if(sessionId.contains(forbiddenCharacter)) {
				resp.setStatus(HttpStatus.SC_BAD_REQUEST);
				resp.getWriter().write("Session ID contained an invalid character: '" + forbiddenCharacter + "'");
				return false;
			}
		}
		
		return true;
	}	
}
