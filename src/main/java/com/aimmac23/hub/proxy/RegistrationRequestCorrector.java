package com.aimmac23.hub.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.grid.common.RegistrationRequest;
import org.openqa.selenium.MutableCapabilities;

/**
 * Some default configurations may try to register nodes that can run more than one browser in parallel. This is
 * a supported feature by Selenium, but doesn't work when trying to take a video of a running test (which will
 * only really record the topmost window).
 * 
 * This class re-writes incoming registration requests to remove any instances of parallelism.
 * 
 * Selenium 3 introduced a small number of incompatible changes, so this class tries to support Selenium 2 as well
 * by using reflection to do its work.
 * 
 * @author Alasdair Macmillan
 *
 */
public final class RegistrationRequestCorrector {

	private static final String MAX_SESSION_WARNING = "Reducing 'maxSession' value to 1: Video node does not support concurrent sessions";
	private static final Logger log = Logger.getLogger(RegistrationRequestCorrector.class.getName());

	public static RegistrationRequest correctRegistrationRequest(RegistrationRequest request) {
		try {
			new V3ConfigurationCorrector().fixupConfig(request);
		}
		catch(Exception e) {
			// not working? its probably running against Selenium v2, so try that next
			try {
				new V2ConfigurationCorrector().fixupConfig(request);
			}
			catch(Exception e2) {
				// if both code paths have failed, then the Selenium project has introduced incompatible code changes, so get the user to report
				// the original problem (discarding legacy exceptions)
				log.log(Level.SEVERE, "Couldn't correct Selenium registration request - this code may need recompiling to support new code changes. "
						+ "Please file a bug against the selenium-video-node project, and attach this stacktrace.", e);
				// this may later prove to be slightly unexpected behaviour, but continue with the original registration request, 
				// and hope that the config is OK.
			}
		}
		return request;
	}
	
	public interface ConfigurationCorrector {
		public void fixupConfig(RegistrationRequest request) throws Exception;
	}
	
	private static class V3ConfigurationCorrector implements ConfigurationCorrector {

		@SuppressWarnings("unchecked")
		@Override
		public void fixupConfig(RegistrationRequest request) throws Exception {
			Method getConfigurationMethod = request.getClass().getMethod("getConfiguration");
			Object configuration = getConfigurationMethod.invoke(request);
			Field maxSessionField = configuration.getClass().getField("maxSession");
			maxSessionField.setAccessible(true);
			Integer maxSessions = (Integer) maxSessionField.get(configuration);

			if(maxSessions != null && maxSessions != 1) {
				log.warning(MAX_SESSION_WARNING);
				maxSessionField.set(configuration, Integer.valueOf(1));
			}
			
			correctCapabilities((Collection<MutableCapabilities>) configuration.getClass().getField("capabilities").get(configuration));
		}
		
	}
	
	private static class V2ConfigurationCorrector implements ConfigurationCorrector {

		@SuppressWarnings("unchecked")
		@Override
		public void fixupConfig(RegistrationRequest request) throws Exception {
			Method getConfigurationMethod = request.getClass().getMethod("getConfigAsInt", String.class, int.class);
			int maxSessions = (Integer) getConfigurationMethod.invoke(request, "maxSession", Integer.valueOf(1));

			if(maxSessions != 1) {
				log.warning(MAX_SESSION_WARNING);
				Map<String, Object> configuration = (Map<String, Object>) request.getClass().getMethod("getConfiguration").invoke(request);
				configuration.put("maxSession", Integer.valueOf(1));
			}
			
			correctCapabilities((Collection<MutableCapabilities>) request.getClass().getMethod("getCapabilities").invoke(request));
		}
		
	}
	
	private static void correctCapabilities(Collection<MutableCapabilities> capabilities) {
		for(MutableCapabilities caps : capabilities) {
			Object maxInstances = caps.getCapability(RegistrationRequest.MAX_INSTANCES);
			caps.setCapability(RegistrationRequest.MAX_INSTANCES, "1");
			if(maxInstances != null && !"1".equals(maxInstances)) {
				log.warning("Reducing " + RegistrationRequest.MAX_INSTANCES + " for browser " + caps.getBrowserName() +
						" to 1: Video node does not support concurrent sessions");
			}
		}
	}
}
