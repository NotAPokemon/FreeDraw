package dev.korgi.math;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds common vectors as constants
 * 
 * @author Arvan
 */
public class VectorConstants {
  public static final Vector4 GREEN = new Vector4(0, 1, 0, 1);
  public static final Vector4 DARK_GREEN = new Vector4(0, 0.5, 0, 1);

  public static final Vector3 ONE = new Vector3(1, 1, 1);
  public static final Vector3 HALF = new Vector3(0.5, 0.5, 0.5);

  public static final Vector3 UP = new Vector3(0, 1, 0);
  public static final Vector3 DOWN = new Vector3(0, -1, 0);
  public static final Vector3 RIGHT = new Vector3(1, 0, 0);
  public static final Vector3 LEFT = new Vector3(-1, 0, 0);
  public static final Vector3 FORWARD = new Vector3(0, 0, 1);
  public static final Vector3 BACK = new Vector3(0, 0, -1);

  public static final Vector3 ZERO = new Vector3();

  private static final List<Vector3> ensure = new ArrayList<>();

  static {
    ensure.add(ONE.copy());
    ensure.add(HALF.copy());
    ensure.add(UP.copy());
    ensure.add(DOWN.copy());
    ensure.add(RIGHT.copy());
    ensure.add(LEFT.copy());
    ensure.add(FORWARD.copy());
    ensure.add(BACK.copy());
    ensure.add(ZERO.copy());
  }

  public static void ensure() {
    if (!ONE.equals(ensure.get(0))) {
      ONE.set(ensure.get(0));
    }

    if (!HALF.equals(ensure.get(1))) {
      HALF.set(ensure.get(1));
    }

    if (!UP.equals(ensure.get(2))) {
      UP.set(ensure.get(2));
    }

    if (!DOWN.equals(ensure.get(3))) {
      DOWN.set(ensure.get(3));
    }

    if (!RIGHT.equals(ensure.get(4))) {
      RIGHT.set(ensure.get(4));
    }

    if (!LEFT.equals(ensure.get(5))) {
      LEFT.set(ensure.get(5));
    }

    if (!FORWARD.equals(ensure.get(6))) {
      FORWARD.set(ensure.get(6));
    }

    if (!BACK.equals(ensure.get(7))) {
      BACK.set(ensure.get(7));
    }

    if (!ZERO.equals(ensure.get(8))) {
      ZERO.set(ensure.get(8));
    }
  }

}
