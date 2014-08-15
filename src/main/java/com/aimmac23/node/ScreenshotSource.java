package com.aimmac23.node;

import com.aimmac23.exception.MissingFrameException;
import com.sun.jna.Pointer;

public interface ScreenshotSource {
	
	void doStartupSanityChecks();
	
	int applyScreenshot(Pointer encoderContext) throws MissingFrameException;
	
	int getWidth();
	
	int getHeight();
	
	

}
