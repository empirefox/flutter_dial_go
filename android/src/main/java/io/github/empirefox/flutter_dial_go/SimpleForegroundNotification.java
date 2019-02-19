package io.github.empirefox.flutter_dial_go;

import java.io.Serializable;
import java.util.List;

public final class SimpleForegroundNotification implements Serializable {
  public final String channelId;
  public final int notificationId;
  public final CharSequence title;
  public final CharSequence text;

  public SimpleForegroundNotification(List<Object> args) {
    this.channelId = (String) args.get(0);
    this.notificationId = (int) args.get(1);
    this.title = (String) args.get(2);
    this.text = (String) args.get(3);
  }
}
