import java.io.IOException;
import java.sql.SQLException;

public class Main {
	public static void main(String[] args) throws IOException, SQLException {
		System.out.println("This is Stage 2");
		Game game = new Game();
		game.run();
	}
}