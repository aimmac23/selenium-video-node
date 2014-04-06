package com.mooo.aimmac23.servlet;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;

import com.mooo.aimmac23.jcodec.SequenceEncoder;

public class RecordVideoCallable implements Callable<File> {
	
	public static final int TARGET_FRAMERATE = 25;
	public static final int TARGET_FRAMERATE_TIME = (int)((1.0 / TARGET_FRAMERATE) * 1000.0);
	
	
	private volatile boolean shouldStop = false;

	@Override
	public File call() throws Exception {
		int frames = 0;
		File outputFile = File.createTempFile("screencast", ".mp4");
		SequenceEncoder encoder = new SequenceEncoder(outputFile, TARGET_FRAMERATE);
		
		Robot robot = new Robot();
		
		long excessTime = 0;

		Rectangle screenSize = getScreenSize();
		while(!shouldStop) {
			// how long to use the next frame for - should be 1 if we're not falling behind
			int frameDuration = 1 + (int)(excessTime / TARGET_FRAMERATE_TIME);
			// if excessTime > TARGET_FRAMERATE_TIME. then we've just taken that into account
			excessTime = excessTime % TARGET_FRAMERATE_TIME;
			System.out.println("Frame duration: " + frameDuration);
			long start = System.currentTimeMillis();
			BufferedImage image = robot.createScreenCapture(screenSize);
			encoder.encodeImage(image, frameDuration);
			long finish = System.currentTimeMillis();
			frames++;
			long timeTaken = finish - start;
			// we're keeping up
			if(timeTaken < TARGET_FRAMERATE_TIME) {
				System.out.println("We needed to sleep for " + (finish - start));
				Thread.sleep(TARGET_FRAMERATE_TIME - timeTaken);
			}
			else {
				// we're falling behind, take that into account for the next frame
				excessTime += timeTaken;
			}
		}
		
		encoder.finish();
		System.out.println("Frames: " + frames);
		return outputFile;
	}
	
	public void stopRecording() {
		shouldStop = true;
	}
	
	private Rectangle getScreenSize() {
		//XXX: This probably won't work with multiple monitors
		return GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().getBounds();
		
	}

}
