package dev.stimmie.orasatkita;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Taps on the widget and the notification, plus the alarms. */
public class ActionReceiver extends BroadcastReceiver {
    static final String STOP = "stop", LONG = "long", REMIND = "remind", TOGGLE = "toggle-";

    static PendingIntent intent(Context c, String action) {
        Intent i = new Intent(c, ActionReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = String.valueOf(i.getAction());
        if (a.equals(STOP)) Timer.stop(c);
        else if (a.equals(LONG)) Notify.longRun(c);
        else if (a.equals(REMIND)) { Notify.reminder(c); Alarms.daily(c); }
        else if (a.startsWith(TOGGLE)) {
            Task t = Task.find(a.substring(TOGGLE.length()));
            if (t != null) Timer.toggle(c, t);
        }
        MainActivity.timerChanged();
    }
}
