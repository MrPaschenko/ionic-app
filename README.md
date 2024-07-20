# geteduroam ionic-based mobile app

Application for configuring 802.1X Wi-Fi networks from eap-config profiles.

Pre-built versions are available on the [App Store](https://apps.apple.com/no/app/geteduroam/id1504076137)
and the [Play Store](https://play.google.com/store/apps/details?id=app.eduroam.geteduroam).

[<img src="https://github.com/user-attachments/assets/8963b828-6e5f-471e-8f43-73e1bfc0cc5f"
     alt="Download on the App Store"
     height="50">](https://apps.apple.com/no/app/geteduroam/id1504076137)&nbsp;&nbsp;&nbsp;&nbsp;[<img src="https://github.com/user-attachments/assets/6a34039e-e0d9-454c-9f81-93c258d016ae"
     alt="Get it on Google Play"
     height="50">](https://play.google.com/store/apps/details?id=app.eduroam.geteduroam)

## Building

The Android app can be built using Docker via the [Makefile]

	make apk

Without Docker, you can also set up the project using the Makefile

You must install these dependencies before running `make`

* [Android-SDK](https://developer.android.com/studio#downloads) (for Android only)
	* [Installation instructions for MacOS](ANDROID_STUDIO_MAC.md)
* [Node.js 14](NODE14.md) (for both Android and iOS)
* [Python 2](PYTHON2.md) (needed for building on some platforms, but not on all)
* [XCode](https://developer.apple.com/xcode/) (for iOS only)
	* If you have problems with the CLI, remember to use the correct developer tools  
	`sudo xcode-select -switch /Applications/Xcode.app/Contents/Developer`
* [CocoaPods](https://cocoapods.org)
	* `sudo gem install ffi -- --enable-libffi-alloc` (Apple Silicon)
	* `sudo gem install cocoapods` (Both Apple Silicon and Intel)

Then start Android Studio or XCode

	make open-android		# To start Android Studio
	make open-xcode  		# To start XCode on MacOS


If you want to ensure you're rebuilding the project,
remove all generated files using

	make clean


### Manual building

If you want to build manually, follow these instructions

* [Manually build Android app](DEV_ANDROID.md)
* [Manually build iOS app](DEV_IOS.md)
