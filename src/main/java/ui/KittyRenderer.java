package ui;

public class KittyRenderer {

    // check if the user is using a terminal that supports Kitty
    public static boolean isSupported() {
        String term = System.getenv("TERM");
        String termProgram = System.getenv("TERM_PROGRAM")
        String konsoleVersion = System.getenv("KONSOLE_VERSION"); // check for the KDE terminal

        return "xterm-kitty".equals(term) || "ghostty".equals(termProgram) || konsoleVersion != null;
    }


}
