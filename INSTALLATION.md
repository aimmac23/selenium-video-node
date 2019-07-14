 
Installation
============

This guide assumes some familiarity with the Selenium Grid setup guide: https://code.google.com/p/selenium/wiki/Grid2

Download:
* The latest "selenium-server-standalone" JAR from http://selenium-release.storage.googleapis.com/index.html
* The latest "selenium-video-node" JAR from http://repo1.maven.org/maven2/com/aimmac23/selenium-video-node/

### Node setup

A Selenium node can be launched by running:

    java -cp selenium-video-node-2.9.jar:selenium-server-standalone-3.141.59.jar org.openqa.grid.selenium.GridLauncherV3 -servlets com.aimmac23.node.servlet.VideoRecordingControlServlet -proxy com.aimmac23.hub.proxy.VideoProxy -role wd

Note that on Windows, the classpath seperator (for the -cp argument) is ";" instead of ":" - Java will not report incorrect usage as an error, but you will get ClassNotFound exceptions.

If you are running this under Linux, then it may be convenient to run the Video Node under a virtual X server, so the real X server can be used for other purposes:

    xvfb-run -a -s "-screen 0 1280x1024x24 -wr" <video node start command>
    
When starting up you should see a line saying something like:

    08:47:44.173 INFO - binding com.aimmac23.node.servlet.VideoRecordingControlServlet to /extra/VideoRecordingControlServlet/*

This means that the extra JAR file has been found, and the native code loaded.

It is also recommended to specify only the browers that your platform supports and has installed (the default set is too permissive) - for example:

    <video node start command> -browser "browserName=firefox" -browser "browserName=chrome"
### Hub setup

We also need to add some extra functionality to the Selenium Hub to make this work:

    java -cp selenium-video-node-2.9.jar:selenium-server-standalone-3.141.59.jar org.openqa.grid.selenium.GridLauncherV3 -servlets com.aimmac23.hub.servlet.HubVideoDownloadServlet -role hub

Note that again on Windows, the classpath separator should again be ";" instead of ":", otherwise you will get ClassNotFound exceptions.

When starting up, you should see a line saying something like:

    INFO: binding com.aimmac23.hub.servlet.HubVideoDownloadServlet to /grid/admin/HubVideoDownloadServlet/*

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

WARNING: By default the Hub will only store 200 recently recorded videos to avoid diskfilling the machine. See [Additional Features](ADDITIONAL_FEATURES.md) on how to change this behaviour.

## Code Examples

See [ExampleSeleniumTests.java](src/test/java/com/aimmac23/hub/examples/ExampleSeleniumTests.java) and [AbstractVideoSeleniumTest.java](src/test/java/com/aimmac23/hub/examples/AbstractVideoSeleniumTest.java) for a Java example how how you can integrate videos into your Selenium tests.

