package com.aimmac23.node.args;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.aimmac23.node.RobotScreenshotSource;
import com.aimmac23.node.ScreenshotSource;
import com.aimmac23.node.X11ScreenshotSource;
import com.aimmac23.node.XvfbFileScreenshotSource;
import com.aimmac23.node.servlet.VideoRecordingControlServlet;
import com.google.common.base.Preconditions;

public class SystemPropertyRecordArgs implements IRecordArgs {
	
	private static final Logger log = Logger.getLogger(SystemPropertyRecordArgs.class.getName());

	@Override
	public int getTargetFramerate() {
		String framerateString = System.getProperty("video.framerate", "15");
		
		try {
			int framerate = Integer.parseInt(framerateString);
			Preconditions.checkArgument(framerate > 0, "video.framerate must be greater than zero!");
			return framerate;
		} catch(NumberFormatException e) {
			log.log(Level.SEVERE, "Couldn't parse video.framerate arg: '" + framerateString + "'");
			throw e;
		}
	}
	
	@Override
	public ScreenshotSource getNewScreenshotSource() {
		
		String videoSourceString = System.getProperty("video.source", "robot");
		File xvfbLocation = getXVFBFileOrNull();
		
		try {
			// TODO: The usage of this additional flag implies using the xvfb screenshot source - fix this in a future release?
			if(xvfbLocation != null) {
				return new XvfbFileScreenshotSource(xvfbLocation);
			}
			else if("x11".equalsIgnoreCase(videoSourceString)){
				return new X11ScreenshotSource();
			}
			else if("robot".equalsIgnoreCase(videoSourceString)) {
				return new RobotScreenshotSource();
			}
			else {
				throw new IllegalArgumentException("Unrecognised screenshot source: " + videoSourceString);
			}

		}
		catch(Exception e) {
			throw new IllegalStateException("Could not create screenshot source for video encoder", e);
		}
	}

	private File getXVFBFileOrNull() {
		String xvfbLocationString = System.getProperty("video.xvfbscreen", null);

		if(xvfbLocationString != null) {
			File xvfbDirectory = new File(xvfbLocationString);
			File xvfbFile = new File(xvfbDirectory, "Xvfb_screen0");
			if(!xvfbFile.exists()) {
				throw new IllegalStateException("Xvfb Screen location not found: " + xvfbFile);
			}
			else if(!xvfbFile.isFile()) {
				throw new IllegalStateException("Xvfb Screen location is not a file: " + xvfbFile);
			}
			else {
				return xvfbFile;
			}
		}
		else {
			return null;
		}
	}
}
