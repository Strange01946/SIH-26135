package in.gov.sih.sih26135.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

final class LandscapeAssessmentPdf {
  private static final int WIDTH = 842;
  private static final int HEIGHT = 595;
  private static final Color NAVY = new Color(21, 48, 89);
  private static final Color SLATE = new Color(67, 84, 104);
  private static final Color RULE = new Color(211, 221, 233);

  private LandscapeAssessmentPdf() {}

  static byte[] render(String title, String outcome, String name, String registration, List<Field> fields, List<String> marks,
                       String authority, String footer, String warning) {
    // Render through the local font system to preserve names in Indian scripts.
    var image = new BufferedImage(WIDTH * 2, HEIGHT * 2, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    try {
      graphics.scale(2, 2);
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, WIDTH, HEIGHT);
      graphics.setColor(RULE);
      graphics.drawRect(22, 22, WIDTH - 44, HEIGHT - 44);
      graphics.setColor(NAVY);
      graphics.fillRect(22, 22, WIDTH - 44, 5);
      Color accent = switch (outcome) {
        case "PASS" -> new Color(22, 106, 79);
        case "FAIL" -> new Color(152, 48, 48);
        default -> new Color(129, 83, 23);
      };
      text(graphics, "National Skill & Placement Tracking Platform", 48, 63, 570, 13, true, SLATE);
      text(graphics, title, 48, 109, 620, 32, true, NAVY);
      text(graphics, outcome, 678, 109, 116, 24, true, accent);
      line(graphics, 127);
      text(graphics, name, 48, 173, 746, 28, true, NAVY);
      text(graphics, "Registration " + registration, 48, 196, 746, 12, false, SLATE);
      for (int i = 0; i < fields.size(); i++) {
        int left = i % 2 == 0 ? 48 : 428;
        int top = 232 + (i / 2) * 48;
        text(graphics, fields.get(i).label(), left, top, 340, 10, true, SLATE);
        text(graphics, fields.get(i).value(), left, top + 19, 350, 14, false, NAVY);
      }
      line(graphics, 368);
      String[] names = {"THEORY", "PRACTICAL", "VIVA", "TOTAL"};
      graphics.setColor(new Color(244, 247, 251));
      graphics.fillRect(48, 387, 746, 65);
      for (int i = 0; i < names.length; i++) {
        int left = 62 + i * 185;
        text(graphics, names[i], left, 410, 175, 10, true, SLATE);
        text(graphics, marks.get(i), left, 434, 175, 17, true, i == 3 ? accent : NAVY);
      }
      text(graphics, authority, 48, 493, 746, 13, true, NAVY);
      text(graphics, footer, 48, 519, 746, 11, false, SLATE);
      text(graphics, warning, 48, 540, 746, 11, true, new Color(152, 48, 48));
    } finally {
      graphics.dispose();
    }
    try {
      var jpg = new ByteArrayOutputStream();
      if (!ImageIO.write(image, "jpeg", jpg)) throw new IOException("JPEG encoder is unavailable");
      return embed(jpg.toByteArray(), image.getWidth(), image.getHeight(), title + " - " + outcome);
    } catch (IOException error) {
      throw new IllegalStateException("Could not generate the assessment PDF", error);
    }
  }

  private static void line(Graphics2D graphics, int y) {
    graphics.setColor(RULE);
    graphics.drawLine(48, y, 794, y);
  }

  private static void text(Graphics2D graphics, String value, int x, int y, int width, int size, boolean bold, Color color) {
    if (value == null || value.isBlank()) return;
    String content = value.replaceAll("\\s+", " ").strip();
    Font font = new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    graphics.setFont(font);
    while (size > 9 && graphics.getFontMetrics().stringWidth(content) > width) {
      graphics.setFont(font.deriveFont((float) --size));
    }
    if (graphics.getFontMetrics().stringWidth(content) > width) {
      int end = content.length();
      while (end > 0 && graphics.getFontMetrics().stringWidth(content.substring(0, end) + "…") > width) {
        end = content.offsetByCodePoints(end, -1);
      }
      content = content.substring(0, end) + "…";
    }
    graphics.setColor(color);
    graphics.drawString(content, x, y);
  }

  private static byte[] embed(byte[] jpeg, int width, int height, String title) {
    var pdf = new ByteArrayOutputStream();
    var offsets = new ArrayList<Integer>();
    write(pdf, "%PDF-1.4\n");
    object(pdf, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
    object(pdf, offsets, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
    object(pdf, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Contents 4 0 R"
        + " /Resources << /XObject << /PageImage 5 0 R >> >> >>");
    String draw = "q 842 0 0 595 0 0 cm /PageImage Do Q";
    object(pdf, offsets, "<< /Length " + draw.length() + " >>\nstream\n" + draw + "\nendstream");
    offsets.add(pdf.size());
    write(pdf, "5 0 obj\n<< /Type /XObject /Subtype /Image /Width " + width + " /Height " + height
        + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpeg.length + " >>\nstream\n");
    pdf.writeBytes(jpeg);
    write(pdf, "\nendstream\nendobj\n");
    object(pdf, offsets, "<< /Title (" + title + ") >>");
    int xref = pdf.size();
    write(pdf, "xref\n0 7\n0000000000 65535 f \n");
    for (int offset : offsets) write(pdf, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
    write(pdf, "trailer\n<< /Size 7 /Root 1 0 R /Info 6 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
    return pdf.toByteArray();
  }

  private static void object(ByteArrayOutputStream pdf, List<Integer> offsets, String content) {
    offsets.add(pdf.size());
    write(pdf, offsets.size() + " 0 obj\n" + content + "\nendobj\n");
  }

  private static void write(ByteArrayOutputStream pdf, String value) {
    pdf.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
  }

  record Field(String label, String value) {}
}
