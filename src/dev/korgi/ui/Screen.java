package dev.korgi.ui;

import java.util.ArrayList;
import java.util.List;

import dev.korgi.math.VectorConstants;
import processing.core.PApplet;

public class Screen extends PApplet {

  private static final Screen screen = new Screen();

  public static Screen getInstance() {
    return screen;
  }

  private final List<GUI> guis = new ArrayList<>();
  boolean eventCanceled = false;

  public List<GUI> getGuiStack() {
    return guis;
  }

  private Screen() {

  }

  @Override
  public void settings() {

  }

  @Override
  public void setup() {
    background(255);

  }

  @Override
  public void draw() {
    VectorConstants.ensure();

    List<GUI> list = getGuiStack();
    for (int i = 0; i < list.size(); i++) {
      if (eventCanceled) {
        eventCanceled = false;
        break;
      }
      list.get(i).draw();
    }
  }

  @Override
  public void keyPressed() {
    List<GUI> list = getGuiStack();
    for (int i = 0; i < list.size(); i++) {
      if (eventCanceled) {
        eventCanceled = false;
        return;
      }
      list.get(i).onKeyPress();
    }
  }

  @Override
  public void mousePressed() {
    List<GUI> list = getGuiStack();
    for (int i = 0; i < list.size(); i++) {
      if (eventCanceled) {
        eventCanceled = false;
        return;
      }
      list.get(i).onClick();
    }
  }

  @Override
  public void mouseReleased() {
    List<GUI> list = getGuiStack();
    for (int i = 0; i < list.size(); i++) {
      if (eventCanceled) {
        eventCanceled = false;
        return;
      }
      list.get(i).onClickOff();
    }
  }

  @Override
  public void mouseDragged() {

  }

}