import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import java.util.ArrayList;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.learning.LearningRule;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;


public class BuildModel {
	int input_units_num = 8;
	int output_units_num = 8;
	int hidden_units_nums = 4;
	NeuralNetwork<BackPropagation> nn;
	BufferedReader reader;
	DataSet training_set;
	DataSet testing_set;
	ArrayList<String> labels;
	
	String model_filepath = "model.nnet";
	
	
	private NeuralNetwork<BackPropagation> load_or_create_model(NeuralNetwork<BackPropagation> nn) throws IOException {
		File model_file = new File(model_filepath);
		if (model_file.exists()) {
			nn = NeuralNetwork.load(model_filepath);
			
		}
		else {
			initialise_NN();
			
			build_dataset_from_file("./src/data.csv");
			System.out.println("Training starts ...");
			nn.learn(training_set);
			System.out.println("Training completed.");
			
			nn.save(model_filepath);
		}
		return nn;
	}
	
	public void run(ArrayList<Double> answer_list) throws IOException {
		
		nn = load_or_create_model(nn);
		predict(answer_list);
		
	}
	
	private void build_dataset_from_file(String filepath) throws IOException {
		reader = new BufferedReader(new FileReader(filepath));
		String line = "";
		DataSet dataset = new DataSet(input_units_num, output_units_num);
		training_set = new DataSet(input_units_num, output_units_num);
		testing_set = new DataSet(input_units_num, output_units_num);
		labels = new ArrayList<>();
		
		while ((line = reader.readLine()) != null){
			String[] features = line.split(",");
			String input = features[0];
			String output = features[1];
			String label = features[2];
			
			ArrayList<Double> inputs = parse_in_outputs(input);
			ArrayList<Double> outputs = parse_in_outputs(output);
			dataset.addRow(new DataSetRow(inputs, outputs));
			
			labels.add(label);
		}

		training_set = dataset;
		
	}
	
	private ArrayList<Double> parse_in_outputs(String input){
		String[] inputs = input.split(" ");
		ArrayList<Double> res = new ArrayList<>();
		for(String c: inputs) {
			res.add(Double.parseDouble(c));
		}
		return res;
	}

	
	private void initialise_NN() {
		nn = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, input_units_num, hidden_units_nums, output_units_num);
		nn.setLearningRule(new MomentumBackpropagation());
		MomentumBackpropagation learning_rule = (MomentumBackpropagation) nn.getLearningRule();
		learning_rule.setLearningRate(0.1);
		learning_rule.setMaxError(0.01);
	}
	
	private void predict(ArrayList<Double> answer) {
		DataSetRow row = new DataSetRow(answer);
		nn.setInput(row.getInput());
		nn.calculate();
		double[] output = nn.getOutput();
        
        String[] animal_list = new String[] {"lion", "cat", "dog", "bird", "fish", "snake", "sheep", "dolphin"};
        int index = get_max_index(output);
        System.out.println("You are thinking about: " + animal_list[index]);
        
	}
	
	private int get_max_index(double[] output) {
		double max = output[0];
		int idx = 0;
		for (int i = 1; i < output.length; i++) {
			if (output[i] > max) {
				max = output[i];
				idx = i;
			}
		}
		return idx;
	}
//	
//	private void print_result() {
//		
//	}
	
}
