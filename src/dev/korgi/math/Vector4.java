package dev.korgi.math;

/**
 * Represents a 4d vector
 * 
 * @author Arvan
 */
public class Vector4 {
    public double x;
    public double y;
    public double z;
    public double w;

    public Vector4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4() {
    }

    public Vector4(Vector3 v) {
        this(v.x, v.y, v.z, 1);
    }

    public Vector4(Vector3 v, double w) {
        this(v.z, v.y, v.z, w);
    }

    public Vector3 toVector3() {
        return new Vector3(x, y, z);
    }

    public static Vector4 of(int color) {

        int r = color & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 16) & 0xFF;
        int a = (color >> 24) & 0xFF;

        return new Vector4(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public boolean equals(Vector4 other) {
        return Math.abs(this.x - other.x) < 1e-5 && Math.abs(this.y - other.y) < 1e-5
                && Math.abs(this.z - other.z) < 1e-5 && Math.abs(this.w - other.w) < 1e-5;
    }

}
