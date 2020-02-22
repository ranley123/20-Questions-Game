import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Game {
	String filepath;
	BufferedReader reader;
	ArrayList<String> question_list;
	ArrayList<Double> answer_list;
	BuildModel model;
	
	/**
	 * Read questions from file into a list
	 * @param list 		- a list storing all questions
	 * @param reader 	- the BufferedReader to read file
	 * @throws IOException 
	 */
	private void read_questions(ArrayList<String> list, BufferedReader reader) throws IOException {
		String question;
		while ((question = reader.readLine()) != null) {
			question_list.add(question);
//			System.out.println("debug: " + question);
		}
	}
	
	private boolean is_valid_input(double input) {
		return (input <= 1 && input >= 0);
	}
	
	private void prompt_questions() {
		answer_list = new ArrayList<Double>();
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter 0 for no, 1 for yes !");
		for (int i = 0; i < question_list.size(); i++) {
			System.out.println("Q(" + (i + 1) + ")" + question_list.get(i));
			double answer = in.nextDouble();
			if (!is_valid_input(answer)) {
				return;
			}
			answer_list.add(answer);
		}
		in.close();
	}

	private void initialise() throws IOException {
		try{
			question_list = new ArrayList<>();
			reader  = new BufferedReader(new FileReader(filepath));
			read_questions(question_list, reader);
			model = new BuildModel();
		}
		catch (FileNotFoundException e){
			e.getMessage();
		}
		catch (IOException i) {
			i.getMessage();
		}
		reader.close();

	}
	
	public void run() throws IOException {
		prompt_questions();
		model.run(answer_list);
	}

	public Game(String filepath) throws IOException {
		this.filepath = filepath;
		initialise();
	}

}
