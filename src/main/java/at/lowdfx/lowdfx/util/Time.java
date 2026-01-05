package at.lowdfx.lowdfx.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repräsentiert eine Zeitdauer mit Unterstützung für Parsing und Formatierung.
 */
public class Time {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*([a-zA-Z]+)");

    private long seconds;

    public Time(long seconds) {
        this.seconds = Math.max(0, seconds);
    }

    public Time(long amount, TimeUnit unit) {
        this.seconds = Math.max(0, amount * unit.getSeconds());
    }

    public Time(Time other) {
        this.seconds = other.seconds;
    }

    /**
     * Gibt die Zeit in Sekunden zurück.
     */
    public long get() {
        return seconds;
    }

    /**
     * Gibt die Zeit in der angegebenen Einheit zurück.
     */
    public long getAs(TimeUnit unit) {
        return seconds / unit.getSeconds();
    }

    /**
     * Gibt die Zeit in der angegebenen Einheit als Dezimalzahl zurück.
     */
    public double getAsExact(TimeUnit unit) {
        return (double) seconds / unit.getSeconds();
    }

    /**
     * Addiert Sekunden zur Zeit.
     */
    public void increment(long secs) {
        this.seconds = Math.max(0, this.seconds + secs);
    }

    /**
     * Addiert Zeit in der angegebenen Einheit.
     */
    public void increment(long amount, TimeUnit unit) {
        increment(amount * unit.getSeconds());
    }

    /**
     * Subtrahiert Sekunden von der Zeit.
     */
    public void decrement(long secs) {
        this.seconds = Math.max(0, this.seconds - secs);
    }

    /**
     * Subtrahiert Zeit in der angegebenen Einheit.
     */
    public void decrement(long amount, TimeUnit unit) {
        decrement(amount * unit.getSeconds());
    }

    /**
     * Formatiert die Zeit präzise (z.B. "1h 30min 45s").
     */
    public String getPreciselyFormatted() {
        return preciselyFormat(this.seconds);
    }

    /**
     * Formatiert die Zeit in der größten passenden Einheit (z.B. "2d").
     */
    public String getOneUnitFormatted() {
        return oneUnitFormat(this.seconds);
    }

    @Override
    public String toString() {
        return getPreciselyFormatted();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Time time = (Time) obj;
        return seconds == time.seconds;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(seconds);
    }

    // ==================== Statische Methoden ====================

    // Maximale Zeit: 100 Jahre in Sekunden (verhindert Overflow)
    public static final long MAX_SECONDS = 100L * 365 * 24 * 60 * 60; // ~3.15 Milliarden Sekunden

