package at.lowdfx.lowdfx.util;

/**
 * Ein generisches Tupel, das zwei zusammengehörige Werte speichert.
 *
 * @param <L> Der Typ des linken/ersten Wertes
 * @param <R> Der Typ des rechten/zweiten Wertes
 */
public record Pair<L, R>(L left, R right) {

    /**
     * Erstellt ein neues Pair.
     *
     * @param left  Der linke/erste Wert
     * @param right Der rechte/zweite Wert
     * @return Ein neues Pair
     */
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    /**
     * Gibt den ersten (linken) Wert zurück.
     */
    public L first() {
        return left;
    }

    /**
     * Gibt den zweiten (rechten) Wert zurück.
     */
    public R second() {
        return right;
    }
}
