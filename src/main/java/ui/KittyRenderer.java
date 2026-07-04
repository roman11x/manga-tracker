package ui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;

public class KittyRenderer {

    // check if the user is using a terminal that supports Kitty
    public static boolean isSupported() {
        String term = System.getenv("TERM");
        String termProgram = System.getenv("TERM_PROGRAM");
        String konsoleVersion = System.getenv("KONSOLE_VERSION"); // check for the KDE terminal

        return "xterm-kitty".equals(term) || "ghostty".equals(termProgram) || konsoleVersion != null;
    }

    // Delete every image the terminal is currently displaying. Lanterna's
    // delta refresh never repaints cells under an image, so without this the
    // cover would keep floating over whatever screen comes next.
    public static void clearImages() {
        System.out.print("\033_Ga=d\033\\");
        System.out.flush();
    }

    /**
     * Renders the image scaled to fit inside a box of maxWidthCells x
     * maxHeightCells without distortion. Terminal cells are roughly twice as
     * tall as they are wide, so the cell box matching the image's aspect
     * ratio is computed with that 2:1 correction before rendering.
     */
    public static void renderFit(String imagePath, int col, int row, int maxWidthCells, int maxHeightCells) throws IOException {
        BufferedImage image = readImage(imagePath);

        if (image == null || maxWidthCells <= 0 || maxHeightCells <= 0) {
            return;
        }

        double aspect = (double) image.getWidth() / image.getHeight();

        int heightCells = maxHeightCells;
        int widthCells = (int) Math.round(heightCells * aspect * 2);

        if (widthCells > maxWidthCells) {
            widthCells = maxWidthCells;
            heightCells = Math.max(1, (int) Math.round(widthCells / (aspect * 2)));
        }

        transmit(image, col, row, Math.max(1, widthCells), heightCells);
    }

    public static void render(String imagePath, int col, int row, int widthCells, int heightCells) throws IOException {
        BufferedImage image = readImage(imagePath);

        if (image == null) {
            return;
        }

        transmit(image, col, row, widthCells, heightCells);
    }

    private static BufferedImage readImage(String imagePath) throws IOException {
        Path path = imagePath.startsWith("file:")
                ? Path.of(URI.create(imagePath))
                : Path.of(imagePath);

        if (!path.toFile().exists()) {
            return null;
        }

        return ImageIO.read(path.toFile());
    }

    private static void transmit(BufferedImage image, int col, int row, int widthCells, int heightCells) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output); // the kitty terminal only supports PNG images

        String b64 = Base64.getEncoder().encodeToString(output.toByteArray());

        // Hide the text cursor while drawing the image.
        System.out.print("\033[?25l");

        // Move cursor to the requested terminal position.
        System.out.printf("\033[%d;%dH", row, col);

        //chunking loop
        int chunkSize = 4096; // the kitty terminal supports a maximum of 4096 bytes per frame
        int offset = 0;     // the current offset in the base64 string
        boolean first = true; // indicates if this is the first chunk
        String parameters = ""; // the first chunk requires display parameters

        while(offset < b64.length()) {
            int end = Math.min(offset + chunkSize, b64.length()); // calculate the end of the current chunk
            String chunk = b64.substring(offset, end); // get the next chunk of data
            boolean more = end < b64.length(); // check if there are more chunks to send

            if (first) {
                 parameters = "a=T,f=100,c=%d,r=%d,m=%d".formatted(widthCells, heightCells, more ? 1 : 0);
                 System.out.printf("\033_G%s;%s\033\\", parameters, chunk);
            }

             else  {
                 parameters = "m=%d".formatted(more ? 1 : 0); // m = 0 no more chunks, m = 1 more chunks
                 System.out.printf("\033_G%s;%s\033\\", parameters, chunk);
            }

            offset = end;
            first = false;
        }

        // Move the text cursor below the image so it does not blink over the cover.
        System.out.printf("\033[%d;%dH", row + heightCells + 1, 1);
        System.out.flush();

    }

}
