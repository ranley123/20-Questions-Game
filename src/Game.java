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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
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
	
	TreeMap<Integer, Integer> id_answer_map;
	ArrayList<Integer> available_questions;
	/**
	 * read questions from question databases into question_list
	 * @param connection
	 */
	private void read_questions_from_db(Connection connection) {
		Statement statement;
		question_list = new ArrayList<>();
		available_questions = new ArrayList<>();
		
		try {
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM question");

			while (resultSet.next()) {
				// extracts question column from the current instance
				String question = resultSet.getString(2);
				question_list.add(question);
			}
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// set up the config
		Config.input_units_num = question_list.size();
		
		// initialise available question list
		for (int i = 0; i < question_list.size(); i++) {
			available_questions.add(i);
		}
	}
	
	/**
	 * returns a random question id
	 * @return 
	 */
	private int next_question_id() {
		int res = -1;
		Random random = new Random();
		int index = random.nextInt(available_questions.size());
		res = available_questions.get(index);
		available_questions.remove(index);
		return res;
	}

	/**
	 * checks if the input is valid
	 * @param input
	 * @return
	 */
	private boolean is_valid_input(int input) {
		return (input == yes || input == no);
	}

	/**
	 * starts to ask question one by one and gets answers
	 */
	private void prompt_questions() {
		answer_list = new ArrayList<>();
		wrong_guess = new ArrayList<>();
		id_answer_map = new TreeMap<>();
		ArrayList<Double> guess_list;

		for (int i = 0; i < question_list.size(); i++) {
			guess_list = new ArrayList<>();

			// get answer
			int answer = -1;
			int index = next_question_id();
			do {
				System.out.println("Q(" + (i + 1) + ")" + question_list.get(index));
				System.out.println("Please enter 0 for no, 1 for yes !");
				String input = in.next();
				try{
					answer = Integer.parseInt(input);
					// is an integer!
				} catch (NumberFormatException e) {
					System.out.println("Please enter 0 or 1!");
					answer = -1;
				}
			} while(!is_valid_input(answer));
			
			id_answer_map.put(index, answer);
			
			// add answer to the answer list
			Collection<Integer> values = id_answer_map.values();
			answer_list = new ArrayList<>(values);
//			System.out.println(id_answer_map);
			// starts to fill guess_list
			// answers so far to be filled
			for(int j = 0; j <= i; j++) {
				guess_list.add(Double.valueOf(answer_list.get(j)));
			}
			// maybe state for rest of them
			for(int j = i + 1; j < question_list.size(); j++) {
				guess_list.add(0.5);
			}

			// predicts the output based on current answers
			double[] guess_output = model.predict(guess_list);
			Map<String, Double> guess_sorted_map = sort_output(guess_output);
			Entry<String, Double> entry = guess_sorted_map.entrySet().iterator().next();

			if(!wrong_guess.contains(entry.getKey()) && entry.getValue() > Config.threshold) {
				System.out.println("You are thinking of: " + entry.getKey());
				System.out.println("Please enter 0 for no, 1 for yes !");
				
				// get valid answer
				int temp = -1;
				do {
					String input = in.next();
					try{
						temp = Integer.parseInt(input);
					} catch (NumberFormatException e) {
						System.out.println("Please enter 0 or 1!");
						temp = -1;
					}
				} while(!is_valid_input(temp));
				
				// process answer
				if (temp == yes) {
					end = true;
					System.out.println("Thank you for playing the game!");
					return;
				}
				else {
					// add into the wrong_guess list so the next time it would be asked
					wrong_guess.add(entry.getKey()); 
				}
			}
		}
		Config.answer_list = answer_list;
	}

	/**
	 * if the game cannot guess, then users can add new concepts and new questions
	 */
	private void ask_for_new_concept() {
		// ask for concept
		in = new Scanner(System.in);
		System.out.println("You win. What is your concept?");
		String new_concept = in.next();

		// ask for new question
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
	
	/**
	 * executes the game workflow
	 * @throws SQLException
	 */
	public void run() throws SQLException {
		in = new Scanner(System.in);

		try {
			// set up or initialise databases
			init_db();
			read_questions_from_db(question_connection);
			question_connection.close();
			concept_connection.close();
			
			// prepare a neural network model
			model = new BuildModel();
			
			// ask players questions to get answer path and early guesses
			prompt_questions();
			
			// if early guesses all failed
			if(!end) {
				// from List<Integer> to List<Double>
				ArrayList<Double> answer_formatted = new ArrayList<>();
				for(Integer i: answer_list) {
					answer_formatted.add(Double.valueOf(i));
				}
				// get outputs and sort it to get maximum likely concepts
				double[] outputs = model.predict(answer_formatted);
				Map<String, Double> sorted_map = sort_output(outputs);
				
				// iterate over the ordered map
				for (Map.Entry<String, Double> entry: sorted_map.entrySet()) {
					// if the guess is confident enough, ask for new concepts and questions
					if(entry.getValue() <= Config.threshold) {
						ask_for_new_concept();
						break;
					}
					// if the current concept is already guessed and failed
					if(wrong_guess.contains(entry.getKey()))
						continue;

					System.out.println("You are think of: " + entry.getKey());
					System.out.println("Please enter 1 for yes, 0 for no");
					// get a valid answer
					int answer = -1;
					do {
						String input = in.next();
						try{
							answer = Integer.parseInt(input);
						} catch (NumberFormatException e) {
							System.out.println("Please enter 0 or 1!");
							answer = -1;
						}
					} while(!is_valid_input(answer));
					
					// process answer
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
			in.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			if (concept_connection != null) 
				concept_connection.close();
			if (question_connection != null)
				question_connection.close();
		}
	}
	
	/**
	 * orders maps by values
	 * @param outputs - the outputs from model
	 * @return a map ordered by value (descending)
	 */
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
	
	/**
	 * initialises two tables: question table and concept table if not existing
	 * @throws SQLException
	 */
	private void init_db() throws SQLException{
		concept_connection = null;
		question_connection = null;
		try {
			// connection 
			String concept_dbUrl = "jdbc:sqlite:concept";
			String question_dbUrl = "jdbc:sqlite:question";
			concept_connection = DriverManager.getConnection(concept_dbUrl);
			question_connection = DriverManager.getConnection(question_dbUrl);
		}
		catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		try {
			// if no existing question table then initialises one
			Statement statement = question_connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM question");

		}
		catch(SQLException e) {
			init_question_table(question_connection);
		}

		try {
			// if no existing concept table then initialise one
			Statement statement = concept_connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM concept");
		}
		catch(SQLException e) {
			init_concept_table(concept_connection);
		}

	}
	
	/**
	 * reads data.csv into concept table
	 * @param connection
	 */
	private void init_concept_table(Connection connection){
		Statement statement;
		try {
			// create a concept table
			statement = connection.createStatement();
			statement.executeUpdate("DROP TABLE IF EXISTS concept");
			statement.executeUpdate("CREATE TABLE concept (name VARCHAR(100), path VARCHAR(100), encoding VARCHAR(100))");
			statement.close();
			
			// read data.csv
			InputStream in = getClass().getResourceAsStream(Config.data_filepath); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			PreparedStatement prepared_statement = connection.prepareStatement("INSERT INTO concept VALUES(?,?,?)");
			
			// set up each instance
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
	
	/**
	 * reads Questions.txt into question database with a proper id assigned.
	 * @param connection
	 */
	private void init_question_table(Connection connection) {
		Statement statement;
		try {
			// create a table
			statement = connection.createStatement();
			statement.executeUpdate("DROP TABLE IF EXISTS question");
			statement.executeUpdate("CREATE TABLE question (id int, question VARCHAR(100))");
			statement.close();
			
			// read Questions.txt
			InputStream in = getClass().getResourceAsStream(Config.question_filepath); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			PreparedStatement prepared_statement = connection.prepareStatement("INSERT INTO question VALUES(?,?)");
			int index = 1;
			
			// set up each instance
			while ((line = reader.readLine()) != null) {
				prepared_statement.setInt(1, index); // id
				prepared_statement.setString(2, line); // question content
				index++;
				prepared_statement.executeUpdate();
			}

			reader.close();
			prepared_statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * For ordering maps by values
 *
 */
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