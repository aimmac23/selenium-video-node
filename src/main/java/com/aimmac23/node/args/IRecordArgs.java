package com.aimmac23.node.args;

import com.aimmac23.node.ScreenshotSource;

public interface IRecordArgs {
	
	int getTargetFramerate();
	
	/**
	 * @return An uninitialised screenshot source
	 */
	ScreenshotSource getNewScreenshotSource();
	

}
