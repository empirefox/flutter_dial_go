#VERSION := $(shell git describe --tags)
# This is just used in all, so as to have something in the arch and OS

android:
	gomobile bind -target android -o ../example/android/go/go.aar \
		github.com/empirefox/flutter_dial_go/go/example/gomobile \
		github.com/empirefox/flutter_dial_go/go/formobile
	cp ../example/android/go/go.aar ../android/libs/
	cp ../example/android/go/go-sources.jar ../android/libs/

ios:
	gomobile bind -target ios -o ../example/Frameworks/ios/Gomobile.framework \
		github.com/empirefox/flutter_dial_go/go/example/gomobile \
		github.com/empirefox/flutter_dial_go/go/formobile

clean:
	rm ../example/android/go/*.aar
	rm ../example/android/go/*.jar
	rm ../android/libs/*.aar
	rm ../android/libs/*.jar
	rm -rf ../example/Frameworks/ios/Gomobile.framework

