import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:grpc/grpc.dart';
import 'package:flutter_dial_go/flutter_dial_go.dart';

import './generated/helloworld.pb.dart';
import './generated/helloworld.pbgrpc.dart';

const channelId = 'flutter_dial_go';
const notificationId = 1;
const importance = AndroidNotificationChannel.IMPORTANCE_HIGH;
const httpPort = 9998;
const grpcPort = 9999;

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  ClientChannel _channel;
  GreeterClient _stub;
  final String _name = 'world';
  String _result = '...';
  bool _requesting = false;

  // do grpc request
  Future _request() async {
    setState(() {
      _requesting = true;
    });
    try {
      final response = await _stub.sayHello(HelloRequest()..name = _name);
      setState(() {
        _result = 'Greeter client received: ${response.message}';
        _requesting = false;
      });
    } catch (e) {
      setState(() {
        _result = 'Caught error: $e';
        _requesting = false;
      });
    }
  }

  @override
  void initState() {
    super.initState();
    initGrpc();
  }

  @override
  void dispose() {
    print('channel.terminate');
    _channel.terminate();
    super.dispose();
  }

  Future<Http2Streams> _connect(String host, int port) async {
    // ignore: close_sinks
    final conn = await Conn.dial(port);
    return Http2Streams(conn.receiveStream, conn, conn.done);
  }

  Future initGrpc() async {
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

    // http request
    Conn c = await Conn.dial(httpPort);
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

    // init grpc channel
    _channel = ClientChannel(
      'go',
      port: grpcPort,
      options: ChannelOptions(
        credentials: ChannelCredentials.insecure(),
        http2: Http2Options(connect: _connect),
      ),
    );
    _stub = GreeterClient(_channel);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('$_result\n'),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: _channel == null || _requesting
              ? null
              : () {
                  _request();
                },
          tooltip: 'send request',
          child: Icon(Icons.send),
        ),
      ),
    );
  }
}
