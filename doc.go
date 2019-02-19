// flutter_dial_go is a flutter plugin for connecting to golang embeded
// servers via platform channel.
//
// For golang side, import "github.com/empirefox/flutter_dial_go/go/forgo".
// A "gomobile" package is expected, and all public apis must be exactly the
// same with `go/example/gomobile/mobile.go`.
//
// Created by: 
//  `flutter create --template=plugin -i swift --org io.github.empirefox --description "A flutter plugin for connecting to golang embeded servers via platform channel." flutter_dial_go`
package flutter_dial_go

import (
	_ "github.com/empirefox/flutter_dial_go/go/forgo"
	_ "github.com/empirefox/flutter_dial_go/go/formobile"
)
