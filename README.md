selenium-video-node
===================

This project adds video recording capability to a Selenium Grid. Videos are encoded into WebM/VP8 format on-the-fly and made available at the end of the test.

## Features

* Easy setup - just need to add another JAR file to the Selenium Node classpath
* Excellent video quality (about 8 frames/sec)
* Simple API to retrieve videos
* Extensible storage mechanism for video storage

## Mis-Features

* Video encoding is done using native code (using JNA) - unfortunately this means that additional effort is required to support more plaforms.

## Video Node System Requirements

* A dual core processor (one core for video encoding, another for everything else).

* A Java 6 JVM (or higher) running on one of the following platforms:

| Platform      | Support | Tested? |
|:---------------:|:---------:|:---------:|
| Linux x86_64 (64-bit) |  Yes        | Yes |
| Linux x86 (32-bit)   | Yes      | Yes |
| Windows x86_64 (64-bit) | Yes | Yes |
| Windows x86 (32-bit) | No | No |
| Mac OS X 10.9 x86_64 (64 bit)² | Yes | Yes |
| Mac OS X 10.5-10.8 x86_64 (64-bit)² | Yes | No |

² - Compiled with flag -mmacosx-version-min=10.5

The above does not apply to the Selenium Hub, which does not perform any video processing.

## Downloads

The latest version can be downloaded here: https://aimmac23.com/public/maven-repository/com/aimmac23/selenium-video-node/

## Basic Installation

To get the plugin working on your Selenium grid, see [Installation](INSTALLATION.md)

## Additional Features

The Video Nodes can change some of their behaviour, based on certain command-line parameters. See [Additional Features](ADDITIONAL_FEATURES.md) on how to make that happen.

