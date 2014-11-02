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

* 32-bit colour display output (8 bits red, 8 bits blue, 8 bits green, 8 bit alpha/unused).

* A Java 6 JVM (or higher) running on one of the following platforms:

| Platform      | Support | Tested? |
|:---------------:|:---------:|:---------:|
| Linux x86_64 (64-bit) |  Yes        | Yes |
| Linux x86 (32-bit)   | Yes      | Yes |
| Windows x86_64 (64-bit) | Yes¹ | Yes¹ |
| Windows x86 (32-bit) | No | No |
| Mac OS X 10.9 x86_64 (64 bit)² | Yes | Yes |
| Mac OS X 10.5-10.8 x86_64 (64-bit)² | Yes | No |

¹ - Compiled and tested on Windows 7.
² - Compiled with flag -mmacosx-version-min=10.5 - it should work with older OS X versions.

The above does not apply to the Selenium Hub, which does not perform any video processing.

## Downloads

The latest version can be downloaded here: https://aimmac23.com/public/maven-repository/com/aimmac23/selenium-video-node/

## Basic Installation

To get the plugin working on your Selenium grid, see [Installation](INSTALLATION.md)

## Additional Features

The Video Nodes can change some of their behaviour, based on certain command-line parameters. See [Additional Features](ADDITIONAL_FEATURES.md) on how to make that happen.

