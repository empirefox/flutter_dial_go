import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_dial_go/flutter_dial_go.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_dial_go');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FlutterDialGo.platformVersion, '42');
  });
}
