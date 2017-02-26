Additional Features
===================

# Video Node Arguments

## Custom Framerate

    -Dvideo.framerate=<value>

Sets the framerate that we try to record video at - default value is "15" frames a second.

Selecting higher values has no harmful side-effects (apart from not getting the requested framerate), while selecting lower values will consume less CPU time.

## Xvfb Acceleration (Linux/Unix only)

Xvfb is a virtual display on Linux/Unix systems, which only renders the screen in memory (so no graphics cards/monitors are involved). With some additional configuration we can use some Xvfb features to improve video performance - see [Xvfb Acceleration](XVFB_ACCELERATION.md) on how to configure it.

## X11 Acceleration (Linux/Unix only)

As of version 2.3 we can use some simplified screenshot code to grab the screen image (currently off by default). To use this, pass this option to the node:

    -Dvideo.source=X11
    
You should then see the node print this at startup:

    Using X11 Native Screenshot Source

# Hub Arguments

## Video Storage Arguments

The Hub supports an extensible plugin system to decide how to store the recorded videos.

    -Dvideo.storage=<classname>

Changes the backend implementation for how we store videos. This can be either a plugin provided by this project, or a user-defined plugin on the classpath.

### com.aimmac23.hub.videostorage.LocalTempFileStore (default)

This implementation stores the videos as temporary files, and deletes them if too many accumulate (maximum of 200 videos). It also tends to forget about currently stored videos if the hub gets restarted.

There are currently no configurable options for this plugin.

### com.aimmac23.hub.videostorage.LocalFileVideoStore

Stores videos in a directory on disk. Videos persist over Hub restarts, and are never deleted. Note that clients can request any file in the configured directory, so be aware of where they are stored.

    -Dvideo.path=<path>
    
Uses the given directory to store the videos.

### com.aimmac23.hub.videostorage.BasicWebDAVStore

Uploads videos to a server which supports WebDAV. Following HTTP redirects when uploading is not supported.

    -Dwebdav.url=<url>
    
The URL can be either start with http:// or https://.

    -Dwebdav.username=<username>
    
Optionally provide a username to use when uploading the video.

    -Dwebdav.password=<password>
    
Optionally provide a password to use when uploading the video.

### User-Defined plugin

You can also create your own plugin to handle storage, but it has to implement com.aimmac23.hub.videostorage.IVideoStore.

## Miscellaneous Arguments

Sometimes the Hub will tell the test that the Session has been successfully closed slightly before it has finished copying the video off the node. If this has occurred, then the Hub will make any video download calls wait up to 20 seconds (by default) for the copying to finish. To control the time, set:

    -Dvideo.downloadTimeout=<timeout_in_milliseconds>

## Hub Video information servlet

The hub can also include information about the videos it is currently storing (such as the location), in case links to the videos need to be included in something else, such as a test report.

To use this, add another servlet to the hub:

    -servlets com.aimmac23.hub.servlet.HubVideoDownloadServlet,com.aimmac23.hub.servlet.HubVideoInfoServlet
    
You can then ask the Hub for information about videos it knows about:

    http://127.0.0.1:4444/grid/admin/HubVideoInfoServlet/?sessionId=14da5f32-5556-4003-bf62-da0ae7653358
    
And the hub will reply with a JSON response:

    {
        "additional": {"path": "/tmp/videos/14da5f32-5556-4003-bf62-da0ae7653358.webm"},
        "fileSize": 279497,
        "storageType": "LOCAL_FILE"
    }
    
To get file path information in the response, you will need to configure a video storage provider - see "Video Storage Arguments" above.
