package io.github.empirefox.flutter_dial_go;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;


public class DialService extends Service {
  private static final String TAG = "DialService#";
  public static final String ACTION_START = "ACTION_START";
  public static final String ACTION_STOP = "ACTION_STOP";
  private static final String INTENT_EXTRA_KEY_SFN = "sfn";

  private Dialer dialer = null;
  private final IBinder binder = new DialerBinder();

  public static Intent createIntent(Context context, SimpleForegroundNotification sfn) {
    Intent intent = new Intent(context, DialService.class);
    if (sfn != null) {
      intent.setAction(ACTION_START).putExtra(INTENT_EXTRA_KEY_SFN, sfn);
    } else {
      intent.setAction(ACTION_STOP);
    }
    return intent;
  }

  public static Dialer getDialer(IBinder binder) {
    return ((DialerBinder) binder).getDialer();
  }

  private class DialerBinder extends Binder {
    Dialer getDialer() {
      return dialer;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent.getAction();
    switch (action) {
      case ACTION_START:
        SimpleForegroundNotification sfn = (SimpleForegroundNotification) intent.getSerializableExtra(INTENT_EXTRA_KEY_SFN);
        showForegroundNotification(sfn);
        break;

      case ACTION_STOP:
        stopForeground(true);
        stopSelf();
        break;
    }
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    dialer = new Dialer(Looper.getMainLooper());
  }

  @Override
  public void onDestroy() {
    dialer.stopDialThread();
    try {
      dialer.doDestroySync();
    } catch (Exception e) {
      Log.e(TAG, "Failed to destroy go resources", e);
    }
    super.onDestroy();
  }

  private void showForegroundNotification(SimpleForegroundNotification sfn) {
    // Create notification default intent.
    PackageManager pm = getApplicationContext().getPackageManager();
    Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    Notification notification;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notification = new Notification.Builder(this, sfn.channelId)
          .setWhen(System.currentTimeMillis())
          .setSmallIcon(android.R.drawable.ic_media_play)
          .setContentTitle(sfn.title)
          .setContentText(sfn.text)
          .setContentIntent(pendingIntent)
          .build();
    } else {
      notification = new NotificationCompat.Builder(this, sfn.channelId)
          .setWhen(System.currentTimeMillis())
          .setSmallIcon(android.R.drawable.ic_media_play)
          .setContentTitle(sfn.title)
          .setContentText(sfn.text)
          .setPriority(Notification.PRIORITY_MAX)
          .setContentIntent(pendingIntent)
          .build();
    }
    startForeground(sfn.notificationId, notification);
  }
}
