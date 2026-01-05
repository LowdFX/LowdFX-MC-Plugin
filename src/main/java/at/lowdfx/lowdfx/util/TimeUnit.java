package at.lowdfx.lowdfx.util;

/**
 * Zeiteinheiten mit Umrechnungsfaktoren zu Sekunden.
 */
public enum TimeUnit {
    SECONDS(1, "s", "Sekunde", "Sekunden"),
    MINUTES(60, "min", "Minute", "Minuten"),
    HOURS(3600, "h", "Stunde", "Stunden"),
    DAYS(86400, "d", "Tag", "Tage"),
    WEEKS(604800, "w", "Woche", "Wochen"),
    MONTHS(2592000, "mo", "Monat", "Monate"),
    YEARS(31536000, "y", "Jahr", "Jahre");

    private final long seconds;
    private final String abbreviation;
    private final String singular;
    private final String plural;

    TimeUnit(long seconds, String abbreviation, String singular, String plural) {
        this.seconds = seconds;
        this.abbreviation = abbreviation;
        this.singular = singular;
        this.plural = plural;
    }

    public long getSeconds() {
        return seconds;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getSingular() {
        return singular;
    }

    public String getPlural() {
        return plural;
    }

    public String format(long amount) {
        return amount + abbreviation;
    }

    /**
     * Findet die passende TimeUnit anhand der Abkürzung.
     */
    public static TimeUnit fromAbbreviation(String abbr) {
        String lower = abbr.toLowerCase();
        for (TimeUnit unit : values()) {
            if (unit.abbreviation.equals(lower)) {
                return unit;
            }
        }
        // Zusätzliche Aliase
        return switch (lower) {
            case "sec", "sek", "second", "seconds", "sekunde", "sekunden" -> SECONDS;
            case "m", "minute", "minutes", "minuten" -> MINUTES;
            case "hr", "hour", "hours", "stunde", "stunden" -> HOURS;
            case "day", "days", "tag", "tage" -> DAYS;
            case "wk", "week", "weeks", "woche", "wochen" -> WEEKS;
            case "month", "months", "monat", "monate" -> MONTHS;
            case "yr", "year", "years", "jahr", "jahre" -> YEARS;
            default -> null;
        };
    }
}
