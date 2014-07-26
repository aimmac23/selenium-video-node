package com.aimmac23.node.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * The native code dependency to pull screenshot data directly from Xvfb's
 * output buffer (when its been mapped to a file).
 * 
 * @author Alasdair Macmillan
 *
 */
public interface XvfbScreenshotInterface extends Library {
	
	Pointer xvfb_interface_init(String frameBufferPath);
	
	String xvfb_interface_sanityChecks(Pointer xvfbInterface);
		
	Pointer xvfb_interface_getScreenshot(Pointer xvfbInterface);
	
	int xvfb_interface_getWidth(Pointer xvfbInterface);
	int xvfb_interface_getHeight(Pointer xvfbInterface);
}
