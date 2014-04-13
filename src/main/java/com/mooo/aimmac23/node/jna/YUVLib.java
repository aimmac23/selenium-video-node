package com.mooo.aimmac23.node.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;


public interface YUVLib extends Library {
	YUVLib INSTANCE = (YUVLib)Native.loadLibrary("yuv", YUVLib.class);
}