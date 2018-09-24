# flutter_dial_go

A flutter plugin for connecting to golang embeded servers via platform channel.

## Getting Started

### Install for golang

```bash
go get -u github.com/empirefox/flutter_dial_go
cd $GOPATH/github.com/empirefox/flutter_dial_go/go
make android
```

### Install for flutter project

Add to `pubsepc.yml`, replace `$GOPATH` with real path.

```yaml
dependencies:
  flutter_dial_go:
    path: $GOPATH/github.com/empirefox/flutter_dial_go
```

### Develop golang side

When write go code:
Only use `github.com/empirefox/flutter_dial_go/go/forgo`.
Do not use `github.com/empirefox/flutter_dial_go/go/formobile`.

```go
import "github.com/empirefox/flutter_dial_go/go/forgo"

  // dart usage: var conn = await Conn.dial(9999)
  listener, err := forgo.Listen(9999)
```

- Implement `gomobile` package exactly like: [Go Example](go/example/gomobile/mobile.go).
- Copy [Makefile](go/Makefile) to flutter project, then replace the `example` path.
- Build with the new `Makefile`.
- For Android Studio: import `go.aar` and add `api project(':go')` to `$PROJECT_DIR$/android/app/build.gradle`.
- For Xcode: add `${PODS_ROOT}/../Frameworks` to all Framework search paths.

### Develop flutter side

Init with no service.

```dart
import 'package:flutter_dial_go/flutter_dial_go.dart';

Future initGo() async {
    await Conn.startGo();
}
```

Or init with foreground service. For ios, service will not start and it will work like above.

```dart
import 'package:flutter_dial_go/flutter_dial_go.dart';

Future initGo() async {
    await Conn.notificationChannel(
      channelId: channelId,
      importance: importance,
      name: 'fdg running',
      description: 'keep fdg running',
    );
    await Conn.startGoWithService(
      channelId: channelId,
      notificationId: notificationId,
      title: 'flutter_dial_go example',
      text: 'make flutter dial go',
    );
}
```

Then dial:

```dart
    // raw http request
    // golang: forgo.Listen(9998)
    Conn c = await Conn.dial(9998);
    print('GET /\n');
    c.receiveStream
        .fold(BytesBuilder(), (BytesBuilder a, List<int> b) => a..add(b))
        .then((a) => setState(() {
              _result = 'GET / HTTP/1.0\n\n' + utf8.decode(a.takeBytes());
            }))
        .catchError((e) => setState(() {
              _result = 'Caught http error: $e';
            }))
        .then((_) => c.close());
    c.add(utf8.encode('GET / HTTP/1.0\r\n\r\n'));
```

Or http2:

```dart
    // golang: forgo.Listen(9997)
    Conn c = await Conn.dial(9997);
    var transport = ClientTransportConnection.viaStreams(c.receiveStream, c);
```

Or grpc:

```dart
  Future<Http2Streams> _connect(String host, int port) async {
    // ignore: close_sinks
    final conn = await Conn.dial(port);
    return Http2Streams(conn.receiveStream, conn, conn.done);
  }

    var channel = ClientChannel(
      'go',
      port: 9999,
      options: ChannelOptions(
        credentials: ChannelCredentials.insecure(),
        http2: Http2Options(connect: connect),
      ),
    );
    var stub = GreeterClient(_channel);

    ...

    channel.terminate();
```

[Flutter Example](example/lib/main.dart).

## Flutter

For help getting started with Flutter, view our online
[documentation](https://flutter.io/).

For help on editing plugin code, view the [documentation](https://flutter.io/developing-packages/#edit-plugin-package).
