package com.mooo.aimmac23.node;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VideoRecordController {
	
	private ThreadPoolExecutor executor;
	RecordVideoCallable currentCallable;
	private Future<File> currentFuture;
	
	public VideoRecordController() {
		executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(5));
		executor.setThreadFactory(new RecorderThreadFactory());
		executor.prestartAllCoreThreads();
	}
	
	public void startRecording() {
		if(currentCallable != null) {
			throw new IllegalStateException("Video recording currently in progress, cannot record again");
		}
		currentCallable = new RecordVideoCallable();
		currentFuture = executor.submit(currentCallable);
	}
	
	public File stopRecording() throws Exception {
		if(currentCallable == null) {
			throw new IllegalStateException("Video recording not currently in progress, cannot stop!");
		}
		currentCallable.stopRecording();
		currentCallable = null;
		return currentFuture.get();
	}
	class RecorderThreadFactory implements ThreadFactory {
		
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("VideoRecordingThread");
			return thread;
		}
	}
}
