package io.github.empirefox.flutter_dial_go;

import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import formobile.Conn;
import formobile.WriteReturn;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.BinaryMessenger.BinaryReply;
import io.flutter.plugin.common.MethodChannel;

import static formobile.Formobile.ErrNone;

public class Pipe extends Handler {
  private static final String streamPrefix = "fdgs#";

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

    conn.startRead((byte[] b, int n, byte err) -> {
      ByteBuffer data = n > 0 ? ByteBuffer.allocateDirect(n).put(b) : null;
      post(() -> {
        if (n > 0) {
          messenger.send(streamName, data);
        }
        if (err != ErrNone) {
          closeWithErr(err);
        }
      });
    });
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
    ArrayList<Object> args = new ArrayList<>();
    args.add(id);
    args.add(Byte.valueOf(err).intValue());
    controller.invokeMethod("close", args);
    conn.close();
  }
}

