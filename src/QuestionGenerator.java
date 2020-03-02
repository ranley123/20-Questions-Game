import java.util.ArrayList;
import java.util.TreeMap;

public class QuestionGenerator {
	ArrayList<Integer> available_question_ids;
	ArrayList<Integer> available_concept_ids;
	// map question id to question
	TreeMap<Integer, String> id_question_map = new TreeMap<>();
	// map question id to available concepts id
	TreeMap<Integer, Integer> id_available_concept_map = new TreeMap<>();
	// map concept id to concept
	TreeMap<Integer, String> id_concept_map = new TreeMap<>();
	
	
}
