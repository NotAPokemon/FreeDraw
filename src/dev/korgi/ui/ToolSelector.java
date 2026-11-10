package dev.korgi.ui;

import dev.korgi.json.JSONObject;

public class ToolSelector extends GUI {

  @Override
  protected void drawGUI() {
    loadStyle("mainBar");
    rect(0, 0, screen.width, 50);

  }

  @Override
  protected void createStyleSheet() {
    stylesheet.set("mainBar", new JSONObject()
        .set("borderColor", StyleConstants.BLACK)
        .set("borderSize", 2))
        .set("bg", StyleConstants.WHITE);
  }

}
