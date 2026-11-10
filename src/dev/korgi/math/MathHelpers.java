package dev.korgi.math;

/**
 * Holds helpful math functions
 * 
 * @author Aarush
 */
public class MathHelpers {
    public static double wrapAngle(double angle) {
        return wrap(angle, -Math.PI, Math.PI);
    }

    public static double wrap(double val, double min, double max) {
        double range = max - min;
        return min + ((val - min) % range + range) % range;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static Vector3 lerp(Vector3 a, Vector3 b, float t) {
        return new Vector3(
                lerp(a.x, b.x, t),
                lerp(a.y, b.y, t),
                lerp(a.z, b.z, t));
    }
}
