import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http2/transport.dart';
import 'package:flutter_dial_go/flutter_dial_go.dart';

const channelId = 'flutter_dial_go';
const notificationId = 1;
const importance = AndroidNotificationChannel.IMPORTANCE_HIGH;
const httpPort = 9998;
const http2Port = 9999;

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Conn _conn;
  ClientTransportConnection _transport;
  String _prefix = 'http2 echo response:';
  String _result = '...';
  int _id = 0;
  bool _requesting = false;

  // do http2 request
  Future _request() async {
    _requesting = true;
    var id = _id++;
    var headers = [
      Header.ascii(':method', 'POST'),
      Header.ascii(':path', '/echo'),
      Header.ascii(':scheme', 'http'),
      Header.ascii(':authority', 'go:$http2Port'),
    ];
    var stream = _transport.makeRequest(headers, endStream: false);
    print('POST /echo\n');
    stream.sendData(utf8.encode('$_prefix $id'), endStream: true);
    await for (var message in stream.incomingMessages) {
      if (message is HeadersStreamMessage) {
        for (var header in message.headers) {
          var name = utf8.decode(header.name);
          var value = utf8.decode(header.value);
          print('$name: $value');
        }
      } else if (message is DataStreamMessage) {
        // Use [message.bytes] (but respect 'content-encoding' header)
        setState(() {
          _result = utf8.decode(message.bytes);
        });
      }
    }
    _requesting = false;
  }

  @override
  void initState() {
    super.initState();
    initHttp2();
  }

  @override
  void dispose() {
    print('transport.terminate');
    _transport.terminate().then((_) => _conn.close());
    super.dispose();
  }

  Future initHttp2() async {
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
            }));
    c.done.then((_) {
      print('init GET Conn auto closed');
      c.close();
    });
    c.add(utf8.encode('GET / HTTP/1.0\r\n\r\n'));

    // init http2 transport
    _conn = await Conn.dial(http2Port);
    _conn.done.then((_) {
      print('http2 conn closed');
      _conn.close();
    });
    _transport =
        ClientTransportConnection.viaStreams(_conn.receiveStream, _conn);
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
          onPressed: _transport == null || _requesting
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
