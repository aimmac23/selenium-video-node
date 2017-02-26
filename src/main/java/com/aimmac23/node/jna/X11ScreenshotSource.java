package com.aimmac23.node.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface X11ScreenshotSource extends Library {

	Pointer x11_screenshot_source_init();
	
	String x11_screenshot_source_sanityChecks(Pointer x11ScreenshotContext);
		
	Pointer x11_screenshot_source_getScreenshot(Pointer x11ScreenshotContext);
	
	void x11_screenshot_source_destroy(Pointer x11ScreenshotContext);
	
	int x11_screenshot_source_getWidth(Pointer x11ScreenshotContext);
	int x11_screenshot_source_getHeight(Pointer x11ScreenshotContext);
}
