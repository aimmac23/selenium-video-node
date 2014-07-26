package com.aimmac23.node;

import com.sun.jna.Pointer;

public interface ScreenshotSource {
	
	void doStartupSanityChecks();
	
	int applyScreenshot(Pointer encoderContext);
	
	int getWidth();
	
	int getHeight();
	
	

}
