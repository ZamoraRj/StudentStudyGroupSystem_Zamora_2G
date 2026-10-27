import dao.DatabaseConnection;
import ui.MenuUI;

/**
 * Entry point of the Student Study Group Finder System.
 */
public class Main {
    public static void main(String[] args) {
        DatabaseConnection.initialize();
        new MenuUI().start();
    }
}
