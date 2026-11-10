package dev.korgi.ui;

import java.util.ArrayList;

import processing.core.PFont;

/**
 * Holds constants for the different GUIs
 * 
 * @author Arvan
 */
public class StyleConstants {

  public static final int BG_DARK = 0xFF0D0F14;
  public static final int BG_CARD = 0xFF1C2130;
  public static final int BG_INPUT = 0xFF161B26;
  public static final int ACCENT = 0xFF4F8EF7;
  public static final int ACCENT_DIM = 0xFF2A4F9A;
  public static final int TEXT_PRIMARY = 0xFFE8EAF0;
  public static final int TEXT_PRIMARY_HOVER = 0xFFB9D2FF;
  public static final int TEXT_DIM = 0xFF7A8099;
  public static final int TEXT_LABEL = 0xFF4F5A72;
  public static final int DANGER = 0xFFE05C5C;
  public static final int DANGER_DIM = 0xFF7A2A2A;
  public static final int SUCCESS_DIM = 0xFF2A7044;
  public static final int SUCCESS = 0xFF4FD17A;
  public static final int BORDER = 0xFF252B3B;
  public static final int RED = 0xFFFF0000;
  public static final int WARN = 0xFFE9D502;
  public static final int TEXT = 0xFFFFFFFF;
  public static final int GREEN_HUD = 0xFF1D9E75;
  public static final int RED_HUD = 0xFFE24B4A;
  public static final int AMBER_HUD = 0xFFEF9F27;
  public static final int BLACK = 0x000000;
  public static final int WHITE = 0x000000;

  private static final ArrayList<PFont> fonts = new ArrayList<>();

  public static int withOpacity(int color, double opacity) {
    opacity = Math.max(0.0, Math.min(1.0, opacity));

    int alpha = (int) (opacity * 255.0);

    return (color & 0x00FFFFFF) | (alpha << 24);
  }

  public static int getFontId(PFont font) {
    int id = fonts.indexOf(font);
    if (id == -1) {
      id = fonts.size();
      fonts.add(font);
    }
    return id;
  }

  public static PFont getFont(int index) {
    return fonts.get(index);
  }

}
