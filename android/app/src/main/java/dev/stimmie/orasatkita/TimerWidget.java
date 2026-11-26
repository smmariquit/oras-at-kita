package dev.stimmie.orasatkita;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

public class TimerWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) { refresh(c); }

    static void refresh(Context c) {
        Task on = Timer.task(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.timer_widget);
        PendingIntent open = Notify.open(c);
        v.setOnClickPendingIntent(R.id.head, open);
        v.setTextViewText(R.id.title, on == null ? "Ano ang ginagawa mo?" : on.label);
        v.setViewVisibility(R.id.clock, on == null ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.stop, on == null ? View.GONE : View.VISIBLE);
        v.setOnClickPendingIntent(R.id.stop, ActionReceiver.intent(c, ActionReceiver.STOP));
        // The clock counts from boot time, and Android draws it.
        long base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - Timer.startedAt(c));
        v.setChronometer(R.id.clock, base, null, on != null);
        for (Task t : Task.values()) {
            if (t.view == 0) continue;
            v.setInt(t.view, "setBackgroundResource", t == on ? R.drawable.tile_on : R.drawable.tile);
            v.setOnClickPendingIntent(t.view, Timer.consent(c) ? ActionReceiver.intent(c, ActionReceiver.TOGGLE + t.name()) : open);
        }
        AppWidgetManager.getInstance(c).updateAppWidget(new ComponentName(c, TimerWidget.class), v);
    }
}
