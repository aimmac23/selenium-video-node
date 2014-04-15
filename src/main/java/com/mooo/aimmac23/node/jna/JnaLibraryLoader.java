package com.mooo.aimmac23.node.jna;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class JnaLibraryLoader {
	
	private static final Logger log = Logger.getLogger(JnaLibraryLoader.class.getName());

	
	static private LibVPX libVPX;
	static private EncoderInterface encoder;
	static private YUVLib yuvLib;
	
	private static void addNativePath(String path) {
		NativeLibrary.addSearchPath("vpx", path);
		NativeLibrary.addSearchPath("yuv", path);
		NativeLibrary.addSearchPath("interface", path);
	}
	
	private static void extractJNABinariesIfAvailable() {
		InputStream zipStream = JnaLibraryLoader.class.getClassLoader().getResourceAsStream("native.zip");
		if(zipStream == null) {
			return;
		}
		
		String targetDirectory = System.getProperty("java.io.tmpdir") + File.separator + "nativeCode-" + System.currentTimeMillis() + File.separator;
		
		File target = new File(targetDirectory);
		if(target.exists()) {
			target.delete();
		}
		
		target.mkdir();
		
		try {
			ZipInputStream zipInputStream = new ZipInputStream(zipStream);
			ZipEntry entry = null;
			while((entry = zipInputStream.getNextEntry()) != null){
				FileOutputStream outputStream = new FileOutputStream(targetDirectory + entry.getName());
				byte[] buffer = new byte[1024];
				int size = 0;
                while((size = zipInputStream.read(buffer)) != -1){
                	outputStream.write(buffer, 0 , size);
                }
                outputStream.flush();
                outputStream.close();
			}
			zipInputStream.close();
			zipStream.close();
			
			addNativePath(targetDirectory);
				
		} catch(IOException e) {
			log.info("Caught IOException");
			e.printStackTrace();
		}
	}
	
	public static void init() {
		
		extractJNABinariesIfAvailable();
		
		// this makes things work in Eclipse
		String classpath = System.getProperty("java.class.path");
		String[] classpathEntries = classpath.split(File.pathSeparator);
		for(String entry : classpathEntries) {
			addNativePath(entry);
		}
		
		libVPX = (LibVPX) Native.loadLibrary("vpx", LibVPX.class);
		yuvLib = (YUVLib) Native.loadLibrary("yuv", YUVLib.class);
		encoder = (EncoderInterface) Native.loadLibrary("interface", EncoderInterface.class);
	}
	
	public static EncoderInterface getEncoder() {
		return encoder;
	}
	
	public static YUVLib getYuvLib() {
		return yuvLib;
	}
	
	public static LibVPX getLibVPX() {
		return libVPX;
	}

}
