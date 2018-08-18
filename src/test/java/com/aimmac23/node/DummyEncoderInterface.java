package com.aimmac23.node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.aimmac23.node.jna.EncoderInterface;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.sun.jna.Pointer;

/**
 * An encoder implementation that write "*FRAME*" to the output file everytime it receives a "frame" of some kind.
 *
 */
final class DummyEncoderInterface implements EncoderInterface {
			
	// DO NOT PASS THIS INTO NATIVE CODE!
	Pointer myContext = new Pointer(0);

	private FileOutputStream fileOutputStream;

	private BufferedWriter writer;

	@Override
	public Pointer create_context(String outputFile) {
		Preconditions.checkArgument(new File(outputFile).exists());
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
		} catch (IOException e) {
			// not technically compliant with the EncoderInterface
			throw Throwables.propagate(e);
		}

		return myContext;
	}

	@Override
	public int init_encoder(Pointer context, int width, int height, int fps) {
		Preconditions.checkArgument(context == myContext);
		Preconditions.checkArgument(width > 0);
		Preconditions.checkArgument(height > 0);
		Preconditions.checkArgument(fps > 0);
		return 0;
	}

	@Override
	public int init_codec(Pointer context) {
		Preconditions.checkArgument(context == myContext);
		return 0;
	}

	@Override
	public int init_image(Pointer context) {
		Preconditions.checkArgument(context == myContext);
		return 0;
	}

	@Override
	public int convert_frame(Pointer context, int[] data) {
		Preconditions.checkArgument(context == myContext);
		return 0;
	}
	

	@Override
	public int convert_frame(Pointer context, byte[] data) {
		Preconditions.checkArgument(context == myContext);
		return 0;
	}

	@Override
	public int convert_frame(Pointer context, Pointer data) {
		Preconditions.checkArgument(context == myContext);
		return 0;
	}

	@Override
	public int encode_next_frame(Pointer context, long duration) {
		Preconditions.checkArgument(context == myContext);
		try {
			writer.write("*FRAME*");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int encode_finish(Pointer context) {
		Preconditions.checkArgument(context == myContext);
		IOUtils.closeQuietly(fileOutputStream);
		return 0;
	}

	@Override
	public String codec_error_detail(Pointer context) {
		throw new IllegalStateException("This implementation does not return errors");
	}

	
}