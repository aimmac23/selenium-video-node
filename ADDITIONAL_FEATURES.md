 

 # Additional Features
 
 ## Extra JVM Arguments:

    -Dvideo.framerate=<value>

Sets the framerate that we try to record video at - default value is "8" frames a second. At the moment we can only support up to about 10 frames a second (we spend a lot of time taking screenshots on the Java end). 

Selecting higher values seems to have no harmful side-effects (apart from not getting the requested framerate), while selecting lower values will consume less CPU resources.
