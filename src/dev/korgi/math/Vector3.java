package dev.korgi.math;

import java.util.function.BiFunction;

import dev.korgi.json.JSONIgnore;

/**
 * Represents a 3-element vector. Used to manage most of the 3d game
 * 
 * @author Arvan
 */
public class Vector3 {
    public double x;
    public double y;
    public double z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Vector3 subtract(Vector3 other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
        return this;
    }

    public Vector3 subtract(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3 add(Vector3 other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
        return this;
    }

    public Vector3 add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3 inverse() {
        this.x = -x;
        this.y = -y;
        this.z = -z;
        return this;
    }

    public Vector3 add(double val) {
        return add(val, val, val);
    }

    public Vector3 multiply(double factor) {
        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }

    public Vector3 multiply(Vector3 other) {
        this.x *= other.x;
        this.y *= other.y;
        this.z *= other.z;
        return this;
    }

    public Vector3 multiply(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3 divide(double factor) {
        this.x /= factor;
        this.y /= factor;
        this.z /= factor;
        return this;
    }

    public Vector3 divide(Vector3 other) {
        this.x /= other.x;
        this.y /= other.y;
        this.z /= other.z;
        return this;
    }

    public Vector3 divide(double x, double y, double z) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        return this;
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double dot(double x, double y, double z) {
        return this.x * x + this.y * y + this.z * z;
    }

    public Vector3 cross(Vector3 other) {
        double x = this.y * other.z - this.z * other.y;
        double y = this.z * other.x - this.x * other.z;
        double z = this.x * other.y - this.y * other.x;
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 cross(double x, double y, double z) {
        double xNew = this.y * z - this.z * y;
        double yNew = this.z * x - this.x * z;
        double zNew = this.x * y - this.y * x;
        this.x = xNew;
        this.y = yNew;
        this.z = zNew;
        return this;
    }

    public Vector3 normalize() {
        double len = magnitude();
        if (len == 0) {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }
        this.x /= len;
        this.y /= len;
        this.z /= len;
        return this;
    }

    public int[] toIntArray() {
        return new int[] {
                (int) x,
                (int) y,
                (int) z
        };
    }

    public float[] toFloatArray() {
        return new float[] {
                (float) x,
                (float) y,
                (float) z
        };
    }

    public double[] toArray() {
        return new double[] {
                x,
                y,
                z
        };
    }

    public double multiplyComp() {
        return x * y * z;
    }

    public Vector3 set(Vector3 other) {
        if (other != null) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
        }

        return this;
    }

    public Vector3 set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 wrap(Vector3 min, Vector3 max) {
        this.x = MathHelpers.wrap(x, min.x, max.x);
        this.y = MathHelpers.wrap(y, min.y, max.y);
        this.z = MathHelpers.wrap(z, min.z, max.z);
        return this;
    }

    public Vector3 wrap(double min, double max) {
        this.x = MathHelpers.wrap(x, min, max);
        this.y = MathHelpers.wrap(y, min, max);
        this.z = MathHelpers.wrap(z, min, max);
        return this;
    }

    public Vector3 modify(BiFunction<Double, Double, Double> modifer, Vector3 val) {
        this.x = modifer.apply(x, val.x);
        this.y = modifer.apply(y, val.y);
        this.z = modifer.apply(z, val.z);
        return this;
    }

    public Vector3 modify(BiFunction<Double, Double, Double> modifer, double val) {
        this.x = modifer.apply(x, val);
        this.y = modifer.apply(y, val);
        this.z = modifer.apply(z, val);
        return this;
    }

    public Vector3 copy() {
        return new Vector3(x, y, z);
    }

    public Vector3 truncate() {
        this.x = (int) x;
        this.y = (int) y;
        this.z = (int) z;
        return this;
    }

    public Vector3 floor() {
        x = Math.floor(x);
        y = Math.floor(y);
        z = Math.floor(z);
        return this;
    }

    public Vector3 abs() {
        x = Math.abs(x);
        y = Math.abs(y);
        z = Math.abs(z);
        return this;
    }

    public Vector3 copy(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    public Vector3 copy(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 sqrt() {
        this.x = Math.sqrt(x);
        this.y = Math.sqrt(y);
        this.z = Math.sqrt(z);
        return this;
    }

    public Vector3 maskX() {
        this.x = 0;
        return this;
    }

    public Vector3 maskY() {
        this.y = 0;
        return this;
    }

    public Vector3 maskZ() {
        this.z = 0;
        return this;
    }

    public double min() {
        return Math.min(Math.min(x, y), z);
    }

    public double max() {
        return Math.max(Math.max(x, y), z);
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @JSONIgnore
    private static final double epsilon = 1e-4;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector4 v) {
            return Math.abs(v.x - x) < epsilon && Math.abs(v.y - y) < epsilon && Math.abs(v.z - z) < epsilon;
        } else if (obj instanceof Vector3 v) {
            return Math.abs(v.x - x) < epsilon && Math.abs(v.y - y) < epsilon && Math.abs(v.z - z) < epsilon;
        }
        return false;
    }

    public double compare(double value, Vector4 results) {
        if (Math.abs(x - value) < epsilon) {
            return results.x;
        } else if (Math.abs(y - value) < epsilon) {
            return results.y;
        } else if (Math.abs(z - value) < epsilon) {
            return results.z;
        } else {
            return results.w;
        }
    }

    public <T> T compare(double value, T xResult, T yResult, T zResult, T defaultRes) {
        if (Math.abs(x - value) < epsilon) {
            return xResult;
        } else if (Math.abs(y - value) < epsilon) {
            return yResult;
        } else if (Math.abs(z - value) < epsilon) {
            return zResult;
        } else {
            return defaultRes;
        }
    }

    public long key() {
        return (((long) (((int) this.x) & 0xFFFFF)) << 40)
                | (((long) (((int) this.y) & 0xFFFFF)) << 20)
                | ((long) (((int) this.z) & 0xFFFFF));
    }

    public static long key(double x, double y, double z) {
        return (((long) (((int) x) & 0xFFFFF)) << 40)
                | (((long) (((int) y) & 0xFFFFF)) << 20)
                | ((long) (((int) z) & 0xFFFFF));
    }

    public static Vector3 min(Vector3 a, Vector3 b) {
        return new Vector3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    public static Vector3 max(Vector3 a, Vector3 b) {
        return new Vector3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    public static Vector3 random() {
        return new Vector3(Math.random(), Math.random(), Math.random());
    }

    @Override
    public String toString() {
        return "(%.3f, %.3f, %.3f)".formatted(x, y, z);
    }
}
