#VERSION := $(shell git describe --tags)

makefile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
makefile_dir := $(shell dirname ${makefile_path})
app_path := $(abspath $(makefile_dir)/../example)
gomobile_pkg := github.com/empirefox/flutter_dial_go/go/example/gomobile

echo:
	@APP_PATH=${app_path} \
		GOMOBILE_PKG=${gomobile_pkg} \
		make -f ${makefile_dir}/example/Makefile echo

bind-android:
	@APP_PATH=${app_path} \
		GOMOBILE_PKG=${gomobile_pkg} \
		make -f ${makefile_dir}/example/Makefile bind-android

	cd ${makefile_dir}/../example/android/go && \
		jar xf go.aar classes.jar && \
		mv classes.jar go.jar && \
		cp go.jar ${makefile_dir}/../android/libs/ && \
		cp go-sources.jar ${makefile_dir}/../android/libs/

bind-ios:
	@APP_PATH=${app_path} \
		GOMOBILE_PKG=${gomobile_pkg} \
		make -f ${makefile_dir}/example/Makefile bind-ios

protoc:
	@APP_PATH=${app_path} \
		GOMOBILE_PKG=${gomobile_pkg} \
		make -f ${makefile_dir}/example/Makefile protoc

clean:
	@APP_PATH=${app_path} \
		make -f ${makefile_dir}/example/Makefile clean
	rm -f ${makefile_dir}/../android/libs/*.aar
	rm -f ${makefile_dir}/../android/libs/*.jar
