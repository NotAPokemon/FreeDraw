package dev.korgi;

import dev.korgi.ui.Screen;
import processing.core.PApplet;

public class Main {
  public static void main(String[] args) {
    PApplet.runSketch(new String[] { "FreeDraw" }, Screen.getInstance());
    Screen.getInstance().windowResizable(true);
  }
}
