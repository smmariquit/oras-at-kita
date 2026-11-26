package dev.stimmie.orasatkita;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Puts the notification, the widget and the alarms back after a restart. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Timer.refresh(c);
        Alarms.daily(c);
    }
}
