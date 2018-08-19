package com.aimmac23.hub.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.openqa.grid.common.RegistrationRequest;
import org.openqa.grid.internal.ExternalSessionKey;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.selenium.proxy.DefaultRemoteProxy;
import org.openqa.grid.web.servlet.handler.RequestType;
import org.openqa.grid.web.servlet.handler.SeleniumBasedRequest;
import org.openqa.selenium.remote.internal.HttpClientFactory;
import org.openqa.selenium.remote.server.jmx.ManagedService;

import com.aimmac23.hub.HubVideoRegistry;

@ManagedService(description = "Selenium Grid Hub Video-Capable TestSlot")
public class TestVideoProxy extends DefaultRemoteProxy {

	private static final Logger log = Logger.getLogger(TestVideoProxy.class.getName());

	private String serviceUrl;
	private HttpClient client;

	public TestVideoProxy(RegistrationRequest request, GridRegistry registry) {
		super(RegistrationRequestCorrector.correctRegistrationRequest(request), registry);
		
		serviceUrl = getRemoteHost() + "/extra/TestVideoRecordServlet";
		
        HttpClientFactory httpClientFactory = new HttpClientFactory();
        client = httpClientFactory.getHttpClient();
        
        // The WebDriver servlet initializes a few objects we need - make sure Jetty hasn't lazy-initialized the Servlet
        forceNodeServletInitialization();

	}
	
	private void forceNodeServletInitialization() {
		HttpGet r = new HttpGet(getRemoteHost() + "/wd/hub/");
		
        try {
			HttpResponse response = client.execute(r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				String stringEntity = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());
				log.warning("Could not initialize node servlet: " + stringEntity);
				return;
			}

		} catch (Exception e) {
			log.log(Level.WARNING, "Could not initialize node servlet due to exception", e);
			return;
		}
        finally {
        	r.releaseConnection();
        }
	}
	
	private void startRecordingForSession(ExternalSessionKey sessionId) {
		
		HttpPost r = new HttpPost(serviceUrl + "?command=start&sessionId=" + sessionId);
		
        try {
			HttpResponse response = client.execute(r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not start video reporting: " + EntityUtils.toString(response.getEntity()));
				return;
			}
						
			log.info("Started recording for new session: " + sessionId + " on node: " + getId());

		} catch (Exception e) {
			log.log(Level.WARNING, "Could not start video for session: " + sessionId + " reporting due to exception", e);
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
		
		if(RequestType.STOP_SESSION.equals(seleniumRequest.getRequestType())) {
			// TODO: Check if we still need this line
			HubVideoRegistry.declareSessionStopping(session);
			stopRecording(session);
		}
	}
	
	@Override
	public void afterCommand(TestSession session, HttpServletRequest request,
			HttpServletResponse response) {
		super.afterCommand(session, request, response);
		
		// its a shame we have to extract this again
		SeleniumBasedRequest seleniumRequest = SeleniumBasedRequest.createFromRequest(request, getRegistry());
		
		if(RequestType.START_SESSION.equals(seleniumRequest.getRequestType()) && session.getExternalKey() != null) {
			startRecordingForSession(session.getExternalKey());
		}
	}
	
	@Override
	public void afterSession(TestSession session) {
		super.afterSession(session);
		
		
	}
	
	void stopRecording(TestSession session) {
		
		// assume session startup failed - nothing to do here
		if(session.getExternalKey() == null) {
			return;
		}
		
		HttpPost r = new HttpPost(serviceUrl + "?command=stop&sessionId=" + session.getExternalKey());
		
        try {
			HttpResponse response = client.execute(r);
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.warning("Could not stop video reporting: " + EntityUtils.toString(response.getEntity()));
			}
			else {
				HubVideoRegistry.copyVideoToHub(session, session.getExternalKey().getKey(), getRemoteHost());
			}
			
		} catch (Exception e) {
			log.log(Level.WARNING, "Could not stop video reporting for session: " + session.getExternalKey() + " due to exception", e);
		}
        finally {
        	r.releaseConnection();
        }
	}
}
