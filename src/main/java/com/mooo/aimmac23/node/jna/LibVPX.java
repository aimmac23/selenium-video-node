package com.mooo.aimmac23.node.jna;
import com.sun.jna.Library;
import com.sun.jna.Pointer;


public interface LibVPX extends Library {
	
	String vpx_codec_err_to_string(int result);
	
	String vpx_codec_iface_name(Pointer codecInterface);
	
	/**
	 * Returns a pointer to some sort of VP8 codec definition
	 * @return
	 */
	Pointer vpx_codec_vp8_cx();
}
