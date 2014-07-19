package com.aimmac23.node;

import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.aimmac23.node.jna.XvfbScreenshotInterface;
import com.sun.jna.Pointer;

public class XvfbFileScreenshotSource implements ScreenshotSource {
	
	private XvfbScreenshotInterface xvfbInterface;
	private Pointer interfacePointer;

	public XvfbFileScreenshotSource() {
		xvfbInterface = JnaLibraryLoader.getXvfbInterface();
		
		interfacePointer = xvfbInterface.xvfb_interface_init("/var/tmp/Xvfb_screen0");
		
		if(interfacePointer == null) {
			throw new IllegalStateException("Could not create xvfb interface");
		}
	}

	@Override
	public int applyScreenshot(Pointer encoderContext) {
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();
		Pointer screenshotData = xvfbInterface.xvfb_interface_getScreenshot(interfacePointer);
		return encoder.convert_frame(encoderContext, screenshotData);
	}

	@Override
	public int getWidth() {
		return xvfbInterface.xvfb_interface_getWidth(interfacePointer);
	}

	@Override
	public int getHeight() {
		return xvfbInterface.xvfb_interface_getHeight(interfacePointer);
	}

}
