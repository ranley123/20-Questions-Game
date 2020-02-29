import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
	
	private NeuralNetwork<BackPropagation> load_or_create_model(NeuralNetwork<BackPropagation> nn) throws IOException {
		File model_file = new File(Config.model_filepath);
		build_config();
		if (model_file.exists()) {
			nn = NeuralNetwork.load(Config.model_filepath);
		}
		else {
			nn = initialise_NN();
			DataSet training_set = build_dataset_from_file(Config.data_filepath);
			System.out.println("Training starts ...");
			nn.learn(training_set);
			System.out.println("Training completed.");
			
			nn.save(Config.model_filepath);
		}
		return nn;
	}
	
	public double[] run() throws IOException {
		nn = load_or_create_model(nn);
		double[] outputs = predict(Config.answer_list);
		return outputs;
	}
	
	public void add_new_animal(String new_animal, String new_question) {
		new_animal = new_animal.toLowerCase();
		
		if (animal_exists(Config.answer_list)) {
			System.out.println("Animal already exists!");
			return;
		}
		else {
			write_new_to_question(new_question);
			write_new_to_data(new_animal);
			System.out.println("write files!");
			ArrayList<Integer> answer_list = Config.answer_list;
			answer_list.add(1);
			Config.answer_list = answer_list;

			try {
				retrain();				
			}
			catch(IOException e) {
				e.getStackTrace();
			}
		}

//		0 0 1 0 0 0 1 1,1 0 0 0 0 0 0 0,lion
//		0 1 0 0 0 0 0 1,0 1 0 0 0 0 0 0,cat
//		0 1 0 1 0 0 0 1,0 0 1 0 0 0 0 0,dog
//		0 1 0 0 1 0 1 1,0 0 0 1 0 0 0 0,bird
//		1 1 0 1 0 1 1 0,0 0 0 0 1 0 0 0,fish
//		0 0 1 0 0 0 1 0,0 0 0 0 0 1 0 0,snake
//		1 0 0 0 0 0 0 1,0 0 0 0 0 0 1 0,sheep
//		0 0 0 1 0 1 1 1,0 0 0 0 0 0 0 1,dolphin
		
	}
	
	private void write_new_to_data(String animal_label) {
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new FileReader(Config.data_filepath));
			String line = "";
			
			while((line = reader.readLine()) != null) {
				String result = "";
				String[] parts = line.split(",");
				// update path
				String path = parts[0];
				path += " 0,"; 
				result += path;
				// update animal encoder
				String encoder = parts[1];
				encoder += " 0,";
				result += encoder;
				result += parts[2];
				sb.append(result + "\n");
//				System.out.println(result);
			}
			reader.close();
			// add new line of animal
			FileWriter writer = new FileWriter(Config.data_filepath, false);
			ArrayList<Integer> answer_list = Config.answer_list;
			String new_line = "";
			for (Integer answer: answer_list) {
				new_line += " " + answer;
			}
			new_line += " 1,";
			new_line = new_line.trim();
			
			int num_of_animals = Config.animal_labels.size();
			String new_encoding = "";
			for (int i = 0; i < num_of_animals; i++) {
				new_encoding += " 0";
			}
			new_encoding += " 1,";
			new_encoding = new_encoding.trim();
			new_line += new_encoding;
			new_line += animal_label;
			sb.append(new_line);
			
			String new_data_file = sb.toString();
			writer.write(new_data_file);
			writer.close();
		}
		catch (IOException e) {
			e.getMessage();
		}
	}
	
	private void write_new_to_question(String question) {
		try {
			FileWriter writer = new FileWriter(Config.question_filepath, true);
			writer.write(question + "\n");
			writer.close();
		}
		catch(IOException e) {
			e.getMessage();
		}
	}
	
	private void retrain() throws IOException {
		build_config();
		initialise_NN();
		DataSet training_set = build_dataset_from_file(Config.data_filepath);
		System.out.println("Retraining starts ...");
//		System.out.println(training_set.getInputSize());
//		System.out.println(nn.getInputsCount());
//		System.out.println(training_set);
		nn.learn(training_set);
		System.out.println("Retraining completed.");
		
		nn.save(Config.model_filepath);
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
	
	private void build_config() {
		try {
			reader = new BufferedReader(new FileReader(Config.data_filepath));
			ArrayList<String> animal_labels = new ArrayList<>();
			ArrayList<ArrayList<Integer>> animal_paths = new ArrayList<>();
			String line = "";
			int index = 0;
			int input_units_num = 0;
			
			while((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				String path = parts[0];
				String label = parts[2];
				ArrayList<Integer> path_list = new ArrayList<>();
				animal_labels.add(label);
				
				for(String c: path.split(" ")) {
					path_list.add(Integer.parseInt(c));
				}
				if(index == 0) {
					input_units_num = path_list.size();
					index++;
				}
				animal_paths.add(path_list);
			}
			Config.animal_labels = animal_labels;
			Config.animal_paths = animal_paths;
			Config.input_units_num = input_units_num;
			Config.output_units_num = animal_labels.size();
			reader.close();
			
		}
		catch(IOException e) {
			e.getStackTrace();
		}
	}

	
	private DataSet build_dataset_from_file(String filepath) throws IOException {
		reader = new BufferedReader(new FileReader(filepath));
		String line = "";
		DataSet training_set = new DataSet(Config.input_units_num, Config.output_units_num);
		
		while ((line = reader.readLine()) != null){
			String[] features = line.split(",");
			String input = features[0];
			String output = features[1];
			
			ArrayList<Double> inputs = parse_in_outputs(input);
			ArrayList<Double> outputs = parse_in_outputs(output);
			training_set.addRow(new DataSetRow(inputs, outputs));
		}
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
		System.out.println(answer.size());
		System.out.println(nn.getInputsCount());
		DataSetRow row = new DataSetRow(answer_formated);
		
		nn.setInput(row.getInput());
		nn.calculate();
		
		double[] output = nn.getOutput();
        return output;
	}
	
}
