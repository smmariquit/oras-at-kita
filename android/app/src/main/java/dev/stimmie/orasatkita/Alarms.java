package dev.stimmie.orasatkita;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import java.util.Calendar;

final class Alarms {
    private Alarms() {}

    private static void at(Context c, String action, long when) {
        AlarmManager a = c.getSystemService(AlarmManager.class);
        PendingIntent p = ActionReceiver.intent(c, action);
        if (when == 0) a.cancel(p);
        else if (Build.VERSION.SDK_INT < 31 || a.canScheduleExactAlarms()) a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p);
        else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p);
    }

    /** Asks once if the running task is still going. */
    static void longRun(Context c) {
        Task t = Timer.task(c);
        long when = t == null ? 0 : Timer.startedAt(c) + t.longMins() * 60000L;
        at(c, ActionReceiver.LONG, when > System.currentTimeMillis() ? when : 0);
    }

    /** Sets the next daily reminder. An empty time turns it off. */
    static void daily(Context c) {
        String time = Timer.prefs(c).getString("remind", "");
        if (time.isEmpty()) { at(c, ActionReceiver.REMIND, 0); return; }
        Calendar d = Calendar.getInstance();
        d.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
        d.set(Calendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
        d.set(Calendar.SECOND, 0);
        if (d.getTimeInMillis() <= System.currentTimeMillis()) d.add(Calendar.DATE, 1);
        at(c, ActionReceiver.REMIND, d.getTimeInMillis());
    }
}
