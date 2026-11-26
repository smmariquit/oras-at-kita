package dev.stimmie.orasatkita;

/** The tasks, with the same ids as the web page. Bukid at benta has no widget button. */
enum Task {
    bahay("Gawaing bahay", R.id.t_bahay),
    alaga("Anak at matanda", R.id.t_alaga),
    babuyan("Pagbababoy", R.id.t_babuyan),
    kapehan("Kape", R.id.t_kapehan),
    iba("Bukid at benta", 0),
    samahan("Samahan", R.id.t_samahan),
    pahinga("Pahinga", R.id.t_pahinga);

    final String label;
    final int view;

    Task(String label, int view) { this.label = label; this.view = view; }

    /** Minutes before the app asks if the task is still going. Same limits as isLong in index.html. */
    int longMins() { return this == pahinga ? 600 : 240; }

    static Task find(String id) {
        for (Task t : values()) if (t.name().equals(id)) return t;
        return null;
    }
}
