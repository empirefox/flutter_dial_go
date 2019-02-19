package io.github.empirefox.flutter_dial_go;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import formobile.Conn;
import formobile.Formobile;
import gomobile.ConcurrentRunner;
import gomobile.FromGo;
import gomobile.Gomobile;

public class Dialer extends Handler implements formobile.Dialer {
  private final formobile.Dialer dialer = Formobile.getDialer();
  private final HandlerThread dialThread;
  private final Handler dialHandler;
  private FromGo go = null;
  private ConcurrentRunner destroyRunner = null;

  public Dialer(Looper looper) {
    super(looper);
    dialThread = new HandlerThread("dial_thread");
    dialThread.start();
    dialHandler = new Handler(dialThread.getLooper());
  }

  public void doInit(Callback cb) {
    if (go == null) {
      go = Gomobile.newFromGo();
    }
    ConcurrentRunner runner = go.doInitOnce();
    dialHandler.post(() -> {
      try {
        runner.done();
        post(() -> cb.onResult(null));
      } catch (Exception e) {
        post(() -> cb.onResult(e));
      }
    });
  }

  public void doDestroy(Callback cb) {
    if (go == null) {
      cb.onResult(null);
      return;
    }

    FromGo go = this.go;
    this.go = null;
    destroyRunner = go.doDestroyOnce();

    dialHandler.post(() -> {
      try {
        destroyRunner.done();
        post(() -> {
          cb.onResult(null);
          destroyRunner = null;
        });
      } catch (Exception e) {
        post(() -> {
          cb.onResult(e);
          destroyRunner = null;
        });
      }
    });
  }

  public void doDestroySync() throws Exception {
    ConcurrentRunner runner = null;
    if (go != null) {
      runner = go.doDestroyOnce();
    }
    if (runner == null) {
      runner = destroyRunner;
      destroyRunner = null;
    }
    if (runner != null) {
      runner.done();
    }
    go = null;
  }

  public Handler getDialHandler() {
    return dialHandler;
  }

  public void stopDialThread() {
    dialThread.quit();
  }

  public boolean isNotInitialized() {
    return go == null;
  }

  @Override
  public Conn dial(int port, long channelId, long timeoutnano) throws Exception {
    return dialer.dial(port, channelId, timeoutnano);
  }

  public interface Callback {
    // fatal error if e is not null, bugs here!!!
    void onResult(Exception e);
  }
}
