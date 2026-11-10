package dev.korgi.ui;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

final class PdfExporter {

  private PdfExporter() {
  }

  static void write(File file, List<BufferedImage> pages) throws IOException {
    if (pages.isEmpty()) {
      throw new IOException("There are no pages to export.");
    }

    try (CountingOutputStream output = new CountingOutputStream(
        new BufferedOutputStream(new java.io.FileOutputStream(file)))) {
      writeAscii(output, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
      int objectCount = 2 + pages.size() * 3;
      long[] offsets = new long[objectCount + 1];

      ByteArrayOutputStream kidsBuilder = new ByteArrayOutputStream();
      for (int i = 0; i < pages.size(); i++) {
        writeAscii(kidsBuilder, (3 + i * 3) + " 0 R ");
      }

      writeObject(output, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
      writeObject(output, offsets, 2, "<< /Type /Pages /Kids ["
          + new String(kidsBuilder.toByteArray(), StandardCharsets.US_ASCII)
          + "] /Count " + pages.size() + " >>");

      for (int i = 0; i < pages.size(); i++) {
        BufferedImage page = pages.get(i);
        int pageObject = 3 + i * 3;
        int contentObject = pageObject + 1;
        int imageObject = pageObject + 2;
        byte[] jpeg = encodeJpeg(page);
        String content = "q\n" + page.getWidth() + " 0 0 " + page.getHeight()
            + " 0 0 cm\n/Im0 Do\nQ\n";

        offsets[pageObject] = output.getCount();
        writeAscii(output, pageObject + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
            + page.getWidth() + " " + page.getHeight()
            + "] /Resources << /XObject << /Im0 " + imageObject
            + " 0 R >> >> /Contents " + contentObject + " 0 R >>\nendobj\n");

        offsets[contentObject] = output.getCount();
        byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
        writeAscii(output, contentObject + " 0 obj\n<< /Length " + contentBytes.length
            + " >>\nstream\n");
        output.write(contentBytes);
        writeAscii(output, "endstream\nendobj\n");

        offsets[imageObject] = output.getCount();
        writeAscii(output, imageObject + " 0 obj\n<< /Type /XObject /Subtype /Image /Width "
            + page.getWidth() + " /Height " + page.getHeight()
            + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
            + jpeg.length + " >>\nstream\n");
        output.write(jpeg);
        writeAscii(output, "\nendstream\nendobj\n");
      }

      long xrefOffset = output.getCount();
      writeAscii(output, "xref\n0 " + (objectCount + 1) + "\n0000000000 65535 f \n");
      for (int i = 1; i <= objectCount; i++) {
        writeAscii(output, String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets[i]));
      }
      writeAscii(output, "trailer\n<< /Size " + (objectCount + 1)
          + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
    }
  }

  private static byte[] encodeJpeg(BufferedImage image) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "jpg", output)) {
      throw new IOException("No JPEG encoder is available.");
    }
    return output.toByteArray();
  }

  private static void writeObject(CountingOutputStream output, long[] offsets, int number, String body)
      throws IOException {
    offsets[number] = output.getCount();
    writeAscii(output, number + " 0 obj\n" + body + "\nendobj\n");
  }

  private static void writeAscii(OutputStream output, String value) throws IOException {
    output.write(value.getBytes(StandardCharsets.ISO_8859_1));
  }

  private static final class CountingOutputStream extends FilterOutputStream {
    private long count;

    private CountingOutputStream(OutputStream output) {
      super(output);
    }

    private long getCount() {
      return count;
    }

    @Override
    public void write(int value) throws IOException {
      out.write(value);
      count++;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      out.write(bytes, offset, length);
      count += length;
    }
  }
}
