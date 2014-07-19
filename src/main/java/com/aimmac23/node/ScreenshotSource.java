package com.aimmac23.node;

import com.sun.jna.Pointer;

public interface ScreenshotSource {
	
	int applyScreenshot(Pointer encoderContext);
	
	int getWidth();
	
	int getHeight();
	
	

}
