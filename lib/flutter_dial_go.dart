import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

// Conn is StreamSink<List<int>>. Conn.receiveStream is Stream<List<int>>.
class Conn implements StreamSink<List<int>> {
  static Map<int, StreamController<List<int>>> _localStreams = {};

  static MethodChannel _initController() {
    MethodChannel controller = MethodChannel('flutter_dial_go');
    controller.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'close':
          {
            List args = call.arguments;
            var id = args[0] as int;
            var err = _GoConnErr.values[args[1] as int];
            var stream = _localStreams[id];
            if (stream != null) {
              switch (err) {
                case _GoConnErr.ErrClosedPipe:
                  stream.addError('Go Closed Pipe');
                  break;
                case _GoConnErr.ErrTimeout:
                  stream.addError('Go Timeout');
                  break;
                default:
                // not a real error
              }
              stream.close();
            }
            return null;
          }
      }
      return null;
    });
    return controller;
  }

  static MethodChannel _controller = _initController();

  static Future notificationChannel({
    String channelId,
    int importance,
    String name,
    String description,
  }) async {
    if (Platform.isIOS) return null;
    var args = <dynamic>[channelId, importance, name, description];
    return await _controller.invokeMethod('notificationChannel', args);
  }

  static Future startGo() async {
    if (Platform.isAndroid) {
      await _controller.invokeMethod('noService');
    }
    return await _controller.invokeMethod('initGo');
  }

  static Future startGoWithService({
    String channelId,
    int notificationId,
    String title,
    String text,
  }) async {
    if (Platform.isAndroid) {
      var args = <dynamic>[channelId, notificationId, title, text];
      await _controller.invokeMethod('startService', args);
    }
    return await _controller.invokeMethod('initGo');
  }

  static Future stopGo() async {
    await _controller.invokeMethod('destroyGo').catchError((err) {
      FlutterError.reportError(FlutterErrorDetails(
        exception: err,
        library: 'flutter_dial_go library',
        context: 'while destroyGo',
      ));
    });
    if (Platform.isAndroid) await _controller.invokeMethod('stopService');
  }

  static const String _streamPrefix = 'fdgs#';
  static int _id = 0;
  final int id;

  StreamController<List<int>> _localStream;
  final String _streamName;
  final Completer<bool> _sinkDone = Completer();
  bool _sinkClosed = false;

  static Future<Conn> dial(int port) async {
    var id = _id++;
    var timeoutnano = 0;
    var args = <dynamic>[port, id, timeoutnano];
    await _controller.invokeMethod('dial', args);
    return Conn._private(id);
  }

  Conn._private(this.id) : _streamName = _streamPrefix + id.toString();

  Stream<List<int>> get receiveStream {
    if (_localStream == null) {
      _localStream = StreamController<List<int>>(
        onListen: () async {
          _localStream.done.then((_) => close());
          BinaryMessages.setMessageHandler(_streamName, (ByteData reply) async {
            if (_sinkClosed || reply == null) {
              _localStream.close();
              return null;
            }

            try {
              _localStream.add(reply.buffer.asUint8List());
            } on PlatformException catch (e) {
              _localStream.addError(e);
            }
            return null;
          });
        },
        onCancel: () => close(),
      );
      _localStreams[id] = _localStream;
    }
    return _localStream.stream;
  }

  @override
  void add(List<int> event) {
    var data = ByteData.view(Uint8List.fromList(event).buffer);
    BinaryMessages.send(_streamName, data);
  }

  @override
  void addError(Object err, [StackTrace stack]) {
    FlutterError.reportError(FlutterErrorDetails(
      exception: err,
      stack: stack,
      library: 'flutter_dial_go library',
      context: 'while de-activating platform stream on channel $_streamName',
    ));
  }

  @override
  Future addStream(Stream<List<int>> stream) {
    return stream
        .listen(
          (data) => add(data),
          onDone: () => close(),
          onError: (Object err, [StackTrace stack]) => addError(err, stack),
          cancelOnError: false,
        )
        .asFuture();
  }

  @override
  Future close() async {
    if (_sinkClosed) return await done;
    _sinkClosed = true;

    BinaryMessages.setMessageHandler(_streamName, null);
    await _controller.invokeMethod('close', id);

    _sinkDone.complete(true);
  }

  @override
  Future get done => _sinkDone.future;
}

class AndroidNotificationChannel {
  static const int IMPORTANCE_UNSPECIFIED = 0xfffffc18;
  static const int IMPORTANCE_NONE = 0x00000000;
  static const int IMPORTANCE_MIN = 0x00000001;
  static const int IMPORTANCE_LOW = 0x00000002;
  static const int IMPORTANCE_DEFAULT = 0x00000003;
  static const int IMPORTANCE_HIGH = 0x00000004;
}

enum _GoConnErr {
  ErrNone,
  EOF,
  ErrClosedPipe,
  ErrTimeout,
}
