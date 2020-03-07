import java.sql.SQLException;

public class Main {
	public static void main(String[] args) {
		System.out.println("This is Stage 2");
		Game game = new Game();
		try {
			game.run();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}