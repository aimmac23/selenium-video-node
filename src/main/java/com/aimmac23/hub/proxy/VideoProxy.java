package com.aimmac23.hub.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.remote.internal.HttpClientFactory;
import org.openqa.selenium.remote.server.jmx.ManagedService;

import com.aimmac23.hub.HubVideoRegistry;

@ManagedService(description = "Selenium Grid Hub Video-Capable TestSlot")
public class VideoProxy extends DefaultRemoteProxy {

	private static final Logger log = Logger.getLogger(VideoProxy.class.getName());

	private String serviceUrl;
	boolean isCurrentlyRecording = false;
	private HttpClient client;

	public VideoProxy(RegistrationRequest request, GridRegistry registry) {
		super(RegistrationRequestCorrector.correctRegistrationRequest(request), registry);
		
		serviceUrl = getRemoteHost() + "/extra/VideoRecordingControlServlet";
		
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        client = httpClientFactory.getHttpClient();
        
        // its possible that the Hub may have crashed/been terminated/lost network connection, leaving the
        // node still recording. Tell it to get back to a known-state (https://github.com/aimmac23/selenium-video-node/issues/5)
		HttpPost r = new HttpPost(serviceUrl + "?command=reset");
		try {
			HttpResponse response = client.execute(r);
			
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String body = EntityUtils.toString(response.getEntity());
				log.log(Level.SEVERE, "Could not reset node " + this.getId() + 
						" to known recording status - incorrect HTTP response. Got: HTTP " + 
						response.getStatusLine().getStatusCode() + " code with body: '" + body + "'");
				// XXX: Does this mean that the node is unusable? We would probably take this path if the node was on an older
				// version of this library
			}			
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Could not reset node " + this.getId() + " to known recording status - caught exception", e);
			// XXX: Does this mean that the node is unusable?
		}
		finally {
			r.releaseConnection();
		}

	}
	
	@Override
	public void beforeSession(TestSession arg0) {
		super.beforeSession(arg0);
				
		HttpPost r = new HttpPost(serviceUrl + "?command=start");
		
        try {
			HttpResponse response = client.execute(r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not start video reporting: " + EntityUtils.toString(response.getEntity()));
				return;
			}
			
			isCurrentlyRecording = true;
			
			log.info("Started recording for new session on node: " + getId());

		} catch (Exception e) {
			log.warning("Could not start video reporting due to exception: " + e.getMessage());
			e.printStackTrace();
			return;
		}
        finally {
        	r.releaseConnection();
        }
	}
	
	@Override
	public void beforeCommand(TestSession session, HttpServletRequest request,
			HttpServletResponse response) {
		super.beforeCommand(session, request, response);
		
		SeleniumBasedRequest seleniumRequest = SeleniumBasedRequest.createFromRequest(request, getRegistry());
		
		// https://github.com/aimmac23/selenium-video-node/issues/10 - we need to mark the session as stopping, before we
		// pass back the client any information to prevent race conditions when downloading the video
		if(RequestType.STOP_SESSION.equals(seleniumRequest.getRequestType())) {
			if(isCurrentlyRecording) {
				HubVideoRegistry.declareSessionStopping(session);
			}
			else {
				log.severe("Recording not started for " + session.getExternalKey() + " on node " + getId() + " - cannot register session as stopping.");				
			}
		}
	}
	
	@Override
	public void afterCommand(TestSession session, HttpServletRequest request,
			HttpServletResponse response) {
		super.afterCommand(session, request, response);
		
		// its a shame we have to extract this again
		SeleniumBasedRequest seleniumRequest = SeleniumBasedRequest.createFromRequest(request, getRegistry());
		
		if(RequestType.STOP_SESSION.equals(seleniumRequest.getRequestType())) {
			
			if(isCurrentlyRecording) {
				log.info("Selenium session closed for " + session.getExternalKey() + " on node " + getId() + " - stopping recording.");
				stopRecording(session);
			}
			else {
				log.severe("Recording not started for " + session.getExternalKey() + " on node " + getId() + 
						" and session being deleted - this could be a bug in the code.");
			}
			
		}
		
	}
	
	@Override
	public void afterSession(TestSession session) {
		super.afterSession(session);
		
		if(isCurrentlyRecording) {
			log.warning("Session session terminated ungracefully for " + session.getExternalKey() + " on node " + getId() + " - stopping recording");
			stopRecording(session);
		}
	}
	
	void stopRecording(TestSession session) {
		
		HttpPost r = new HttpPost(serviceUrl + "?command=stop");
		
        try {
			HttpResponse response = client.execute(r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not stop video reporting: " + EntityUtils.toString(response.getEntity()));
			}
			
			isCurrentlyRecording = false;
			
			// check that the session didn't explode on startup
			if(session.getExternalKey() != null) {
				JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
				String fileKey = json.getString("filekey");
				HubVideoRegistry.copyVideoToHub(session, "/extra/VideoRecordingControlServlet?command=download&fileKey=" + fileKey, getRemoteHost());
				

			}
		} catch (Exception e) {
			log.warning("Could not stop video reporting due to exception: " + e.getMessage());
			e.printStackTrace();
		}
        finally {
        	r.releaseConnection();
        }
	}
}
