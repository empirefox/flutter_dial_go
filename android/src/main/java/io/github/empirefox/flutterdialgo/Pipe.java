package io.github.empirefox.flutterdialgo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.nio.ByteBuffer;

import formobile.Conn;
import formobile.WriteReturn;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.BinaryMessenger.BinaryReply;
import io.flutter.plugin.common.MethodChannel;

import static formobile.Formobile.ErrNone;

public class Pipe extends Handler {
  private static final String streamPrefix = "fdgs#";

  private static final int WRITE = 0;
  private static final int CLOSE = 1;

  private final BinaryMessenger messenger;
  private final MethodChannel controller;
  private final Conn conn;
  private final long id;
  private final String streamName;

  private boolean closed = false;

  public Pipe(BinaryMessenger messenger, MethodChannel controller, Conn conn, long id) {
    this(messenger, controller, conn, id, Looper.myLooper());
  }

  public Pipe(BinaryMessenger messenger, MethodChannel controller, Conn conn, long id, Looper looper) {
    super(looper);
    this.messenger = messenger;
    this.controller = controller;
    this.conn = conn;
    this.id = id;
    this.streamName = streamPrefix + String.valueOf(id);
  }

  public void start() {
    messenger.setMessageHandler(streamName, (ByteBuffer message, final BinaryReply reply) -> {
      WriteReturn r = conn.write(message.array());
      byte err = r.getErr();
      if (err != ErrNone) {
        closeWithErr(err);
      }
    });

    new Thread(() -> {
      byte err = ErrNone;
      while (err == ErrNone) {
        err = conn.read((byte[] b, int n) -> {
          if (n > 0) {
            sendMessage(obtainMessage(WRITE, ByteBuffer.allocateDirect(n).put(b)));
          }
        });
      }
      sendMessage(obtainMessage(CLOSE, err));
    }).start();
  }

  @Override
  public void handleMessage(Message msg) {
    switch (msg.what) {
      case WRITE:
        messenger.send(streamName, (ByteBuffer) msg.obj);
        break;
      case CLOSE:
        closeWithErr((byte) msg.obj);
        break;
    }
  }

  public void stop() {
    closeWithErr(ErrNone);
  }

  private void closeWithErr(byte err) {
    if (closed) {
      return;
    }
    closed = true;

    messenger.setMessageHandler(streamName, null);
    controller.invokeMethod("close", new long[]{id, Byte.valueOf(err).longValue()});
    conn.close();
  }
}
