package com.mooo.aimmac23.hub.proxy;

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.internal.HttpClientFactory;

import com.mooo.aimmac23.hub.HubVideoRegistry;

public class VideoProxy extends DefaultRemoteProxy {

    private static final Logger log = Logger.getLogger(VideoProxy.class.getName());

	private String serviceUrl;
	boolean isCurrentlyRecording = false;
	private HttpClient client;
	private HttpHost remoteHost;
	
	public VideoProxy(RegistrationRequest request, Registry registry) {
		super(transformRegistration(request), registry);
		
		serviceUrl = getRemoteHost() + "/extra/VideoRecordingControlServlet";
		
		remoteHost = new HttpHost(getRemoteHost().getHost(),
				getRemoteHost().getPort());
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        client = httpClientFactory.getHttpClient();
	}
	
	static RegistrationRequest transformRegistration(RegistrationRequest request) {
		int maxSessions = request.getConfigAsInt(RegistrationRequest.MAX_SESSION, 1);
		request.getConfiguration().put(RegistrationRequest.MAX_SESSION, 1);
		
		if(maxSessions != 1) {
			log.warning("Reducing " + RegistrationRequest.MAX_SESSION + " value to 1: Video node does not support concurrent sessions");
		}
		
		for(DesiredCapabilities caps : request.getCapabilities()) {
			Object maxInstances = caps.getCapability(RegistrationRequest.MAX_INSTANCES);
			caps.setCapability(RegistrationRequest.MAX_INSTANCES, "1");
			if(maxInstances != null && !"1".equals(maxInstances)) {
				log.warning("Reducing " + RegistrationRequest.MAX_INSTANCES + " for browser " + caps.getBrowserName() + 
						" to 1: Video node does not support concurrent sessions");
			}
		}
			
		return request;
	}
	
	@Override
	public void beforeSession(TestSession arg0) {
		super.beforeSession(arg0);
				
		BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest(
                "POST", serviceUrl + "?command=start");
		
        try {
			HttpResponse response = client.execute(remoteHost, r);
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
		
		BasicHttpEntityEnclosingRequest r = new BasicHttpEntityEnclosingRequest(
                "POST", serviceUrl + "?command=stop");
		
        try {
			HttpResponse response = client.execute(remoteHost, r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not stop video reporting: " + EntityUtils.toString(response.getEntity()));
			}
			
			isCurrentlyRecording = false;
			
			// check that the session didn't explode on startup
			if(session.getExternalKey() != null) {
				JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));
				String fileKey = json.getString("filekey");
				HubVideoRegistry.copyVideoToHub(session, fileKey, getRemoteHost());
				

			}
		} catch (Exception e) {
			log.warning("Could not stop video reporting due to exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
