package dev.stimmie.orasatkita;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** The running task and the finished ones the page hasn't picked up yet. */
final class Timer {
    private Timer() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("timer", Context.MODE_PRIVATE); }

    static Task task(Context c) { return Task.find(prefs(c).getString("cat", "")); }
    static long startedAt(Context c) { return prefs(c).getLong("start", 0); }
    static boolean consent(Context c) { return prefs(c).getBoolean("consent", false); }

    static synchronized void set(Context c, boolean consent, Task task, long start) {
        boolean same = consent(c) == consent && task(c) == task && startedAt(c) == start;
        prefs(c).edit().putBoolean("consent", consent).putString("cat", task == null ? "" : task.name())
                .putLong("start", task == null ? 0 : start).commit();
        if (!same) refresh(c);
    }

    /** Starts a task, or stops it if it's the one running. */
    static synchronized void toggle(Context c, Task t) {
        boolean wasOn = task(c) == t;
        stop(c);
        if (!wasOn) set(c, consent(c), t, System.currentTimeMillis());
    }

    static synchronized void stop(Context c) {
        Task t = task(c);
        if (t == null) return;
        long start = startedAt(c);
        long mins = Math.min(720, Math.round((System.currentTimeMillis() - start) / 60000.0));
        if (mins >= 1) {
            try {
                JSONArray done = new JSONArray(prefs(c).getString("done", "[]"));
                done.put(new JSONObject().put("id", UUID.randomUUID().toString()).put("cat", t.name()).put("start", start).put("mins", mins));
                prefs(c).edit().putString("done", done.toString()).commit();
            } catch (JSONException e) { throw new IllegalStateException(e); }
        }
        set(c, consent(c), null, 0);
    }

    static synchronized String pull(Context c) {
        try {
            Task t = task(c);
            return new JSONObject().put("cat", t == null ? "" : t.name()).put("start", startedAt(c))
                    .put("done", new JSONArray(prefs(c).getString("done", "[]"))).toString();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }

    /** Drops the first n finished tasks, once the page has saved them. */
    static synchronized void ack(Context c, int n) {
        try {
            JSONArray done = new JSONArray(prefs(c).getString("done", "[]")), left = new JSONArray();
            for (int i = n; i < done.length(); i++) left.put(done.get(i));
            prefs(c).edit().putString("done", left.toString()).commit();
        } catch (JSONException e) { throw new IllegalStateException(e); }
    }

    static void refresh(Context c) {
        Notify.timer(c);
        Alarms.longRun(c);
        TimerWidget.refresh(c);
    }
}
