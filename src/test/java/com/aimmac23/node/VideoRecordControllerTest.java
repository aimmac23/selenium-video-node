package com.aimmac23.node;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.aimmac23.node.RobotScreenshotSource;
import com.aimmac23.node.ScreenshotSource;
import com.aimmac23.node.VideoRecordController;
import com.aimmac23.node.args.IRecordArgs;
import com.aimmac23.node.jna.JnaLibraryLoader;
import com.google.common.base.Throwables;

public class VideoRecordControllerTest {

	private final class TestRecordArgs implements IRecordArgs {
		
		@Override
		public int getTargetFramerate() {
			return 15;
		}

		@Override
		public ScreenshotSource getNewScreenshotSource() {
			return new DummyScreenshotSource();
		}
	}

	@Test
	public void canCreateForThisPlatform() throws Exception {

		JnaLibraryLoader.init();
		IRecordArgs args = new TestRecordArgs();
		try (VideoRecordController controller = new VideoRecordController(args, JnaLibraryLoader.getLibVPX(), JnaLibraryLoader.getEncoder())) {
			controller.startRecording();

			Thread.sleep(2000);

			File recording = controller.stopRecording();

			Assert.assertTrue("Recording file does not exist!", recording.exists());
			Assert.assertTrue("File is zero length!", recording.length() > 0);
		}
	}
}
