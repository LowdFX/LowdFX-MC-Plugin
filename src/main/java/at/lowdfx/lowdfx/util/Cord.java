package at.lowdfx.lowdfx.util;

/**
 * Repräsentiert 3D-Koordinaten (X, Y, Z).
 */
public record Cord(double x, double y, double z) {

    /**
     * Erstellt neue Koordinaten.
     */
    public static Cord of(double x, double y, double z) {
        return new Cord(x, y, z);
    }

    /**
     * Gibt die Block-X-Koordinate zurück.
     */
    public int blockX() {
        return (int) Math.floor(x);
    }

    /**
     * Gibt die Block-Y-Koordinate zurück.
     */
    public int blockY() {
        return (int) Math.floor(y);
    }

    /**
     * Gibt die Block-Z-Koordinate zurück.
     */
    public int blockZ() {
        return (int) Math.floor(z);
    }

    /**
     * Berechnet die Distanz zu einer anderen Koordinate.
     */
    public double distance(Cord other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Addiert einen Offset zu den Koordinaten.
     */
    public Cord add(double dx, double dy, double dz) {
        return new Cord(x + dx, y + dy, z + dz);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}
