#VERSION := $(shell git describe --tags)

GOMOBILE_PKG := ${GOMOBILE_PKG}
APP_PATH := ${APP_PATH}

makefile_dir := $(shell dirname ${GOPATH}/src/${GOMOBILE_PKG})

echo:
	@echo ${GOMOBILE_PKG}
	@echo ${APP_PATH}

bind-android:
	gomobile bind -target android -o ${APP_PATH}/android/go/go.aar \
		${GOMOBILE_PKG} \
		github.com/empirefox/flutter_dial_go/go/formobile

bind-ios:
	gomobile bind -target ios -o ${APP_PATH}/ios/Frameworks/Gomobile.framework \
		${GOMOBILE_PKG} \
		github.com/empirefox/flutter_dial_go/go/formobile

protoc:
	cd ${makefile_dir} && \
		protoc -I ./protos --go_out=plugins=grpc:./protos ./protos/helloworld.proto

	cd ${makefile_dir} && \
		protoc --dart_out=grpc:../../example/lib/src/generated -Iprotos protos/helloworld.proto

clean:
	rm -f ${APP_PATH}/android/go/*.aar
	rm -f ${APP_PATH}/android/go/*.jar
	rm -rf ${APP_PATH}/ios/Frameworks/Gomobile.framework
