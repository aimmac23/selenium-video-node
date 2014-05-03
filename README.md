selenium-video-node
===================

This project adds video recording capability to a Selenium Grid. Videos are encoded into WebM/VP8 format on-the-fly and made available at the end of the test.

## Video Node System Requirements

* A dual core processor (one core for video encoding, another for everything else).
* 64-bit Linux on x86_64 (see "Mis-Features" below)
* 64-bit Windows on x86_64 (after version 0.4)

The above does not apply to the Selenium Hub, which does not perform any video processing.

## Features

* (Reasonably) easy setup - just need to add another JAR file to the Selenium Node classpath
* Excellent video quality (about 8 frames/sec)
* Simple API to retrieve videos

## Mis-Features

* Video encoding is done using native code (using JNA) - unfortunately this means that additional effort is required to support more plaforms.

## Installation

This guide assumes some familiarity with the Selenium Grid setup guide: https://code.google.com/p/selenium/wiki/Grid2

Download:
* The latest "selenium-server-standalone" JAR from http://selenium-release.storage.googleapis.com/index.html
* The latest "selenium-video-node" JAR from https://aimmac23.mooo.com/public/maven-repository/com/mooo/aimmac23/selenium-video-node/

### Node setup

A Selenium node can be launched by running:

    java -cp selenium-video-node-0.4.jar:selenium-server-standalone-2.40.0.jar org.openqa.grid.selenium.GridLauncher -servlets com.mooo.aimmac23.node.servlet.VideoRecordingControlServlet -proxy com.mooo.aimmac23.hub.proxy.VideoProxy -role node

If you are running this under Linux, then it may be convenient to run the Video Node under a virtual X server, so the real X server can be used for other purposes:

    xvfb-run -a -s "-screen 0 1280x1024x24 -wr" <video node start command>
    
When starting up you should see a line saying something like:

    08:47:44.173 INFO - started extra node servlet visible at : http://xxx:5555/extra/VideoRecordingControlServlet/*

This means that the extra JAR file has been found, and the native code loaded.

It is also recommended to specify only the browers that your platform supports and has installed (the default set is too permissive) - for example:

    <video node start command> -browser "browserName=firefox" -browser "browserName=chrome"
### Hub setup

We also need to add some extra functionality to the Selenium Hub to make this work:

    java -cp selenium-video-node-0.4.jar:selenium-server-standalone-2.40.0.jar org.openqa.grid.selenium.GridLauncher -servlets com.mooo.aimmac23.hub.servlet.HubVideoDownloadServlet -role hub

When starting up, you should see a line saying something like:

    INFO: binding com.mooo.aimmac23.hub.servlet.HubVideoDownloadServlet to /grid/admin/HubVideoDownloadServlet/*

Which means that the extra servlet in the Hub has been installed successfully.

## Usage

When the Hub starts a new session on the Video node, the Hub will send a request to the extra servlet on the Video node to start the video recording.

While the test is running the Video node will continuously record a video of the whole screen (not just the browser window).

When the test closes the test session (or the Hub decides it has expired due to session timeout), the Hub will request that the Video node stops recording, and finalises the video.

The Hub then retrieves the video off of the Video node, to make it available to the Hub video download servlet.

The video can then be downloaded by the tester by sending in a request like:

    http://<Hub URL>:4444/grid/admin/HubVideoDownloadServlet/?sessionId=<Driver Session ID>

For example:

    http://127.0.0.1:4444/grid/admin/HubVideoDownloadServlet/?sessionId=38d9cb20-f3d7-40d5-8e08-6f31b2ed1506
    
Note that videos are only available after the Session has been closed - for example, by calling RemoteWebDriver.quit() (as a Java example).

## Extra JVM Arguments:

    -Dvideo.framerate=<value>

Sets the framerate that we try to record video at - default value is "8" frames a second. At the moment we can only support up to about 10 frames a second (we spend a lot of time taking screenshots on the Java end). 

Selecting higher values seems to have no harmful side-effects (apart from not getting the requested framerate), while selecting lower values will consume less CPU resources.
