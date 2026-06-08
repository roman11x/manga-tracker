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

    public static void render(String imagePath, int col, int row, int widthCells, int heightCells) throws IOException {

        Path path = imagePath.startsWith("file:")
                ? Path.of(URI.create(imagePath))
                : Path.of(imagePath);

        BufferedImage image = ImageIO.read(path.toFile());

        if (image == null) {
            return;
        }

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
