package dev.korgi.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;

import dev.korgi.json.JSONObject;
import processing.core.PGraphics;
import processing.core.PImage;

public class DrawingBoard extends GUI {

  private static final int PROJECT_MAGIC = 0x46524457;
  private static final int PROJECT_VERSION = 1;
  private static final long MAX_PROJECT_SIZE = 512L * 1024 * 1024;
  private static final int MAX_PAGE_COUNT = 1000;
  private static final int MAX_STROKES_PER_PAGE = 100_000;
  private static final int MAX_POINTS_PER_STROKE = 1_000_000;
  private static final long MAX_IMAGE_PIXELS = 100_000_000L;
  private static final int MAX_HISTORY_STATES = 50;
  private static final int TOOLBAR_HEIGHT = 50;
  private static final int PAGE_CONTROLS_Y_OFFSET = 44;
  private static final int PAGE_BUTTON_HEIGHT = 34;
  private static final int GRAPH_SPACING = 50;
  private static final int GRAPH_LABEL_INTERVAL = 2;
  private static final float DRAW_WIDTH = 4;
  private static final float ERASE_WIDTH = 24;
  private static final int PREVIOUS_X = 12;
  private static final int PREVIOUS_WIDTH = 34;
  private static final int PAGE_NUMBER_X = 52;
  private static final int PAGE_NUMBER_WIDTH = 64;
  private static final int NEXT_X = 122;
  private static final int NEXT_WIDTH = 34;
  private static final int DELETE_X = 166;
  private static final int ADD_X = 222;
  private static final int PAGE_ACTION_WIDTH = 48;

  private final List<Page> pages = new ArrayList<>();
  private final Deque<HistoryState> undoHistory = new ArrayDeque<>();
  private final Deque<HistoryState> redoHistory = new ArrayDeque<>();
  private final Queue<java.io.File> pendingImages = new ConcurrentLinkedQueue<>();
  private int currentPageIndex;
  private boolean drawing;
  private boolean erasing;
  private boolean selectMode;
  private boolean selectingRegion;
  private boolean movingSelection;
  private boolean selectionMoveRecorded;
  private boolean graphMode;
  private Stroke activeStroke;
  private Point selectionStart;
  private Point selectionEnd;
  private Point lastSelectionPoint;
  private Rectangle2D.Float selectionBounds;
  private final List<Stroke> selectedStrokes = new ArrayList<>();
  private final List<Stroke> selectionClipboard = new ArrayList<>();
  private Rectangle2D.Float clipboardBounds;

  public DrawingBoard() {
    pages.add(new Page());
  }

  @Override
  protected void drawGUI() {
    loadPendingImages();

    Page page = getCurrentPage();
    screen.noStroke();
    screen.fill(255);
    screen.rect(0, TOOLBAR_HEIGHT, screen.width, screen.height - TOOLBAR_HEIGHT);

    if (page.background != null) {
      drawPageBackground(page.background);
    }

    if (graphMode) {
      drawGraph();
    }

    drawAnnotations(page);
    drawSelection();
    drawPageControls();
  }

  @Override
  protected void createStyleSheet() {
    addButtonStyles("page-control", 0xFFE7EAF0, 0xFFD9DEE8, 0xFF303846);
    addButtonStyles("page-delete", 0xFFDC4C4C, 0xFFC83D3D, 0xFFFFFFFF);
    addButtonStyles("page-add", 0xFF3478F6, 0xFF2868DE, 0xFFFFFFFF);
    stylesheet.set("page-number", new JSONObject()
        .set("bg", 0xFF303846)
        .set("txtAlignX", Screen.CENTER)
        .set("txtAlignY", Screen.CENTER));
  }

  @Override
  protected void onClick() {
    if (screen.mouseY < TOOLBAR_HEIGHT || screen.mouseY >= screen.height) {
      return;
    }
    if (handlePageControlClick()) {
      cancelEvent();
      return;
    }

    if (selectMode) {
      Point clickPoint = new Point(screen.mouseX, screen.mouseY);
      if (selectionBounds != null && selectionBounds.contains(clickPoint.x, clickPoint.y)) {
        movingSelection = true;
        selectionMoveRecorded = false;
        lastSelectionPoint = clickPoint;
      } else {
        selectingRegion = true;
        selectionStart = clickPoint;
        selectionEnd = clickPoint;
        selectionBounds = null;
        selectedStrokes.clear();
      }
      cancelEvent();
      return;
    }

    drawing = true;
    recordEdit();
    activeStroke = new Stroke(erasing);
    activeStroke.points.add(new Point(screen.mouseX, screen.mouseY));
    Page page = getCurrentPage();
    page.strokes.add(activeStroke);
    page.annotationsDirty = true;
    cancelEvent();
  }

