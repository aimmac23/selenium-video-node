Build: ![Build Status](https://aimmac23.com/private/jenkins/job/Github/job/selenium-video-node%20(MASTER)/badge/icon)
Latest Selenium 3.x: ![Build Status](https://aimmac23.com/private/jenkins/job/Github/job/selenium-video-node%20(latest%20Selenium%203.x)/badge/icon)

selenium-video-node
===================

This project adds video recording capability to a Selenium Grid. Videos are encoded into WebM/VP8 format on-the-fly and made available at the end of the test.

## Features

* Easy setup - just need to add another JAR file to the Selenium Node classpath
* Excellent video quality (at about 8-15 frames/sec)
* Additional support when running under the Xvfb virtual X Server on Linux (6 times faster video encoding)
* Simple API to retrieve videos
* Extensible storage mechanism for video storage

## Mis-Features

* Video encoding is done using native code (using JNA) - unfortunately this means that additional effort is required to support more plaforms.

## Example videos

For example videos recorded using this plugin, [see the examples page](http://selenium-videos.s3-website-eu-west-1.amazonaws.com/).

## Video Node System Requirements

* A dual core processor (one core for video encoding, another for everything else). It is possible to lower this requirement by reducing the framerate.

* 32-bit colour display output - 8 bits red, 8 bits blue, 8 bits green, with either 8 bits of alpha or 8 bits unused. [See Wikipedia](http://en.wikipedia.org/wiki/Color_depth#True_color_.2824-bit.29) for more information.

* A Java 6 JVM (or higher) running on one of the following platforms:

| Platform      | Support | Tested? | Jenkins Build |
|:---------------:|:---------:|:---------:|:--------:|
| Linux x86_64 (64-bit) |  Yes        | Yes | ![Build Status](https://aimmac23.com/private/jenkins/job/Github/job/selenium-video-node%20(64-bit%20Linux)/badge/icon) |
| Linux x86 (32-bit)   | Yes      | Yes | ![Build Status](https://aimmac23.com/private/jenkins/job/Github/job/selenium-video-node%20(32-bit%20Linux)/badge/icon) |
| Windows x86_64 (64-bit) | Yes¹ | Yes¹ | |
| Windows x86 (32-bit) | Yes | Yes | |
| Mac OS X 10.9 x86_64 (64 bit)² | Yes | Yes | |
| Mac OS X 10.5-10.8 x86_64 (64-bit)² | Yes | No | |

¹ - Compiled and tested on Windows 7.
² - Compiled with flag -mmacosx-version-min=10.5 - it should work with older OS X versions.

The above does not apply to the Selenium Hub, which does not perform any video processing.

## Downloads

Download the latest version from Maven Central:

http://repo1.maven.org/maven2/com/aimmac23/selenium-video-node/

Older releases can be found here:

https://aimmac23.com/public/maven-repository/com/aimmac23/selenium-video-node/

Alternatively, if you are creating your own Selenium Maven project, add the following to your POM file's dependency section:

    <dependency>
        <groupId>com.aimmac23</groupId>
        <artifactId>selenium-video-node</artifactId>
        <version>2.8</version>
    </dependency>
  
## Basic Installation

To get the plugin working on your Selenium grid, see [Installation](INSTALLATION.md)

## Additional Features

The Video Nodes can change some of their behaviour, based on certain command-line parameters. See [Additional Features](ADDITIONAL_FEATURES.md) on how to make that happen.

