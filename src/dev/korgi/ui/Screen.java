package dev.korgi.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import dev.korgi.math.VectorConstants;
import processing.core.PApplet;

public class Screen extends PApplet {

  private static final Screen screen = new Screen();

  public static Screen getInstance() {
    return screen;
  }

  private final List<GUI> guis = new ArrayList<>();
  private final Queue<Runnable> pendingFileOperations = new ConcurrentLinkedQueue<>();
  private volatile File rememberedFolder = new File(System.getProperty("user.home"));
  private volatile File currentProjectFile;
  boolean eventCanceled = false;
  ToolSelector selector;
  DrawingBoard board;

  public List<GUI> getGuiStack() {
    return guis;
  }

  private Screen() {

  }

  @Override
  public void settings() {
    size(900, 600);
  }

  @Override
  public void setup() {
    background(255);
    board = new DrawingBoard();
    selector = new ToolSelector();
    board.show();
    selector.show();
    installImageDropTarget();
  }

  @Override
  public void draw() {
    VectorConstants.ensure();
    processPendingFileOperations();

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
    if (isSaveShortcut()) {
      saveProject();
      return;
    }

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
    if (board != null) {
      board.onMouseDrag();
    }

  }

  void exportPdf() {
    chooseFile(true, "Export drawing as PDF", "PDF files", "pdf", file -> enqueueFileOperation(() -> {
      try {
        board.exportPdf(withExtension(file, "pdf"));
      } catch (IOException e) {
        showFileError("Unable to export PDF", e);
      }
    }));
  }

  void saveProject() {
    File projectFile = currentProjectFile;
    if (projectFile != null) {
      enqueueProjectSave(projectFile);
      return;
    }
    saveProjectAs();
  }

  void saveProjectAs() {
    chooseFile(true, "Save drawing project as", "FreeDraw projects", "freedraw",
        file -> enqueueProjectSave(file));
  }

  private void enqueueProjectSave(File file) {
    enqueueFileOperation(() -> {
      try {
        File selected = withExtension(file, "freedraw");
        board.saveProject(selected);
        currentProjectFile = selected;
        rememberFolder(selected.getParentFile());
      } catch (IOException e) {
        showFileError("Unable to save project", e);
      }
    });
  }

  void loadProject() {
    chooseFile(false, "Load drawing project", "FreeDraw projects", "freedraw",
        file -> enqueueFileOperation(() -> {
          try {
            board.loadProject(file);
            selector.setGraphMode(board.isGraphMode());
            currentProjectFile = file.getAbsoluteFile();
            rememberFolder(currentProjectFile.getParentFile());
          } catch (IOException | RuntimeException e) {
            showFileError("Unable to load project", e);
          }
        }));
  }

  private void chooseFile(boolean save, String title, String filterName, String extension,
      Consumer<File> onSelected) {
    SwingUtilities.invokeLater(() -> {
      try {
        Component parent = surface.getNative() instanceof Component ? (Component) surface.getNative() : null;
        JFileChooser chooser = new JFileChooser();
        if (rememberedFolder != null && rememberedFolder.isDirectory()) {
          chooser.setCurrentDirectory(rememberedFolder);
        }
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(filterName + " (*." + extension + ")", extension));
        int result = save ? chooser.showSaveDialog(parent) : chooser.showOpenDialog(parent);
        rememberFolder(chooser.getCurrentDirectory());
        if (result != JFileChooser.APPROVE_OPTION) {
          return;
        }

        File selected = save ? withExtension(chooser.getSelectedFile(), extension) : chooser.getSelectedFile();
        rememberFolder(selected.getAbsoluteFile().getParentFile());
        if (save && selected.exists()) {
          int overwrite = JOptionPane.showConfirmDialog(parent,
              "Replace the existing file?", title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
          if (overwrite != JOptionPane.YES_OPTION) {
            return;
          }
        }
        onSelected.accept(selected);
      } catch (RuntimeException e) {
        showFileError(title, e);
      }
    });
  }

  private void enqueueFileOperation(Runnable operation) {
    pendingFileOperations.add(operation);
  }

  private void processPendingFileOperations() {
    Runnable operation;
    while ((operation = pendingFileOperations.poll()) != null) {
      operation.run();
    }
  }

  private File withExtension(File file, String extension) {
    if (file.getName().toLowerCase(java.util.Locale.ROOT).endsWith("." + extension)) {
      return file;
    }
    return new File(file.getParentFile(), file.getName() + "." + extension);
  }

  private void rememberFolder(File folder) {
    if (folder != null && folder.isDirectory()) {
      rememberedFolder = folder;
    }
  }

  @SuppressWarnings("deprecation")
  private boolean isSaveShortcut() {
    return keyEvent != null
        && (keyEvent.isMetaDown() || keyEvent.isControlDown())
        && Character.toLowerCase(keyEvent.getKey()) == 's';
  }

  private void showFileError(String title, Exception error) {
    error.printStackTrace();
    SwingUtilities.invokeLater(() -> {
      Component parent = surface.getNative() instanceof Component ? (Component) surface.getNative() : null;
      JOptionPane.showMessageDialog(parent, error.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    });
  }

  private void installImageDropTarget() {
    Object nativeSurface = surface.getNative();
    if (!(nativeSurface instanceof Component)) {
      System.err.println("Image drag and drop is unavailable for this Processing surface.");
      return;
    }

    new DropTarget((Component) nativeSurface, DnDConstants.ACTION_COPY,
        new DropTargetAdapter() {
          @Override
          public void drop(DropTargetDropEvent event) {
            Point location = event.getLocation();
            if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || location.x < 0 || location.x >= width
                || location.y < 50 || location.y >= height - 44) {
              event.rejectDrop();
              return;
            }

            event.acceptDrop(DnDConstants.ACTION_COPY);
            try {
              Object transferData = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
              if (!(transferData instanceof List<?>)) {
                event.dropComplete(false);
                return;
              }

              boolean queued = false;
              for (Object item : (List<?>) transferData) {
                if (item instanceof File && ((File) item).isFile() && isImageFile((File) item)) {
                  board.enqueueDroppedImage((File) item);
                  queued = true;
                }
              }
              event.dropComplete(queued);
            } catch (UnsupportedFlavorException | IOException e) {
              System.err.println("Unable to read dropped image file: " + e.getMessage());
              event.dropComplete(false);
            }
          }
        });
  }

  private boolean isImageFile(File file) {
    String name = file.getName().toLowerCase(java.util.Locale.ROOT);
    return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
        || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".tif")
        || name.endsWith(".tiff") || name.endsWith(".tga");
  }

}