  @Override
  protected void onClickOff() {
    drawing = false;
    activeStroke = null;
    if (selectingRegion) {
      finishSelection();
    }
    selectingRegion = false;
    movingSelection = false;
    selectionMoveRecorded = false;
    lastSelectionPoint = null;
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void onKeyPress() {
    if (screen.keyEvent == null) {
      return;
    }

    char pressed = Character.toLowerCase(screen.keyEvent.getKey());
    boolean commandDown = screen.keyEvent.isMetaDown() || screen.keyEvent.isControlDown();
    if (commandDown && pressed == 'z' && screen.keyEvent.isShiftDown()) {
      redo();
    } else if (commandDown && pressed == 'z') {
      undo();
    } else if (commandDown && pressed == 'c') {
      copySelection();
    } else if (commandDown && pressed == 'v') {
      pasteSelection();
    } else if (commandDown && pressed == 'x') {
      cutSelection();
    } else if (screen.keyEvent.getKeyCode() == KeyEvent.VK_DELETE
        || screen.keyEvent.getKeyCode() == KeyEvent.VK_BACK_SPACE
        || pressed == 127 || pressed == 8) {
      deleteSelection();
    } else {
      return;
    }
    cancelEvent();
  }

  void onMouseDrag() {
    if (selectMode) {
      if (selectingRegion) {
        selectionEnd = new Point(screen.mouseX, screen.mouseY);
      } else if (movingSelection && !selectedStrokes.isEmpty()) {
        if (!selectionMoveRecorded) {
          recordEdit();
          selectionMoveRecorded = true;
        }
        moveSelection(screen.mouseX - lastSelectionPoint.x, screen.mouseY - lastSelectionPoint.y);
        lastSelectionPoint = new Point(screen.mouseX, screen.mouseY);
      }
      return;
    }

    if (!drawing || activeStroke == null) {
      return;
    }

    if (screen.mouseY < TOOLBAR_HEIGHT || screen.mouseY >= screen.height
        || isPageControlsArea(screen.mouseX, screen.mouseY)) {
      activeStroke = null;
      return;
    }

    if (activeStroke.points.isEmpty()) {
      activeStroke = new Stroke(erasing);
      activeStroke.points.add(new Point(screen.mouseX, screen.mouseY));
      Page page = getCurrentPage();
      page.strokes.add(activeStroke);
      page.annotationsDirty = true;
      return;
    }

    activeStroke.points.add(new Point(screen.mouseX, screen.mouseY));
    getCurrentPage().annotationsDirty = true;
  }

  void setErasing(boolean erasing) {
    this.erasing = erasing;
  }

  void setSelectMode(boolean selectMode) {
    this.selectMode = selectMode;
    selectingRegion = false;
    movingSelection = false;
    if (!selectMode) {
      selectionStart = null;
      selectionEnd = null;
    }
  }

  void setGraphMode(boolean graphMode) {
    if (this.graphMode == graphMode) {
      return;
    }
    recordEdit();
    this.graphMode = graphMode;
  }

  void clear() {
    Page page = getCurrentPage();
    if (page.strokes.isEmpty()) {
      return;
    }
    recordEdit();
    page.strokes.clear();
    page.annotationsDirty = true;
    activeStroke = null;
    selectedStrokes.clear();
    selectionBounds = null;
  }

  void copySelection() {
    if (selectedStrokes.isEmpty() || selectionBounds == null) {
      return;
    }

    selectionClipboard.clear();
    for (Stroke stroke : selectedStrokes) {
      selectionClipboard.add(copyStroke(stroke, 0, 0));
    }
    clipboardBounds = copyBounds(selectionBounds);
  }

  private void cutSelection() {
    copySelection();
    deleteSelection();
  }

  private void deleteSelection() {
    if (selectedStrokes.isEmpty()) {
      return;
    }
    recordEdit();
    getCurrentPage().strokes.removeAll(selectedStrokes);
    getCurrentPage().annotationsDirty = true;
    selectedStrokes.clear();
    selectionBounds = null;
  }

  void pasteSelection() {
    if (selectionClipboard.isEmpty() || clipboardBounds == null) {
      return;
    }

    recordEdit();
    Page page = getCurrentPage();
    float offsetX = screen.mouseX - clipboardBounds.x;
    float offsetY = screen.mouseY - clipboardBounds.y;
    selectedStrokes.clear();
    for (Stroke stroke : selectionClipboard) {
      Stroke pastedStroke = copyStroke(stroke, offsetX, offsetY);
      page.strokes.add(pastedStroke);
      selectedStrokes.add(pastedStroke);
    }
    selectionBounds = copyBounds(clipboardBounds);
    selectionBounds.x += offsetX;
    selectionBounds.y += offsetY;
    page.annotationsDirty = true;
  }

  void enqueueDroppedImage(java.io.File file) {
    pendingImages.add(file);
  }

  boolean isGraphMode() {
    return graphMode;
  }

  void saveProject(File file) throws IOException {
    try (DataOutputStream output = new DataOutputStream(
        new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
      output.writeInt(PROJECT_MAGIC);
      output.writeInt(PROJECT_VERSION);
      output.writeBoolean(graphMode);
      output.writeInt(currentPageIndex);
      output.writeInt(pages.size());

      for (Page page : pages) {
        output.writeBoolean(page.background != null);
        if (page.background != null) {
          byte[] imageBytes = encodeImage(page.background, "png");
          output.writeInt(imageBytes.length);
          output.write(imageBytes);
        }

        output.writeInt(page.strokes.size());
        for (Stroke stroke : page.strokes) {
          output.writeBoolean(stroke.erasing);
          output.writeInt(stroke.points.size());
          for (Point point : stroke.points) {
            output.writeFloat(point.x);
            output.writeFloat(point.y);
          }
        }
      }
    }
  }

  void loadProject(File file) throws IOException {
    int currentPageBeforeLoad = currentPageIndex;
    long fileSize = Files.size(file.toPath());
    if (fileSize > MAX_PROJECT_SIZE) {
      throw new IOException("Project file is too large.");
    }

    List<Page> loadedPages = new ArrayList<>();
    boolean loadedGraphMode;
    int loadedCurrentPage;
    try (DataInputStream input = new DataInputStream(
        new BufferedInputStream(Files.newInputStream(file.toPath())))) {
      if (input.readInt() != PROJECT_MAGIC || input.readInt() != PROJECT_VERSION) {
        throw new IOException("This is not a supported FreeDraw project.");
      }
      loadedGraphMode = input.readBoolean();
      loadedCurrentPage = input.readInt();
      int pageCount = input.readInt();
      if (pageCount < 1 || pageCount > MAX_PAGE_COUNT
          || loadedCurrentPage < 0 || loadedCurrentPage >= pageCount) {
        throw new IOException("The project contains invalid page information.");
      }

      for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
        PImage background = null;
        if (input.readBoolean()) {
          int imageLength = input.readInt();
          if (imageLength <= 0 || imageLength > MAX_PROJECT_SIZE) {
            throw new IOException("The project contains an invalid background image.");
          }
          byte[] imageBytes = new byte[imageLength];
          input.readFully(imageBytes);
          background = decodeImage(imageBytes);
        }

        Page page = new Page(background);
        int strokeCount = input.readInt();
        if (strokeCount < 0 || strokeCount > MAX_STROKES_PER_PAGE) {
          throw new IOException("The project contains an invalid number of strokes.");
        }
        for (int strokeIndex = 0; strokeIndex < strokeCount; strokeIndex++) {
          Stroke stroke = new Stroke(input.readBoolean());
          int pointCount = input.readInt();
          if (pointCount < 1 || pointCount > MAX_POINTS_PER_STROKE) {
            throw new IOException("The project contains an invalid stroke.");
          }
          for (int pointIndex = 0; pointIndex < pointCount; pointIndex++) {
            float x = input.readFloat();
            float y = input.readFloat();
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
              throw new IOException("The project contains invalid drawing data.");
            }
            stroke.points.add(new Point(x, y));
          }
          page.strokes.add(stroke);
        }
        loadedPages.add(page);
      }
    } catch (java.io.EOFException e) {
      throw new IOException("The project file is incomplete.", e);
    }

    pages.clear();
    pages.addAll(loadedPages);
    currentPageIndex = Math.min(currentPageBeforeLoad, pages.size() - 1);
    graphMode = loadedGraphMode;
    undoHistory.clear();
    redoHistory.clear();
    stopDrawing();
  }

