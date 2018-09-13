package io.github.empirefox.flutterdialgo;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import formobile.Conn;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ViewDestroyListener;
import io.flutter.view.FlutterNativeView;

/**
 * FlutterDialGoPlugin
 */
public class FlutterDialGoPlugin extends Handler implements MethodCallHandler, ServiceConnection, ViewDestroyListener {
  private static final String TAG = "FlutterDialGoPlugin#";
  private final MethodChannel controller;
  private final Registrar registrar;
  private final Map<Long, Pipe> pipes = new HashMap<>();

  private Dialer dialer;
  private Result bindResult = null;
  private Result unbindResult = null;

  public FlutterDialGoPlugin(Registrar registrar, MethodChannel controller) {
    this.registrar = registrar;
    this.controller = controller;
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel controller = new MethodChannel(registrar.messenger(), "flutter_dial_go");
    final FlutterDialGoPlugin instance = new FlutterDialGoPlugin(registrar, controller);
    controller.setMethodCallHandler(instance);
    registrar.addViewDestroyListener(instance);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "notificationChannel":
        onMethodCreateNotificationChannel(call, result);
        return;

      case "startService":
        onMethodStartService(call, result);
        return;

      case "stopService":
        onMethodStopService(call, result);
        return;
    }

    if (dialer == null) {
      result.error(call.method, "service stopped", null);
      return;
    }

    switch (call.method) {
      case "initGo":
        onMethodInitGo(call, result);
        return;

      case "destroyGo":
        onMethodDestroyGo(call, result);
        return;
    }

    if (dialer.isNotInitialized()) {
      result.error(call.method, "go not initialized", null);
      return;
    }

    switch (call.method) {
      // dial Conn
      case "dial":
        onMethodDial(call, result);
        return;

      // close Conn
      case "close":
        onMethodClose(call, result);
        return;
    }

    result.notImplemented();
  }

  private void onMethodCreateNotificationChannel(MethodCall call, Result result) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ArrayList<Object> args = call.arguments();
      String channelId = (String) args.get(0);
      int importance = (int) args.get(1);
      CharSequence name = (String) args.get(2);
      String description = (String) args.get(3);

      NotificationChannel channel = new NotificationChannel(channelId, name, importance);
      channel.setDescription(description);
      // Register the channel with the system; you can't change the importance
      // or other notification behaviors after this
      NotificationManager notificationManager = registrar.context().getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
    result.success(null);
  }

  private void onMethodStartService(MethodCall call, Result result) {
    ArrayList<Object> args = call.arguments();
    SimpleForegroundNotification sfn = new SimpleForegroundNotification(args);
    Activity activity = registrar.activity();
    Intent intent = DialService.createIntent(activity, sfn);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      activity.startForegroundService(intent);
    } else {
      activity.startService(intent);
    }
    bindResult = result;
    activity.bindService(intent, this, Context.BIND_AUTO_CREATE);
  }

  private void onMethodStopService(MethodCall call, Result result) {
    Activity activity = registrar.activity();
    Intent intent = DialService.createIntent(activity, null);
    unbindResult = result;
    activity.unbindService(this);
    activity.stopService(intent);
  }

  private void onMethodInitGo(MethodCall call, Result result) {
    dialer.init(new Dialer.Callback() {
      @Override
      public void success() {
        result.success(null);
      }

      @Override
      public void error(Exception e) {
        result.error("initGo", e.toString(), null);
      }
    });
  }

  private void onMethodDestroyGo(MethodCall call, Result result) {
    dialer.destroy(new Dialer.Callback() {
      @Override
      public void success() {
        result.success(null);
      }

      @Override
      public void error(Exception e) {
        result.error("destroyGo", e.toString(), null);
      }
    });
  }

  private void onMethodDial(MethodCall call, Result result) {
    dialer.getDialHandler().post(() -> {
      long[] args = call.arguments();
      int port = Long.valueOf(args[0]).intValue();
      long id = args[1];
      long timeoutnano = args[2];
      try {
        Conn conn = dialer.dial(port, id, timeoutnano);
        post(() -> {
          Pipe pipe = new Pipe(registrar.messenger(), controller, conn, id);
          pipe.start();
          pipes.put(id, pipe);
          result.success(null);
        });
      } catch (Exception e) {
        post(() -> result.error("dial failed", e.getMessage(), null));
      }
    });
  }

  private void onMethodClose(MethodCall call, Result result) {
    long id = ((Number) call.arguments()).longValue();
    Pipe pipe = pipes.get(id);
    if (pipe != null) {
      pipe.stop();
    }
    result.success(null);
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    dialer = DialService.getDialer(service);
    bindResult.success(null);
    bindResult = null;
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    dialer = null;
    unbindResult.success(null);
    unbindResult = null;
  }

  @Override
  public boolean onViewDestroy(FlutterNativeView flutterNativeView) {
    Log.i(TAG, "onViewDestroy");
    if (dialer != null) {
      registrar.activity().unbindService(this);
    }
    for (Pipe pipe : pipes.values()) {
      pipe.stop();
    }
    return true;
  }
}
