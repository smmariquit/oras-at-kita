package dev.stimmie.orasatkita;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

final class Notify {
    private static final int TIMER = 1, LONG = 2, REMIND = 3;

    private Notify() {}

    private static NotificationManager manager(Context c) {
        NotificationManager m = c.getSystemService(NotificationManager.class);
        m.createNotificationChannel(new NotificationChannel("timer", "Running timer", NotificationManager.IMPORTANCE_LOW));
        m.createNotificationChannel(new NotificationChannel("reminder", "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT));
        return m;
    }

    private static Notification.Builder base(Context c, String channel) {
        return new Notification.Builder(c, channel).setSmallIcon(R.drawable.ic_clock).setColor(c.getColor(R.color.kita)).setContentIntent(open(c));
    }

    static PendingIntent open(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    }

    private static Notification.Action stopAction(Context c) {
        return new Notification.Action.Builder(null, "Tapos na", ActionReceiver.intent(c, ActionReceiver.STOP)).build();
    }

    /** Android draws the counting clock, so nothing in the app runs while it counts. */
    static void timer(Context c) {
        Task t = Timer.task(c);
        NotificationManager m = manager(c);
        m.cancel(LONG);
        if (t == null) { m.cancel(TIMER); return; }
        m.notify(TIMER, base(c, "timer").setContentTitle(t.label).setContentText("Timer running in background (battery-safe).")
                .setUsesChronometer(true).setShowWhen(true).setWhen(Timer.startedAt(c)).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(stopAction(c)).build());
    }

    static void longRun(Context c) {
        Task t = Timer.task(c);
        if (t == null) return;
        manager(c).notify(LONG, base(c, "reminder").setContentTitle(t.label + " pa rin ba?")
                .setContentText("Pindutin ang Tapos na kung tapos ka na.").setAutoCancel(true).addAction(stopAction(c)).build());
    }

    static void reminder(Context c) {
        manager(c).notify(REMIND, base(c, "reminder").setContentTitle("Na-log mo na ba ang oras mo today?")
                .setContentText("Quick check-in para i-log ang mga gawain mo.").setAutoCancel(true).build());
    }
}
