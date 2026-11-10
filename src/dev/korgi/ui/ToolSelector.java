package dev.korgi.ui;

import dev.korgi.json.JSONObject;

public class ToolSelector extends GUI {

  private static final int TOOLBAR_HEIGHT = 50;
  private static final int BUTTON_HEIGHT = 34;
  private static final int BUTTON_Y = 8;
  private static final int DRAW_X = 12;
  private static final int TOOL_WIDTH = 82;
  private static final int SELECT_X = 102;
  private static final int ERASE_X = 192;
  private static final int CLEAR_X = 282;
  private static final int GRAPH_X = 372;
  private static final int GRAPH_WIDTH = 96;
  private static final int EXPORT_X = 476;
  private static final int EXPORT_WIDTH = 96;
  private static final int SAVE_X = 580;
  private static final int LOAD_X = 656;
  private static final int SAVE_AS_X = 732;
  private static final int FILE_BUTTON_WIDTH = 70;
  private static final int SAVE_AS_WIDTH = 100;

  private boolean erasing;
  private boolean selecting;
  private boolean graphMode;

  @Override
  protected void drawGUI() {
    loadStyle("mainBar");
    rect(0, 0, screen.width, TOOLBAR_HEIGHT);

    drawGhostButton("Draw", DRAW_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT,
        !erasing && !selecting ? "tool-active" : "tool-inactive");
    drawGhostButton("Select", SELECT_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT,
        selecting ? "select-active" : "tool-inactive");
    drawGhostButton("Erase", ERASE_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT,
        erasing ? "tool-active" : "tool-inactive");
    drawGhostButton("Clear", CLEAR_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT, "clear");
    drawGhostButton("XY Graph", GRAPH_X, BUTTON_Y, GRAPH_WIDTH, BUTTON_HEIGHT,
        graphMode ? "graph-active" : "tool-inactive");
    drawGhostButton("Export PDF", EXPORT_X, BUTTON_Y, EXPORT_WIDTH, BUTTON_HEIGHT, "tool-inactive");
    drawGhostButton("Save", SAVE_X, BUTTON_Y, FILE_BUTTON_WIDTH, BUTTON_HEIGHT, "tool-inactive");
    drawGhostButton("Load", LOAD_X, BUTTON_Y, FILE_BUTTON_WIDTH, BUTTON_HEIGHT, "tool-inactive");
    drawGhostButton("Save As", SAVE_AS_X, BUTTON_Y, SAVE_AS_WIDTH, BUTTON_HEIGHT, "tool-inactive");
  }

  @Override
  protected void createStyleSheet() {
    stylesheet.set("mainBar", new JSONObject()
        .set("bg", 0xFFF5F6F8)
        .set("borderColor", 0xFFD8DDE5)
        .set("borderSize", 1));

    addButtonStyles("tool-active", 0xFF3478F6, 0xFF2868DE, 0xFFFFFFFF);
    addButtonStyles("tool-inactive", 0xFFE7EAF0, 0xFFD9DEE8, 0xFF303846);
    addButtonStyles("clear", 0xFFDC4C4C, 0xFFC83D3D, 0xFFFFFFFF);
    addButtonStyles("graph-active", 0xFF7455C7, 0xFF6243B5, 0xFFFFFFFF);
    addButtonStyles("select-active", 0xFF168A76, 0xFF117462, 0xFFFFFFFF);
  }

  @Override
  protected void onClick() {
    if (hitTest(DRAW_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT)) {
      selecting = false;
      erasing = false;
      screen.board.setSelectMode(false);
      screen.board.setErasing(false);
      cancelEvent();
    } else if (hitTest(SELECT_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT)) {
      selecting = true;
      erasing = false;
      screen.board.setSelectMode(true);
      screen.board.setErasing(false);
      cancelEvent();
    } else if (hitTest(ERASE_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT)) {
      selecting = false;
      erasing = true;
      screen.board.setSelectMode(false);
      screen.board.setErasing(true);
      cancelEvent();
    } else if (hitTest(CLEAR_X, BUTTON_Y, TOOL_WIDTH, BUTTON_HEIGHT)) {
      screen.board.clear();
      cancelEvent();
    } else if (hitTest(GRAPH_X, BUTTON_Y, GRAPH_WIDTH, BUTTON_HEIGHT)) {
      graphMode = !graphMode;
      screen.board.setGraphMode(graphMode);
      cancelEvent();
    } else if (hitTest(EXPORT_X, BUTTON_Y, EXPORT_WIDTH, BUTTON_HEIGHT)) {
      screen.exportPdf();
      cancelEvent();
    } else if (hitTest(SAVE_X, BUTTON_Y, FILE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
      screen.saveProject();
      cancelEvent();
    } else if (hitTest(LOAD_X, BUTTON_Y, FILE_BUTTON_WIDTH, BUTTON_HEIGHT)) {
      screen.loadProject();
      cancelEvent();
    } else if (hitTest(SAVE_AS_X, BUTTON_Y, SAVE_AS_WIDTH, BUTTON_HEIGHT)) {
      screen.saveProjectAs();
      cancelEvent();
    }
  }

  void setGraphMode(boolean graphMode) {
    this.graphMode = graphMode;
  }

  private void addButtonStyles(String name, int background, int hoverBackground, int textColor) {
    stylesheet.set("btn-" + name + "-bg", new JSONObject()
        .set("bg", background)
        .set("borderRadius", 6f));
    stylesheet.set("btn-" + name + "-bg.hover", new JSONObject()
        .set("bg", hoverBackground));
    stylesheet.set("btn-" + name + "-label", new JSONObject()
        .set("bg", textColor)
        .set("txtAlignX", Screen.CENTER)
        .set("txtAlignY", Screen.CENTER));
    stylesheet.set("btn-" + name + "-label.hover", new JSONObject()
        .set("bg", textColor));
  }

}
