Additional Features
===================

# Video Node Arguments

    -Dvideo.framerate=<value>

Sets the framerate that we try to record video at - default value is "15" frames a second.

Selecting higher values seems to have no harmful side-effects (apart from not getting the requested framerate), while selecting lower values will consume less CPU resources.


    -D-Dvideo.xvfbscreen=<path>
    
Enables Xvfb video encoding acceleration (Linux only, use of Xvfb virtual X Server required). See [Xvfb Acceleration](XVFB_ACCELERATION.md) on how to configure it.

# Hub Arguments

## Video Storage Arguments

The Hub supports an extensible plugin system to decide how to store the recorded videos.

    -Dvideo.storage=<classname>

Changes the backend implementation for how we store videos. This can be either a plugin provided by this project, or a user-defined plugin on the classpath.

### com.aimmac23.hub.videostorage.LocalTempFileStore (default)

This implementation stores the videos as temporary files, and deletes them if too many accumulate. It also tends to forget about currently stored videos if the hub gets restarted.

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

