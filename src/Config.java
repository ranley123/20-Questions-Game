import java.util.ArrayList;

public class Config {
	public static ArrayList<Integer> answer_list;
	public static ArrayList<ArrayList<Integer>> concept_paths;
	public static ArrayList<String> concept_labels;
	public static int input_units_num;
	public static int hidden_units_num = 4;
	public static int output_units_num;
	public static final String model_filepath = "model.nnet";
	public static final String question_filepath = "Questions.txt";
	public static final String data_filepath = "data.csv";
	public static double threshold = 0.5;
}