import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Game {
	String filepath;
	BufferedReader reader;
	ArrayList<String> question_list;
	
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
			System.out.println("debug: " + question);
		}
	}

	private void initialise() throws IOException {
		try{
			question_list = new ArrayList<>();
			reader  = new BufferedReader(new FileReader(filepath));
			read_questions(question_list, reader);
		}
		catch (FileNotFoundException e){
			e.getMessage();
		}
		catch (IOException i) {
			i.getMessage();
		}

	}

	public Game(String filepath) throws IOException {
		this.filepath = filepath;
		initialise();
	}

}
