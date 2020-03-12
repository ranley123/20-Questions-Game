import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
	Connection concept_connection; // connection to concept database
	Connection question_connection; // connection to question database
	
	/**
	 * if a model exists then loads it. Otherwise creates a model, trains it, and saves it to a file
	 * @return a usable model
	 * @throws IOException
	 */
	private NeuralNetwork<BackPropagation> load_or_create_model() throws IOException {
		File model_file = new File(Config.model_filepath);
		build_config_from_db();
		if (model_file.exists()) {
			nn = NeuralNetwork.load(Config.model_filepath);
		}
		else {
			nn = initialise_NN();
			DataSet training_set = build_dataset_from_db();
			System.out.println("Training starts ...");
			//			System.out.println(training_set.getInputSize());
			//			System.out.println(nn.getInputsCount());
			nn.learn(training_set);
			System.out.println("Training completed.");
			nn.save(Config.model_filepath);
		}
		return nn;
	}
	
	public void test_run() {
		try {
//			test_hidden_units(nn);
			test_learning_rate(nn);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * computes the mse for an input for testing
	 * @param input
	 * @param target
	 * @return
	 */
	private double get_trial_mse(double[] input, double[] target) {
		DataSetRow row = new DataSetRow(input);

		nn.setInput(row.getInput());
		nn.calculate();
		double[] output = nn.getOutput();
		double mse = 0;
		for(int i = 0; i < target.length; i++) {
			mse += (target[i] - output[i]) * (target[i] - output[i]);
		}
		return Math.sqrt(mse);
	}
	
	/**
	 * iterates over possible hidden units to see the total network error, for testing
	 * @param nn
	 * @throws IOException
	 */
	public void test_hidden_units(NeuralNetwork<BackPropagation> nn) throws IOException{
		double[] input = {0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0};
		double[] target = {0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		build_config_from_db();
		nn = load_or_create_model();
		DataSet training_set = build_dataset_from_db();
		for(int i = 2; i <= 16; i++) {
			Config.hidden_units_num = i;
			nn.learn(training_set);
			double total_error = get_total_error(nn);
			System.out.println();
			System.out.println("hidden units: " + i + " total error: " + total_error);
			double mse = get_trial_mse(input, target);
			System.out.println("trial mse: " + mse);
			System.out.println("ratio (trial mse/total error): " + (mse/total_error) * 100 + "%");
		}
	}
	
	public void test_learning_rate(NeuralNetwork<BackPropagation> nn) throws IOException {
		double[] input = {0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0};
		double[] target = {0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		build_config_from_db();
		nn = load_or_create_model();
		DataSet training_set = build_dataset_from_db();
		for(double i = 0.05; i < 1; i = i + 0.05) {
			nn.getLearningRule().setLearningRate(i);
			nn.learn(training_set);
			double total_error = get_total_error(nn);
			System.out.println();
			System.out.println("learning rates: " + i + " total error: " + total_error);
			double mse = get_trial_mse(input, target);
			System.out.println("trial mse: " + mse);
			System.out.println("ratio (trial mse/total error): " + (mse/total_error) * 100 + "%");
		}
	}

	public double get_total_error(NeuralNetwork<BackPropagation> nn) {
		if(nn != null)
			return nn.getLearningRule().getTotalNetworkError();
		else
			return -1; 
	}

	/**
	 * prints out the question database
	 * @param connection to the question database
	 */
	public void print_question_table(Connection connection) {
		Statement statement;
		try {
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM question");

			System.out.println("\ncontents of table:");
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				String question = resultSet.getString(2);

				System.out.println("id: " + id);
				System.out.println("question: " + question);
				System.out.println();
			}
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * prints out concept database for further debugging
	 * @param connection
	 * @throws SQLException
	 */
	public void print_concept_table(Connection connection){
		Statement statement;
		try {
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT * FROM concept");
			
			System.out.println("\ncontents of table:");
			while (resultSet.next()) {
				String name = resultSet.getString(1);
				String path = resultSet.getString(2);
				String encoding = resultSet.getString(3);
				
				System.out.println("name: " + name);
				System.out.println("path: " + path);
				System.out.println("encoding: " + encoding);
				System.out.println();
			}
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * adds new concept and a new question to databases
	 * @param new_concept 	- the name of the new concept
	 * @param new_question 	- the new question
	 * @throws SQLException
	 */
	public void add_new_concept(String new_concept, String new_question) throws SQLException {
		new_concept = new_concept.toLowerCase();
		
		// if the new concept path already exists
		if (concept_exists(new_concept)) {
			System.out.println("concept already exists!");
			return;
		}
		else {
			ArrayList<Integer> answer_list = Config.answer_list;
			answer_list.add(1); // since the new concept has the feature from the question
			
			// update the config
			Config.answer_list = answer_list;
			Config.concept_labels.add(new_concept);
			Config.concept_paths.add(answer_list);
			Config.input_units_num += 1;
			Config.output_units_num += 1;
			Config.hidden_units_num = (3/2) * (Config.input_units_num + Config.output_units_num);
			
			// starts to adding new information to databases
			System.out.println("adding new information to database......");
			update_question_db(new_question);
			update_concept_db(new_concept);
			System.out.println("adding successful!");
			
			// retrain starts
			retrain();
			
			// connections close
			question_connection.close();
			concept_connection.close();
		}
	}
	
	/**
	 * adds new concepts to database, also modifies other concepts to fit new input size and new output size
	 * @param concept_label - the name of the new concept
	 */
	private void update_concept_db(String concept_label) {
		Statement statement;
		try {
			statement = concept_connection.createStatement();
			ResultSet result_set = statement.executeQuery("SELECT * FROM concept");
			while(result_set.next()) {
				String label = result_set.getString("name");
				String path = result_set.getString("path");
				path += " 0"; // default answer to the new question as "no"
				String encoding = result_set.getString("encoding");
				encoding += " 0";
				String query = "update concept set path = ?, encoding = ? where name = ?";
				
				// update the current instance
				PreparedStatement prepare_statement = concept_connection.prepareStatement(query);
				prepare_statement.setString(1, path);
				prepare_statement.setString(2, encoding);
				prepare_statement.setString(3, label);
				prepare_statement.executeUpdate();
				prepare_statement.close();
			}
			
			// insert new concept instance
			PreparedStatement prepared_statement = concept_connection.prepareStatement("INSERT INTO concept VALUES(?,?,?)");
			String answer_path = "";
			for(Integer i: Config.answer_list) {
				answer_path += i + " ";
			}
			answer_path = answer_path.trim();
			String encoding = "";
			int num_of_concepts = Config.concept_labels.size();
			for(int i = 0; i < num_of_concepts - 1; i++) {
				encoding += "0 ";
			}
			encoding += "1";
			
			// set up the statement
			prepared_statement.setString(1, concept_label);
			prepared_statement.setString(2, answer_path);
			prepared_statement.setString(3, encoding);
			prepared_statement.executeUpdate();
			prepared_statement.close();

		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * adds new question
	 * @param question
	 */
	private void update_question_db(String question) {
		try {
			int new_id = Config.answer_list.size() + 1; // add new question id
			PreparedStatement prepared_statement = question_connection.prepareStatement("INSERT INTO question VALUES(?,?)");
			prepared_statement.setInt(1, new_id);
			prepared_statement.setString(2, question);
			prepared_statement.executeUpdate();
			prepared_statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * retrains the model based on new databases
	 */
	private void retrain() {
		initialise_NN();
		DataSet training_set = build_dataset_from_db();
		System.out.println("Retraining starts ...");
		nn.learn(training_set);
		System.out.println("Retraining completed.");

		nn.save(Config.model_filepath);
	}
	
	/**
	 * check if a given answer path already exists
	 * @param answer_list
	 * @return
	 */
	private boolean concept_exists(String new_concept) {
		return (Config.concept_labels.contains(new_concept));
	}
	
	/**
	 * builds config based on concept table
	 */
	private void build_config_from_db() {
		ArrayList<String> concept_labels = new ArrayList<>();
		ArrayList<ArrayList<Integer>> concept_paths = new ArrayList<>();
		Statement statement;
		
		try {
			statement = concept_connection.createStatement();
			ResultSet result_set = statement.executeQuery("SELECT * FROM concept");
			while(result_set.next()) {
				String label = result_set.getString("name");
				String path = result_set.getString("path");
				ArrayList<Integer> path_list = new ArrayList<>();
				concept_labels.add(label);

				for(String c: path.split(" ")) {
					path_list.add(Integer.parseInt(c));
				}
				concept_paths.add(path_list);
			}
			Config.concept_labels = concept_labels;
			Config.concept_paths = concept_paths;
			Config.output_units_num = concept_labels.size();
			Config.hidden_units_num = (3/2) * (Config.input_units_num + Config.output_units_num);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * builds data set from databases
	 * @return
	 */
	private DataSet build_dataset_from_db() {
		DataSet training_set = new DataSet(Config.input_units_num, Config.output_units_num);
		Statement statement;
		try {
			// extract all information from concept databases
			statement = concept_connection.createStatement();
			ResultSet result_set = statement.executeQuery("SELECT * FROM concept");
			while(result_set.next()) {
				String input = result_set.getString("path"); // answer path is the input
				String output = result_set.getString("encoding"); // encoding is the output
				ArrayList<Double> inputs = parse_in_outputs(input);
				ArrayList<Double> outputs = parse_in_outputs(output);
				training_set.addRow(new DataSetRow(inputs, outputs));
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return training_set;
	}
	
	/**
	 * parses a string to a list of Double for dataset training
	 * @param input
	 * @return
	 */
	private ArrayList<Double> parse_in_outputs(String input){
		String[] inputs = input.split(" ");
		ArrayList<Double> res = new ArrayList<>();
		for(String c: inputs) {
			res.add(Double.parseDouble(c));
		}
		return res;
	}
	
	/**
	 * set up basic configuration of the neural network model
	 * @return a neural network model 
	 */
	private NeuralNetwork<BackPropagation> initialise_NN() {
		nn = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, Config.input_units_num, Config.hidden_units_num, Config.output_units_num);
		nn.setLearningRule(new MomentumBackpropagation());
		MomentumBackpropagation learning_rule = (MomentumBackpropagation) nn.getLearningRule();
		learning_rule.setLearningRate(0.5);
		learning_rule.setMaxError(0.01);
		return nn;
	}
	
	/**
	 * calculates the output based on the input via the model
	 * @param answer 	- given the answer list, obtain the output
	 * @return 			- the output
	 */
	public double[] predict(ArrayList<Double> answer) {
		DataSetRow row = new DataSetRow(answer);
		nn.setInput(row.getInput());
		nn.calculate();
		double[] output = nn.getOutput();
		return output;
	}
	
	/**
	 * the constructor to initialise
	 */
	public BuildModel() {
		try {
			String concept_dbUrl = "jdbc:sqlite:concept";
			String question_dbUrl = "jdbc:sqlite:question";
			concept_connection = DriverManager.getConnection(concept_dbUrl);
			question_connection = DriverManager.getConnection(question_dbUrl);

			nn = load_or_create_model();
		}
		catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}