  void exportPdf(File file) throws IOException {
    List<BufferedImage> renderedPages = new ArrayList<>(pages.size());
    for (Page page : pages) {
      renderedPages.add(renderPage(page));
    }
    PdfExporter.write(file, renderedPages);
  }

  private byte[] encodeImage(PImage image, String format) throws IOException {
    BufferedImage bufferedImage = toBufferedImage(image);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(bufferedImage, format, output)) {
      throw new IOException("No " + format.toUpperCase(java.util.Locale.ROOT) + " encoder is available.");
    }
    return output.toByteArray();
  }

  private PImage decodeImage(byte[] bytes) throws IOException {
    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
    if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0
        || (long) bufferedImage.getWidth() * bufferedImage.getHeight() > MAX_IMAGE_PIXELS) {
      throw new IOException("The project contains an invalid or oversized background image.");
    }
    PImage image = screen.createImage(bufferedImage.getWidth(), bufferedImage.getHeight(), Screen.ARGB);
    image.loadPixels();
    bufferedImage.getRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
    image.updatePixels();
    return image;
  }

  private BufferedImage toBufferedImage(PImage image) {
    image.loadPixels();
    BufferedImage bufferedImage = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB);
    bufferedImage.setRGB(0, 0, image.width, image.height, image.pixels, 0, image.width);
    return bufferedImage;
  }

  private BufferedImage renderPage(Page page) {
    int canvasHeight = screen.height - TOOLBAR_HEIGHT;
    BufferedImage result = new BufferedImage(screen.width, canvasHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = result.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    if (page.background != null) {
      BufferedImage background = toBufferedImage(page.background);
      float scale = Math.min((float) result.getWidth() / background.getWidth(),
          (float) result.getHeight() / background.getHeight());
      int width = Math.round(background.getWidth() * scale);
      int height = Math.round(background.getHeight() * scale);
      graphics.drawImage(background, (result.getWidth() - width) / 2, (result.getHeight() - height) / 2,
          width, height, null);
    }
    if (graphMode) {
      drawGraph(graphics, result.getWidth(), result.getHeight());
    }

    BufferedImage annotations = new BufferedImage(result.getWidth(), result.getHeight(),
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D annotationGraphics = annotations.createGraphics();
    annotationGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    for (Stroke stroke : page.strokes) {
      annotationGraphics.setStroke(new BasicStroke(stroke.erasing ? ERASE_WIDTH : DRAW_WIDTH,
          BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      annotationGraphics.setComposite(stroke.erasing ? AlphaComposite.Clear : AlphaComposite.SrcOver);
      annotationGraphics.setColor(stroke.erasing ? Color.WHITE : new Color(25, 25, 25));
      if (stroke.points.size() == 1) {
        Point point = stroke.points.get(0);
        int diameter = Math.round(stroke.erasing ? ERASE_WIDTH : DRAW_WIDTH);
        annotationGraphics.fillOval(Math.round(point.x - diameter / 2f),
            Math.round(point.y - TOOLBAR_HEIGHT - diameter / 2f), diameter, diameter);
      } else {
        for (int i = 1; i < stroke.points.size(); i++) {
          Point from = stroke.points.get(i - 1);
          Point to = stroke.points.get(i);
          annotationGraphics.drawLine(Math.round(from.x), Math.round(from.y - TOOLBAR_HEIGHT),
              Math.round(to.x), Math.round(to.y - TOOLBAR_HEIGHT));
        }
      }
    }
    annotationGraphics.dispose();
    graphics.drawImage(annotations, 0, 0, null);
    graphics.dispose();
    return result;
  }

  private void drawGraph(Graphics2D graphics, int width, int height) {
    int axisX = width / 2;
    int axisY = height / 2;
    graphics.setStroke(new BasicStroke(1));
    graphics.setColor(new Color(220, 227, 236));
    for (int x = axisX % GRAPH_SPACING; x < width; x += GRAPH_SPACING) {
      graphics.drawLine(x, 0, x, height);
    }
    for (int y = 0; y < height; y += GRAPH_SPACING) {
      graphics.drawLine(0, y, width, y);
    }
    graphics.setStroke(new BasicStroke(2));
    graphics.setColor(new Color(82, 97, 116));
    graphics.drawLine(0, axisY, width, axisY);
    graphics.drawLine(axisX, 0, axisX, height);

    graphics.setColor(new Color(89, 103, 122));
    graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
    for (int x = axisX - GRAPH_SPACING * GRAPH_LABEL_INTERVAL; x >= 0;
        x -= GRAPH_SPACING * GRAPH_LABEL_INTERVAL) {
      graphics.drawString(Integer.toString((x - axisX) / GRAPH_SPACING), x - 4, axisY + 16);
    }
    graphics.drawString("0", axisX + 4, axisY + 16);
    for (int x = axisX + GRAPH_SPACING * GRAPH_LABEL_INTERVAL; x < width;
        x += GRAPH_SPACING * GRAPH_LABEL_INTERVAL) {
      graphics.drawString(Integer.toString((x - axisX) / GRAPH_SPACING), x - 4, axisY + 16);
    }
    for (int y = axisY - GRAPH_SPACING * GRAPH_LABEL_INTERVAL; y >= 0;
        y -= GRAPH_SPACING * GRAPH_LABEL_INTERVAL) {
      graphics.drawString(Integer.toString((axisY - y) / GRAPH_SPACING), axisX + 8, y + 4);
    }
    for (int y = axisY + GRAPH_SPACING * GRAPH_LABEL_INTERVAL; y < height;
        y += GRAPH_SPACING * GRAPH_LABEL_INTERVAL) {
      graphics.drawString(Integer.toString((axisY - y) / GRAPH_SPACING), axisX + 8, y + 4);
    }
    graphics.drawString("x", width - 12, axisY + 16);
    graphics.drawString("y", axisX + 8, 14);
  }

  private void loadPendingImages() {
    java.io.File file;
    while ((file = pendingImages.poll()) != null) {
      PImage image = screen.loadImage(file.getAbsolutePath());
      if (image == null) {
        System.err.println("Unable to load dropped image: " + file.getAbsolutePath());
        continue;
      }

      stopDrawing();
      recordEdit();
      currentPageIndex++;
      pages.add(currentPageIndex, new Page(image));
    }
  }

  private void drawPageBackground(PImage image) {
    float availableHeight = screen.height - TOOLBAR_HEIGHT;
    float scale = Math.min((float) screen.width / image.width, availableHeight / image.height);
    float width = image.width * scale;
    float height = image.height * scale;
    screen.imageMode(Screen.CORNER);
    screen.image(image, (screen.width - width) / 2f, TOOLBAR_HEIGHT + (availableHeight - height) / 2f,
        width, height);
  }

  private void drawAnnotations(Page page) {
    if (page.annotations == null || page.annotations.width != screen.width
        || page.annotations.height != screen.height) {
      page.annotations = screen.createGraphics(screen.width, screen.height);
      page.annotationsDirty = true;
    }

    if (page.annotationsDirty) {
      page.annotations.beginDraw();
      page.annotations.clear();
      page.annotations.strokeCap(Screen.ROUND);
      for (Stroke stroke : page.strokes) {
        if (stroke.erasing) {
          page.annotations.blendMode(Screen.REPLACE);
          page.annotations.stroke(0, 0);
          page.annotations.strokeWeight(ERASE_WIDTH);
        } else {
          page.annotations.blendMode(Screen.BLEND);
          page.annotations.stroke(25);
          page.annotations.strokeWeight(DRAW_WIDTH);
        }

        if (stroke.points.size() == 1) {
          Point point = stroke.points.get(0);
          page.annotations.point(point.x, point.y);
        } else {
          for (int i = 1; i < stroke.points.size(); i++) {
            Point from = stroke.points.get(i - 1);
            Point to = stroke.points.get(i);
            page.annotations.line(from.x, from.y, to.x, to.y);
          }
        }
      }
      page.annotations.endDraw();
      page.annotationsDirty = false;
    }

    screen.image(page.annotations, 0, 0);
  }

  private Page getCurrentPage() {
    return pages.get(currentPageIndex);
  }

  private boolean handlePageControlClick() {
    int controlsY = screen.height - PAGE_CONTROLS_Y_OFFSET;
    if (hitTest(PREVIOUS_X, controlsY, PREVIOUS_WIDTH, PAGE_BUTTON_HEIGHT)) {
      if (currentPageIndex > 0) {
        currentPageIndex--;
      }
      stopDrawing();
      return true;
    }
    if (hitTest(NEXT_X, controlsY, NEXT_WIDTH, PAGE_BUTTON_HEIGHT)) {
      if (currentPageIndex < pages.size() - 1) {
        currentPageIndex++;
      }
      stopDrawing();
      return true;
    }
    if (hitTest(DELETE_X, controlsY, PAGE_ACTION_WIDTH, PAGE_BUTTON_HEIGHT)) {
      if (!selectedStrokes.isEmpty()) {
        deleteSelection();
      } else if (pages.size() == 1) {
        if (!getCurrentPage().strokes.isEmpty()) {
          recordEdit();
        }
        getCurrentPage().strokes.clear();
      } else {
        recordEdit();
        pages.remove(currentPageIndex);
        currentPageIndex = Math.max(0, currentPageIndex - 1);
      }
      stopDrawing();
      return true;
    }
    if (hitTest(ADD_X, controlsY, PAGE_ACTION_WIDTH, PAGE_BUTTON_HEIGHT)) {
      recordEdit();
      pages.add(currentPageIndex + 1, new Page());
      currentPageIndex++;
      stopDrawing();
      return true;
    }
    return isPageControlsArea(screen.mouseX, screen.mouseY);
  }

  private boolean isPageControlsArea(float x, float y) {
    return x >= PREVIOUS_X && x <= ADD_X + PAGE_ACTION_WIDTH
        && y >= screen.height - PAGE_CONTROLS_Y_OFFSET
        && y <= screen.height - PAGE_CONTROLS_Y_OFFSET + PAGE_BUTTON_HEIGHT;
  }

  private void stopDrawing() {
    drawing = false;
    activeStroke = null;
    selectingRegion = false;
    movingSelection = false;
    selectionStart = null;
    selectionEnd = null;
    selectionBounds = null;
    selectedStrokes.clear();
    selectionMoveRecorded = false;
  }

  private void finishSelection() {
    if (selectionStart == null || selectionEnd == null) {
      return;
    }

    float left = Math.min(selectionStart.x, selectionEnd.x);
    float right = Math.max(selectionStart.x, selectionEnd.x);
    float top = Math.min(selectionStart.y, selectionEnd.y);
    float bottom = Math.max(selectionStart.y, selectionEnd.y);
    Rectangle2D.Float region = new Rectangle2D.Float(left, top, right - left, bottom - top);
    selectedStrokes.clear();
    selectionBounds = null;

    for (Stroke stroke : getCurrentPage().strokes) {
      boolean intersects = false;
      for (int i = 0; i < stroke.points.size(); i++) {
        Point point = stroke.points.get(i);
        if (point.x >= left && point.x <= right && point.y >= top && point.y <= bottom) {
          intersects = true;
          break;
        }
        if (i > 0) {
          Point previous = stroke.points.get(i - 1);
          if (region.intersectsLine(previous.x, previous.y, point.x, point.y)) {
            intersects = true;
            break;
          }
        }
      }
      if (intersects) {
        selectedStrokes.add(stroke);
        for (Point point : stroke.points) {
          selectionBounds = expandBounds(selectionBounds, point.x, point.y);
        }
      }
    }
    if (selectionBounds != null) {
      selectionBounds.x -= 6;
      selectionBounds.y -= 6;
      selectionBounds.width += 12;
      selectionBounds.height += 12;
    }
    selectionStart = null;
    selectionEnd = null;
  }

  private void moveSelection(float dx, float dy) {
    if (dx == 0 && dy == 0) {
      return;
    }
    for (Stroke stroke : selectedStrokes) {
      for (Point point : stroke.points) {
        point.x += dx;
        point.y += dy;
      }
    }
    selectionBounds.x += dx;
    selectionBounds.y += dy;
    getCurrentPage().annotationsDirty = true;
  }

  private void recordEdit() {
    undoHistory.addLast(captureHistoryState());
    if (undoHistory.size() > MAX_HISTORY_STATES) {
      undoHistory.removeFirst();
    }
    redoHistory.clear();
  }

  private void undo() {
    if (undoHistory.isEmpty()) {
      return;
    }
    redoHistory.addLast(captureHistoryState());
    if (redoHistory.size() > MAX_HISTORY_STATES) {
      redoHistory.removeFirst();
    }
    restoreHistoryState(undoHistory.removeLast());
  }

  private void redo() {
    if (redoHistory.isEmpty()) {
      return;
    }
    undoHistory.addLast(captureHistoryState());
    if (undoHistory.size() > MAX_HISTORY_STATES) {
      undoHistory.removeFirst();
    }
    restoreHistoryState(redoHistory.removeLast());
  }

  private HistoryState captureHistoryState() {
    List<Page> pageCopies = new ArrayList<>(pages.size());
    for (Page page : pages) {
      Page copy = new Page(page.background);
      for (Stroke stroke : page.strokes) {
        copy.strokes.add(copyStroke(stroke, 0, 0));
      }
      pageCopies.add(copy);
    }
    return new HistoryState(pageCopies, currentPageIndex, graphMode);
  }

  private void restoreHistoryState(HistoryState state) {
    pages.clear();
    pages.addAll(state.pages);
    currentPageIndex = state.currentPageIndex;
    graphMode = state.graphMode;
    if (screen.selector != null) {
      screen.selector.setGraphMode(graphMode);
    }
    stopDrawing();
    selectionStart = null;
    selectionEnd = null;
    selectionBounds = null;
  }

  private Stroke copyStroke(Stroke source, float dx, float dy) {
    Stroke copy = new Stroke(source.erasing);
    for (Point point : source.points) {
      copy.points.add(new Point(point.x + dx, point.y + dy));
    }
    return copy;
  }

  private Rectangle2D.Float expandBounds(Rectangle2D.Float bounds, float x, float y) {
    if (bounds == null) {
      return new Rectangle2D.Float(x, y, 0, 0);
    }
    float left = Math.min(bounds.x, x);
    float top = Math.min(bounds.y, y);
    float right = Math.max(bounds.x + bounds.width, x);
    float bottom = Math.max(bounds.y + bounds.height, y);
    bounds.setFrame(left, top, right - left, bottom - top);
    return bounds;
  }

  private Rectangle2D.Float copyBounds(Rectangle2D.Float bounds) {
    return new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height);
  }

  private void drawSelection() {
    Rectangle2D.Float bounds = selectionBounds;
    if (selectingRegion && selectionStart != null && selectionEnd != null) {
      bounds = new Rectangle2D.Float(
          Math.min(selectionStart.x, selectionEnd.x),
          Math.min(selectionStart.y, selectionEnd.y),
          Math.abs(selectionEnd.x - selectionStart.x),
          Math.abs(selectionEnd.y - selectionStart.y));
    }
    if (bounds == null) {
      return;
    }

    screen.noFill();
    screen.stroke(0xFF168A76);
    screen.strokeWeight(1);
    drawDashedLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y);
    drawDashedLine(bounds.x + bounds.width, bounds.y,
        bounds.x + bounds.width, bounds.y + bounds.height);
    drawDashedLine(bounds.x + bounds.width, bounds.y + bounds.height,
        bounds.x, bounds.y + bounds.height);
    drawDashedLine(bounds.x, bounds.y + bounds.height, bounds.x, bounds.y);
    screen.noStroke();
  }

  private void drawDashedLine(float x1, float y1, float x2, float y2) {
    float length = (float) Math.hypot(x2 - x1, y2 - y1);
    float dx = (x2 - x1) / Math.max(length, 1);
    float dy = (y2 - y1) / Math.max(length, 1);
    for (float distance = 0; distance < length; distance += 8) {
      float end = Math.min(distance + 4, length);
      screen.line(x1 + dx * distance, y1 + dy * distance, x1 + dx * end, y1 + dy * end);
    }
  }

  private void drawPageControls() {
    int controlsY = screen.height - PAGE_CONTROLS_Y_OFFSET;
    drawGhostButton("<", PREVIOUS_X, controlsY, PREVIOUS_WIDTH, PAGE_BUTTON_HEIGHT, "page-control");
    loadStyle("page-number");
    screen.text((currentPageIndex + 1) + " / " + pages.size(),
        PAGE_NUMBER_X + PAGE_NUMBER_WIDTH / 2f, controlsY + PAGE_BUTTON_HEIGHT / 2f);
    drawGhostButton(">", NEXT_X, controlsY, NEXT_WIDTH, PAGE_BUTTON_HEIGHT, "page-control");
    drawGhostButton("Delete", DELETE_X, controlsY, PAGE_ACTION_WIDTH, PAGE_BUTTON_HEIGHT, "page-delete");
    drawGhostButton("Add", ADD_X, controlsY, PAGE_ACTION_WIDTH, PAGE_BUTTON_HEIGHT, "page-add");
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

  private void drawGraph() {
    float axisX = screen.width / 2f;
    float axisY = TOOLBAR_HEIGHT + (screen.height - TOOLBAR_HEIGHT) / 2f;

    screen.stroke(0xFFDCE3EC);
    screen.strokeWeight(1);
    for (int x = Math.round(axisX) % GRAPH_SPACING; x < screen.width; x += GRAPH_SPACING) {
      screen.line(x, TOOLBAR_HEIGHT, x, screen.height);
    }
    int firstGridY = TOOLBAR_HEIGHT
        + (GRAPH_SPACING - (TOOLBAR_HEIGHT % GRAPH_SPACING)) % GRAPH_SPACING;
    for (int y = firstGridY; y < screen.height; y += GRAPH_SPACING) {
      screen.line(0, y, screen.width, y);
    }

    screen.stroke(0xFF526174);
    screen.strokeWeight(2);
    screen.line(0, axisY, screen.width, axisY);
    screen.line(axisX, TOOLBAR_HEIGHT, axisX, screen.height);

    screen.noStroke();
    screen.fill(0xFF59677A);
    screen.textSize(12);
    screen.textAlign(Screen.LEFT, Screen.CENTER);
    for (int y = Math.round(axisY) - GRAPH_SPACING * GRAPH_LABEL_INTERVAL; y >= TOOLBAR_HEIGHT; y -= GRAPH_SPACING
        * GRAPH_LABEL_INTERVAL) {
      int value = (int) ((axisY - y) / GRAPH_SPACING);
      screen.text(Integer.toString(value), axisX + 8, y);
    }
    for (int y = Math.round(axisY) + GRAPH_SPACING * GRAPH_LABEL_INTERVAL; y < screen.height; y += GRAPH_SPACING
        * GRAPH_LABEL_INTERVAL) {
      int value = (int) ((axisY - y) / GRAPH_SPACING);
      screen.text(Integer.toString(value), axisX + 8, y);
    }

    screen.textAlign(Screen.CENTER, Screen.TOP);
    screen.text("0", axisX, axisY + 6);
    for (int x = Math.round(axisX) - GRAPH_SPACING * GRAPH_LABEL_INTERVAL; x >= 0; x -= GRAPH_SPACING
        * GRAPH_LABEL_INTERVAL) {
      int value = (int) ((x - axisX) / GRAPH_SPACING);
      screen.text(Integer.toString(value), x, axisY + 6);
    }
    for (int x = Math.round(axisX) + GRAPH_SPACING * GRAPH_LABEL_INTERVAL; x < screen.width; x += GRAPH_SPACING
        * GRAPH_LABEL_INTERVAL) {
      int value = (int) ((x - axisX) / GRAPH_SPACING);
      screen.text(Integer.toString(value), x, axisY + 6);
    }

    screen.textAlign(Screen.RIGHT, Screen.TOP);
    screen.text("x", screen.width - 8, axisY + 6);
    screen.textAlign(Screen.LEFT, Screen.TOP);
    screen.text("y", axisX + 8, TOOLBAR_HEIGHT + 4);
  }

  private static class Stroke {
    private final boolean erasing;
    private final List<Point> points = new ArrayList<>();

    private Stroke(boolean erasing) {
      this.erasing = erasing;
    }
  }

  private static class Page {
    private final List<Stroke> strokes = new ArrayList<>();
    private final PImage background;
    private PGraphics annotations;
    private boolean annotationsDirty = true;

    private Page() {
      this(null);
    }

    private Page(PImage background) {
      this.background = background;
    }
  }

  private static class HistoryState {
    private final List<Page> pages;
    private final int currentPageIndex;
    private final boolean graphMode;

    private HistoryState(List<Page> pages, int currentPageIndex, boolean graphMode) {
      this.pages = pages;
      this.currentPageIndex = currentPageIndex;
      this.graphMode = graphMode;
    }
  }

  private static class Point {
    private float x;
    private float y;

    private Point(float x, float y) {
      this.x = x;
      this.y = y;
    }
  }

}
