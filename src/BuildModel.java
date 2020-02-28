import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import java.util.ArrayList;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;


public class BuildModel {
	NeuralNetwork<BackPropagation> nn;
	BufferedReader reader;
	ArrayList<Integer> answer_list;
	
	private NeuralNetwork<BackPropagation> load_or_create_model(NeuralNetwork<BackPropagation> nn) throws IOException {
		File model_file = new File(Config.model_filepath);
		if (model_file.exists()) {
			nn = NeuralNetwork.load(Config.model_filepath);
		}
		else {
			nn = initialise_NN();
			
			DataSet training_set = build_dataset_from_file("./src/data.csv");
			System.out.println("Training starts ...");
			System.out.println(nn);
			nn.learn(training_set);
			System.out.println("Training completed.");
			
			nn.save(Config.model_filepath);
		}
		return nn;
	}
	
	public double[] run(ArrayList<Integer> answer_list) throws IOException {
		nn = load_or_create_model(nn);
		this.answer_list = answer_list;
		double[] outputs = predict(answer_list);
		return outputs;
	}
	
	public void add_new_animal(String new_animal) {
		new_animal = new_animal.toLowerCase();
		System.out.println("Give me a feature about your animal but my guess dont have");
		
		if (animal_exists(answer_list)) {
			System.out.println("Animal already exists!");
			return;
		}
		else {
			
		}
		
	}
	
	private boolean animal_exists(ArrayList<Integer> answer_list) {
		ArrayList<ArrayList<Integer>> animal_paths = Config.animal_paths;
		for (ArrayList<Integer> path: animal_paths) {
			if(path.equals(answer_list)) {
				return true;
			}
		}
		return false;
	}
	
	private DataSet build_dataset_from_file(String filepath) throws IOException {
		reader = new BufferedReader(new FileReader(filepath));
		String line = "";
		DataSet training_set = new DataSet(Config.input_units_num, Config.output_units_num);
		ArrayList<String> animal_labels = new ArrayList<>();
		ArrayList<ArrayList<Integer>> animal_paths = new ArrayList<>();
		
		while ((line = reader.readLine()) != null){
			String[] features = line.split(",");
			String input = features[0];
			String output = features[1];
			String animal_label = features[2];
			
			ArrayList<Double> inputs = parse_in_outputs(input);
			ArrayList<Double> outputs = parse_in_outputs(output);
			training_set.addRow(new DataSetRow(inputs, outputs));
			
			// set animal_paths
			ArrayList<Integer> path = parse_animal_paths(input);
			animal_paths.add(path);
			
			animal_labels.add(animal_label);
		}
		Config.animal_paths = animal_paths;
		Config.animal_labels = animal_labels;
//		System.out.println(animal_labels.size());
//		write_animals_to_csv(config.get_animal_filepath(), labels);
		return training_set;
	}
	
	private ArrayList<Double> parse_in_outputs(String input){
		String[] inputs = input.split(" ");
		ArrayList<Double> res = new ArrayList<>();
		for(String c: inputs) {
			res.add(Double.parseDouble(c));
		}
		return res;
	}
	
	private ArrayList<Integer> parse_animal_paths(String input){
		String[] inputs = input.split(" ");
		ArrayList<Integer> res = new ArrayList<>();
		for(String c: inputs) {
			res.add(Integer.parseInt(c));
		}
		return res;
	}

	
	private NeuralNetwork<BackPropagation> initialise_NN() {
		nn = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, Config.input_units_num, Config.hidden_units_num, Config.output_units_num);
		nn.setLearningRule(new MomentumBackpropagation());
		MomentumBackpropagation learning_rule = (MomentumBackpropagation) nn.getLearningRule();
		learning_rule.setLearningRate(0.1);
		learning_rule.setMaxError(0.01);
		return nn;
	}
	
	private double[] predict(ArrayList<Integer> answer) {
		ArrayList<Double> answer_formated = new ArrayList<>();
		for (Integer a: answer) {
			answer_formated.add(Double.valueOf(a));
		}
		DataSetRow row = new DataSetRow(answer_formated);
		answer_list = answer;
		
		nn.setInput(row.getInput());
		nn.calculate();
		
		double[] output = nn.getOutput();
        return output;
	}
	
}