    /**
     * Parst einen Zeit-String wie "10m", "1h30m", "2d 5h".
     *
     * @param input Der zu parsende String
     * @return Ein Time-Objekt
     * @throws IllegalArgumentException wenn das Format ungültig ist oder der Wert zu groß ist
     */
    public static Time parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Zeit-String darf nicht leer sein");
        }

        String cleaned = input.trim().toLowerCase();
        Matcher matcher = TIME_PATTERN.matcher(cleaned);

        long totalSeconds = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Zahl ist zu groß: " + matcher.group(1));
            }

            String unitStr = matcher.group(2);

            TimeUnit unit = TimeUnit.fromAbbreviation(unitStr);
            if (unit == null) {
                throw new IllegalArgumentException("Unbekannte Zeiteinheit: " + unitStr);
            }

            // Overflow-Schutz: Prüfe vor der Multiplikation
            long unitSeconds = unit.getSeconds();
            if (amount > MAX_SECONDS / unitSeconds) {
                throw new IllegalArgumentException("Zeitwert ist zu groß: " + amount + unitStr);
            }

            long additionalSeconds = amount * unitSeconds;

            // Overflow-Schutz: Prüfe vor der Addition
            if (totalSeconds > MAX_SECONDS - additionalSeconds) {
                throw new IllegalArgumentException("Gesamtzeit überschreitet Maximum (100 Jahre)");
            }

            totalSeconds += additionalSeconds;
        }

        if (!found) {
            // Versuche als reine Zahl (Sekunden) zu parsen
            try {
                totalSeconds = Long.parseLong(cleaned);
                if (totalSeconds > MAX_SECONDS) {
                    throw new IllegalArgumentException("Zeitwert ist zu groß (Maximum: 100 Jahre)");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ungültiges Zeit-Format: " + input);
            }
        }

        return new Time(totalSeconds);
    }

    /**
     * Formatiert Sekunden präzise mit allen Einheiten.
     */
    public static String preciselyFormat(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();
        long remaining = totalSeconds;

        // Jahre
        if (remaining >= TimeUnit.YEARS.getSeconds()) {
            long years = remaining / TimeUnit.YEARS.getSeconds();
            remaining %= TimeUnit.YEARS.getSeconds();
            sb.append(years).append(TimeUnit.YEARS.getAbbreviation()).append(" ");
        }

        // Monate
        if (remaining >= TimeUnit.MONTHS.getSeconds()) {
            long months = remaining / TimeUnit.MONTHS.getSeconds();
            remaining %= TimeUnit.MONTHS.getSeconds();
            sb.append(months).append(TimeUnit.MONTHS.getAbbreviation()).append(" ");
        }

        // Wochen
        if (remaining >= TimeUnit.WEEKS.getSeconds()) {
            long weeks = remaining / TimeUnit.WEEKS.getSeconds();
            remaining %= TimeUnit.WEEKS.getSeconds();
            sb.append(weeks).append(TimeUnit.WEEKS.getAbbreviation()).append(" ");
        }

        // Tage
        if (remaining >= TimeUnit.DAYS.getSeconds()) {
            long days = remaining / TimeUnit.DAYS.getSeconds();
            remaining %= TimeUnit.DAYS.getSeconds();
            sb.append(days).append(TimeUnit.DAYS.getAbbreviation()).append(" ");
        }

        // Stunden
        if (remaining >= TimeUnit.HOURS.getSeconds()) {
            long hours = remaining / TimeUnit.HOURS.getSeconds();
            remaining %= TimeUnit.HOURS.getSeconds();
            sb.append(hours).append(TimeUnit.HOURS.getAbbreviation()).append(" ");
        }

        // Minuten
        if (remaining >= TimeUnit.MINUTES.getSeconds()) {
            long minutes = remaining / TimeUnit.MINUTES.getSeconds();
            remaining %= TimeUnit.MINUTES.getSeconds();
            sb.append(minutes).append(TimeUnit.MINUTES.getAbbreviation()).append(" ");
        }

        // Sekunden
        if (remaining > 0 || sb.isEmpty()) {
            sb.append(remaining).append(TimeUnit.SECONDS.getAbbreviation());
        }

        return sb.toString().trim();
    }

    /**
     * Formatiert Sekunden in der größten passenden Einheit.
     */
    public static String oneUnitFormat(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }

        if (totalSeconds >= TimeUnit.YEARS.getSeconds()) {
            return (totalSeconds / TimeUnit.YEARS.getSeconds()) + TimeUnit.YEARS.getAbbreviation();
        }
        if (totalSeconds >= TimeUnit.MONTHS.getSeconds()) {
            return (totalSeconds / TimeUnit.MONTHS.getSeconds()) + TimeUnit.MONTHS.getAbbreviation();
        }
        if (totalSeconds >= TimeUnit.WEEKS.getSeconds()) {
            return (totalSeconds / TimeUnit.WEEKS.getSeconds()) + TimeUnit.WEEKS.getAbbreviation();
        }
        if (totalSeconds >= TimeUnit.DAYS.getSeconds()) {
            return (totalSeconds / TimeUnit.DAYS.getSeconds()) + TimeUnit.DAYS.getAbbreviation();
        }
        if (totalSeconds >= TimeUnit.HOURS.getSeconds()) {
            return (totalSeconds / TimeUnit.HOURS.getSeconds()) + TimeUnit.HOURS.getAbbreviation();
        }
        if (totalSeconds >= TimeUnit.MINUTES.getSeconds()) {
            return (totalSeconds / TimeUnit.MINUTES.getSeconds()) + TimeUnit.MINUTES.getAbbreviation();
        }
        return totalSeconds + TimeUnit.SECONDS.getAbbreviation();
    }

    /**
     * Formatiert ein Time-Objekt präzise.
     */
    public static String preciselyFormat(Time time) {
        return preciselyFormat(time.get());
    }

    /**
     * Formatiert ein Time-Objekt in der größten passenden Einheit.
     */
    public static String oneUnitFormat(Time time) {
        return oneUnitFormat(time.get());
    }
}
