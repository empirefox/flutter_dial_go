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
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  private final MethodChannel controller;
  private final Registrar registrar;
  private final Map<Long, Pipe> pipes = new HashMap<>();

  private Dialer dialer = null;
  private Result bindResult = null;
  private Result unbindResult = null;
  private boolean serviceMode = false;

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

      case "noService":
        if (dialer != null || serviceMode) {
          result.error("onMethodCall", "service mode or dialer already exist", null);
          return;
        }
        dialer = new Dialer(Looper.getMainLooper());
        result.success(null);
        return;

      case "startService":
        if (dialer != null || serviceMode) {
          result.error("onMethodCall", "dialer already exist", null);
          return;
        }
        serviceMode = true;
        onMethodStartService(call, result);
        return;

      case "stopService":
        if (!serviceMode) {
          dialer = null;
          result.success(null);
          return;
        }
        if (dialer == null) {
          result.success(null);
          return;
        }
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
    dialer.doInit((e) -> {
      if (e != null) {
        result.error("initGo", e.toString(), null);
      } else {
        result.success(null);
      }
    });
  }

  private void onMethodDestroyGo(MethodCall call, Result result) {
    dialer.doDestroy((e) -> {
      if (e != null) {
        result.error("destroyGo", e.toString(), null);
      } else {
        result.success(null);
      }
    });
  }

  private void onMethodDial(MethodCall call, Result result) {
    dialer.getDialHandler().post(() -> {
      List<Object> args = call.arguments();
      int port = (int) args.get(0);
      long id = ((Number) args.get(1)).longValue();
      long timeoutnano = ((Number) args.get(2)).longValue();
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
    serviceMode = false;
  }

  @Override
  public boolean onViewDestroy(FlutterNativeView flutterNativeView) {
    if (dialer != null && serviceMode) {
      registrar.activity().unbindService(this);
    }
    for (Pipe pipe : pipes.values()) {
      pipe.stop();
    }
    pipes.clear();
    return true;
  }
}

