package com.aimmac23.node;

import java.io.File;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.aimmac23.node.args.IRecordArgs;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.LibVPX;

public class VideoRecordControllerTest {

	private final class TestRecordArgs implements IRecordArgs {
		
		private EncoderInterface encoderInterface;

		public TestRecordArgs(EncoderInterface encoderInterface) {
			this.encoderInterface = encoderInterface;
		}
		
		@Override
		public int getTargetFramerate() {
			return 15;
		}

		@Override
		public ScreenshotSource getNewScreenshotSource() {
			return new DummyScreenshotSource(encoderInterface);
		}
	}
	
	@Test
	public void canCreateForThisPlatform() throws Exception {

		// mock the LibVPX dependency - technically we only need it for error handling...
		LibVPX vpx = EasyMock.createStrictMock(LibVPX.class);
		EncoderInterface encoderInterface = new DummyEncoderInterface();
				
		EasyMock.replay(vpx);
		IRecordArgs args = new TestRecordArgs(encoderInterface);
		try (VideoRecordController controller = new VideoRecordController(args, vpx, encoderInterface)) {
			controller.startRecording();

			Thread.sleep(1000);

			File recording = controller.stopRecording();

			Assert.assertTrue("Recording file does not exist!", recording.exists());
			Assert.assertTrue("File is zero length!", recording.length() > 0);
		}
		
		EasyMock.verify(vpx);
	}
}
