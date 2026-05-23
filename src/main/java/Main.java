import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import model.Manga;
import model.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary entry point used for testing the basic Manga model behavior.
 *
 * This class will later be replaced or expanded with the terminal user
 * interface that lets users search for manga, add them to their list,
 * and update their reading progress.
 */
public class Main {

    public static void main(String[] args) {

       /* var list = new ArrayList<Manga>();
        var Dorohedoro = new Manga(1, "Dorohedoro", 10, Status.COMPLETED);
        var MahouSenseiNegima = new Manga(2, "Mahou Sensei Negima", 10, Status.READING);
        var Naruto = new Manga(3, "Naruto", 10, Status.READING);
        var JujutsuKaisen = new Manga(4, "Jujutsu Kaisen", 10, Status.PLAN_TO_READ);
        var Bleach = new Manga(5, "Bleach", 10, Status.READING);
        list.add(Dorohedoro);
        list.add(MahouSenseiNegima);
        list.add(Naruto);
        list.add(JujutsuKaisen);
        list.add(Bleach);

        List<Manga> processedResult = list.stream()
                .filter(manga -> manga.getStatus() == Status.READING)
                .sorted((m1, m2) -> m1.getTitle().compareTo(m2.getTitle()))
                .toList();

        List<Manga> page1 = processedResult.stream()
                .skip(0)
                .limit(2)
                .toList();

        List<Manga> page2 = processedResult.stream()
                .skip(2)
                .limit(2)
                .toList();

        System.out.println("page 1: " + page1.toString());
        System.out.println("page 2: " + page2.toString());
*/


        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Terminal terminal = null;
        try {
            terminal = defaultTerminalFactory.createTerminal();
            terminal.setBackgroundColor(TextColor.ANSI.BLUE);
            terminal.setForegroundColor(TextColor.ANSI.YELLOW);
            terminal.putCharacter('H');
            terminal.putCharacter('e');
            terminal.putCharacter('l');
            terminal.putCharacter('l');
            terminal.putCharacter('o');
            terminal.putCharacter('\n');
            terminal.flush();
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}