import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

public class Game {
	BufferedReader reader;
	ArrayList<String> question_list;
	ArrayList<Integer> answer_list;
	ArrayList<String> wrong_guess;
	BuildModel model;
	Scanner in;
	Connection concept_connection;
	Connection question_connection;
	boolean end = false;

	final int yes = 1;
	final int no = 0;

	private void read_questions_from_db(Connection connection) {
		Statement statement;
		question_list = new ArrayList<>();
		try {
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM question");

			while (resultSet.next()) {
				String question = resultSet.getString(2);
				question_list.add(question);
			}
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Config.input_units_num = question_list.size();
	}

	private boolean is_valid_input(int input) {
		return (input == yes || input == no);
	}

	private void prompt_questions() {
		answer_list = new ArrayList<>();
		wrong_guess = new ArrayList<>();

		System.out.println("Please enter 0 for no, 1 for yes !");
		for (int i = 0; i < question_list.size(); i++) {
			ArrayList<Double> guess_list = new ArrayList<>();
			System.out.println("Q(" + (i + 1) + ")" + question_list.get(i));
			int answer = in.nextInt();
			//			System.out.println(answer);

			if (!is_valid_input(answer)) {
				return;
			}
			answer_list.add(answer);
			for(int j = 0; j <= i; j++) {
				guess_list.add(Double.valueOf(answer_list.get(j)));
			}
			for(int j = i + 1; j < question_list.size(); j++) {
				guess_list.add(0.5);
			}
			double[] guess_output = model.predict(guess_list);
			Map<String, Double> guess_sorted_map = sort_output(guess_output);
			Entry<String, Double> entry = guess_sorted_map.entrySet().iterator().next();
			if(!wrong_guess.contains(entry.getKey()) && entry.getValue() > Config.threshold) {
				System.out.println("You are thinking of: " + entry.getKey());
				System.out.println("Please enter 0 for no, 1 for yes !");
				int temp = in.nextInt();
				if (temp == 1) {
					end = true;
					System.out.println("end");
					return;
				}
				else {
					wrong_guess.add(entry.getKey());
				}
			}
		}
		Config.answer_list = answer_list;
	}

	private void ask_for_new_concept() {
		in = new Scanner(System.in);
		System.out.println("You win. What is your concept?");
		String new_concept = in.next();
		System.out.println("Give me a feature about your concept but my guess dont have");
		in = new Scanner(System.in);
		String new_question = in.nextLine();
		// need to check type here
		System.out.println("I need to learn it now");
		try {
			model.add_new_concept(new_concept, new_question);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		in.close();
	}

	public void run() throws SQLException {
		in = new Scanner(System.in);

		try {
			init_db();
			read_questions_from_db(question_connection);
			question_connection.close();
			concept_connection.close();

			model = new BuildModel();
			prompt_questions();
			if(!end) {
				ArrayList<Double> answer_formatted = new ArrayList<>();
				for(Integer i: answer_list) {
					answer_formatted.add(Double.valueOf(i));
				}
				double[] outputs = model.predict(answer_formatted);
				Map<String, Double> sorted_map = sort_output(outputs);

				for (Map.Entry<String, Double> entry: sorted_map.entrySet()) {
					if(entry.getValue() <= Config.threshold) {
						ask_for_new_concept();
						break;
					}
					if(wrong_guess.contains(entry.getKey()))
						continue;
					System.out.println("You are think of: " + entry.getKey());
					System.out.println("Please enter 1 for yes, 0 for no");
					int answer = in.nextInt();
					if (is_valid_input(answer)) {
						if (answer == yes) {
							System.out.println("Thank you for playing the game!");
							in.close();
							break;
						}
						else {
							wrong_guess.add(entry.getKey());
						}
					}
				}
			}
		in.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	finally {
		if (concept_connection != null) 
			concept_connection.close();
		if (question_connection != null)
			question_connection.close();
	}
}

private Map<String, Double> sort_output(double[] outputs) {
	Map<String, Double> map = new HashMap<>();
	ValueComparator bvc = new ValueComparator(map);
	Map<String, Double> sorted_map = new TreeMap<>(bvc);
	ArrayList<String> concept_labels = Config.concept_labels;
	for (int i = 0; i < outputs.length; i++) {
		map.put(concept_labels.get(i), outputs[i]);
	}
	sorted_map.putAll(map);
	return sorted_map;
}

private void init_db() throws SQLException{
	concept_connection = null;
	question_connection = null;
	try {
		String concept_dbUrl = "jdbc:sqlite:concept";
		String question_dbUrl = "jdbc:sqlite:question";
		concept_connection = DriverManager.getConnection(concept_dbUrl);
		question_connection = DriverManager.getConnection(question_dbUrl);
	}
	catch (SQLException e) {
		System.out.println(e.getMessage());
	}

	try {
		Statement statement = question_connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT * FROM question");

	}
	catch(SQLException e) {
		init_question_table(question_connection);
	}

	try {
		Statement statement = concept_connection.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT * FROM concept");
	}
	catch(SQLException e) {
		init_concept_table(concept_connection);
	}

}

private void init_concept_table(Connection connection){
	Statement statement;
	try {
		statement = connection.createStatement();
		statement.executeUpdate("DROP TABLE IF EXISTS concept");
		statement.executeUpdate("CREATE TABLE concept (name VARCHAR(100), path VARCHAR(100), encoding VARCHAR(100))");
		statement.close();

		InputStream in = getClass().getResourceAsStream(Config.data_filepath); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = "";
		PreparedStatement prepared_statement = connection.prepareStatement("INSERT INTO concept VALUES(?,?,?)");
		while ((line = reader.readLine()) != null) {
			String[] cols = line.split(",");
			prepared_statement.setString(1, cols[2]); // name
			prepared_statement.setString(2, cols[0]); // path
			prepared_statement.setString(3, cols[1]); // encoding
			prepared_statement.executeUpdate();
		}
		reader.close();
		prepared_statement.close();
	} catch (SQLException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
}

private void init_question_table(Connection connection) {
	Statement statement;
	try {
		statement = connection.createStatement();
		statement.executeUpdate("DROP TABLE IF EXISTS question");
		statement.executeUpdate("CREATE TABLE question (id int, question VARCHAR(100))");
		statement.close();

		InputStream in = getClass().getResourceAsStream(Config.question_filepath); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = "";
		PreparedStatement prepared_statement = connection.prepareStatement("INSERT INTO question VALUES(?,?)");
		int index = 1;

		while ((line = reader.readLine()) != null) {
			prepared_statement.setInt(1, index);
			prepared_statement.setString(2, line);
			index++;
			prepared_statement.executeUpdate();
		}

		reader.close();
		prepared_statement.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	catch (IOException e) {
		e.printStackTrace();
	}
}
}

class ValueComparator implements Comparator<String> {
	Map<String, Double> base;

	public ValueComparator(Map<String, Double> base) {
		this.base = base;
	}

	public int compare(String a, String b) {
		if (base.get(a) >= base.get(b)) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}