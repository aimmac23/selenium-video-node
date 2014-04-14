package com.mooo.aimmac23.node;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.mooo.aimmac23.node.jna.EncoderInterface;
import com.mooo.aimmac23.node.jna.JnaLibraryLoader;
import com.sun.jna.Pointer;

public class RecordVideoCallable implements Callable<File> {
	
	private static final Logger log = Logger.getLogger(RecordVideoCallable.class.getSimpleName());
	
	public static final int TARGET_FRAMERATE = 8;
	public static final int TARGET_FRAMERATE_TIME = (int)((1.0 / TARGET_FRAMERATE) * 1000.0);
	
	
	private volatile boolean shouldStop = false;

	@Override
	public File call() throws Exception {
		int frames = 0;
		
		Rectangle screenSize = getScreenSize();

		File outputFile = File.createTempFile("screencast", ".webm");
		
		JnaLibraryLoader.init();
		
		EncoderInterface encoder = JnaLibraryLoader.getEncoder();
		
		Pointer context = encoder.create_context(outputFile.getCanonicalPath());
		int result = encoder.init_encoder(context, (int)screenSize.getWidth(),
						(int) screenSize.getHeight(), TARGET_FRAMERATE);
		
		encoder.init_codec(context);
		
		encoder.init_image(context);
				
		log.info("Started recording to file: " + outputFile.getCanonicalPath());
		Robot robot = new Robot();
		
		long excessTime = 0;

		long videoStartTime = System.currentTimeMillis();
		while(!shouldStop) {
			// how long to use the next frame for - should be 1 if we're not falling behind
			int frameDuration = 1 + (int)(excessTime / TARGET_FRAMERATE_TIME);
			// if excessTime > TARGET_FRAMERATE_TIME. then we've just taken that into account
			excessTime = excessTime % TARGET_FRAMERATE_TIME;
			long start = System.currentTimeMillis();
			BufferedImage image = robot.createScreenCapture(screenSize);
			
			int[] data = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

			encoder.convert_frame(context, data);
			
			encoder.encode_next_frame(context, frameDuration);
			long finish = System.currentTimeMillis();
			frames++;
			long timeTaken = finish - start;
			// we're keeping up
			if(timeTaken < TARGET_FRAMERATE_TIME) {
				Thread.sleep(TARGET_FRAMERATE_TIME - timeTaken);
			}
			else {
				// we're falling behind, take that into account for the next frame
				excessTime += timeTaken;
			}
		}
		encoder.encode_finish(context);
		
		long videoEndTime = System.currentTimeMillis();
		
		long duration = ((videoEndTime - videoStartTime) / 1000);
		log.info("Finished recording - frames: " + frames + " duration: " +  duration + " seconds  fps: " + frames / duration);
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
