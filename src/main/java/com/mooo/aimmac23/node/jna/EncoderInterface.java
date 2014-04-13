package com.mooo.aimmac23.node.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


public interface EncoderInterface extends Library {
	EncoderInterface INSTANCE = (EncoderInterface)Native.loadLibrary("interface.so", EncoderInterface.class);
	
	Pointer create_context(String outputFile);
	
	int init_encoder(Pointer context, int width, int height);
	
	int init_codec(Pointer context);
	
	int init_image(Pointer context);
	
	int convert_frame(Pointer context, int[] data);
	
	int encode_next_frame(Pointer context, long duration);
	
	int encode_finish(Pointer context);
	
	String codec_error_detail(Pointer context);

}
	

