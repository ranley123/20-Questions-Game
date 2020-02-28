import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Game {
	String filepath;
	BufferedReader reader;
	ArrayList<String> question_list;
	ArrayList<Integer> answer_list;
	ArrayList<ArrayList<Integer>> animal_paths;
	BuildModel model;
	Scanner in;
	
	final int yes = 1;
	final int no = 0;
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

	private boolean is_valid_input(int input) {
		return (input <= 1 && input >= 0);
	}

	private void prompt_questions() {
		answer_list = new ArrayList<>();
		System.out.println("Please enter 0 for no, 1 for yes !");
		for (int i = 0; i < question_list.size(); i++) {
			System.out.println("Q(" + (i + 1) + ")" + question_list.get(i));
			int answer = in.nextInt();
			if (!is_valid_input(answer)) {
				return;
			}
			answer_list.add(answer);
		}
		Config.answer_list = answer_list;
	}

	private void initialise() throws IOException {
		try{
			question_list = new ArrayList<>();
			reader  = new BufferedReader(new FileReader(filepath));
			read_questions(question_list, reader);
			model = new BuildModel();
			in = new Scanner(System.in);
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
		double[] outputs = model.run(answer_list);
		predict(outputs);
		in.close();
	}

	private void predict(double[] outputs) {
		Map<String, Double> map = new HashMap<>();
		ValueComparator bvc = new ValueComparator(map);
		Map<String, Double> sorted_map = new TreeMap<>(bvc);
		ArrayList<String> animal_labels = Config.animal_labels;
		for (int i = 0; i < outputs.length; i++) {
			map.put(animal_labels.get(i), outputs[i]);
		}
		sorted_map.putAll(map);
		int counter = 0;
		
		for (Map.Entry<String, Double> entry: sorted_map.entrySet()) {
			if(counter > 2){
				System.out.println("You win. What is your animal?");
				in = new Scanner(System.in);
				String new_animal = in.next();
				// need to check type here
				System.out.println("I need to learn it now");
				model.add_new_animal(new_animal);
				return;
			}
			else{
				counter++;
				System.out.println("You are think of: " + entry.getKey());
				System.out.println("Please enter 1 for yes, 0 for no");
				int answer = in.nextInt();
				if (is_valid_input(answer)) {
					if (answer == yes) {
						System.out.println("Thank you for playing the game!");
						return;
					}
				}
			}
		}
		
		

	}
	
	
	public Game(String filepath) throws IOException {
		this.filepath = filepath;
		initialise();
	}

}

class ValueComparator implements Comparator<String> {
    Map<String, Double> base;

    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with
    // equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
