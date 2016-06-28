package com.aimmac23.node.jna;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

public class JnaLibraryLoader {
	
	private static final Logger log = Logger.getLogger(JnaLibraryLoader.class.getName());

	
	static private LibVPX libVPX;
	static private YUVLib yuvLib;
	static private LibMKV libMKV;
	static private EncoderInterface encoder;
	// optional dependency
	static private XvfbScreenshotInterface xvfbInterface;
	
	private static void addNativePath(String path) {
		NativeLibrary.addSearchPath("vpx_mine", path);
		NativeLibrary.addSearchPath("yuv", path);
		NativeLibrary.addSearchPath("mkv", path);
		NativeLibrary.addSearchPath("interface", path);
		
		NativeLibrary.addSearchPath("xvfb_interface", path);
	}
	
	private static File extractJNABinariesIfAvailable() {
		InputStream zipStream = JnaLibraryLoader.class.getClassLoader().getResourceAsStream("native.zip");
		if(zipStream == null) {
			throw new IllegalStateException("Native code zip file not found");
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
				File extractedFilePath = new File(targetDirectory + entry.getName());
				if(entry.isDirectory()) {
					extractedFilePath.mkdirs();	
					continue;
				}
				FileOutputStream outputStream = new FileOutputStream(extractedFilePath);
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
			
			// at this point, we will have extracted the native libraries into a new temporary folder, containing directories
			// "32bit" and "64bit". There is no platform-independent way to figure out what bit-size this JVM uses, and no way to
			// change the search path of JNA once added, so we're going to copy things onto the search path instead and see what happens.
			
			return target;
				
		} catch(IOException e) {
			log.info("Caught IOException");
			throw new IllegalStateException("Could not extract native libraries", e);
		}
	}
	
	public static void init() {
		
		File targetDirectory = extractJNABinariesIfAvailable();
		
		addShutdownCleanupHook(targetDirectory);
		
		// this makes things work in Eclipse
		String classpath = System.getProperty("java.class.path");
		String[] classpathEntries = classpath.split(File.pathSeparator);
		for(String entry : classpathEntries) {
			addNativePath(entry);
		}
		
		try {
			tryBitDepth(BitDepth.BIT_64, targetDirectory);	
		}
		catch(Error e) {
			log.info("Could not load 64 bit native libraries - attempting 32 bit instead");
			deleteAllFilesInDirectory(targetDirectory);
			tryBitDepth(BitDepth.BIT_32, targetDirectory);
		}
	}
	
	private static void deleteAllFilesInDirectory(File directory) {
		File[] files = directory.listFiles();
		for(File file : files) {
			if(file.isFile()) {
				file.delete();
			}
		}
	}
	
	private static void tryLoadLibraries() {

		libVPX = (LibVPX) Native.loadLibrary("vpx_mine", LibVPX.class);
		yuvLib = (YUVLib) Native.loadLibrary("yuv", YUVLib.class);
		libMKV = (LibMKV) Native.loadLibrary("mkv", LibMKV.class);
		encoder = (EncoderInterface) Native.loadLibrary("interface", EncoderInterface.class);
	}
	
	private static void tryBitDepth(BitDepth depth, File targetDirectory) {
		File sourceDirectory = new File(targetDirectory, depth.getDirectoryName());
		
		if(!sourceDirectory.exists()) {
			throw new IllegalStateException("Native code directory not found for bit depth: " + depth);
		}
		
		File[] filesToCopy = sourceDirectory.listFiles();
		
		for(File file : filesToCopy) {
			File destinationLocation = new File(targetDirectory, file.getName()); 
			try {
				FileUtils.copyFile(file, destinationLocation);
			}
			catch(IOException e) {
				throw new IllegalStateException("Could not copy file: " + file + " to: " + destinationLocation, e);
			}
		}
		
		tryLoadLibraries();
	}
	
	private static void disposeLibrary(Library lib, String name) {
		if(lib != null) {
			NativeLibrary.getInstance(name).dispose();
		}
	}
	
	private static synchronized void addShutdownCleanupHook(final File extractedDirectory) {
		
		Thread shutdownThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// close the native libraries in reverse order
				disposeLibrary(xvfbInterface, "xvfb_interface");
				
				disposeLibrary(encoder, "interface");
				
				disposeLibrary(libMKV, "mkv");
				disposeLibrary(yuvLib, "yuv");
				disposeLibrary(libVPX, "vpx");
				
				// Java File doesn't want to recursively delete things
				Iterator<File> iterator = Files.fileTreeTraverser().postOrderTraversal(extractedDirectory).iterator();
				while(iterator.hasNext()) {
					iterator.next().delete();
				}
			}
		}, "JNA Shutdown Hook Thread");
		
		Runtime.getRuntime().addShutdownHook(shutdownThread);
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
	
	public static XvfbScreenshotInterface getXvfbInterface() {
		if(xvfbInterface == null) {
			xvfbInterface = (XvfbScreenshotInterface) Native.loadLibrary("xvfb_interface", XvfbScreenshotInterface.class);
		}
		return xvfbInterface;
	}
	
	private static enum BitDepth {
		BIT_32("32bit"), BIT_64("64bit");
		
		private String directoryName;

		private BitDepth(String directoryName) {
			this.directoryName = directoryName;
		}
		
		public String getDirectoryName() {
			return directoryName;
		}
	}

}
