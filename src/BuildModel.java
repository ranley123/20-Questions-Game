import java.io.BufferedReader;
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
	BufferedReader reader;
	Connection concept_connection;
	Connection question_connection;

	private NeuralNetwork<BackPropagation> load_or_create_model() throws IOException {
		File model_file = new File(Config.model_filepath);
		build_config_from_db();
		if (model_file.exists()) {
			//			System.out.println("model exists");
			nn = NeuralNetwork.load(Config.model_filepath);
		}
		else {
			//			System.out.println("model not exists");
			nn = initialise_NN();
			DataSet training_set = build_dataset_from_db();
			System.out.println("Training starts ...");
//			System.out.println(training_set.getInputSize());
//			System.out.println(nn.getInputsCount());
			nn.learn(training_set);
			
			System.out.println("Training completed.");

			nn.save(Config.model_filepath);
		}
		System.out.println("total error is: " + get_total_error());
		return nn;
	}

	public void test_hidden_units(NeuralNetwork<BackPropagation> nn) throws IOException{
		build_config_from_db();
		nn = initialise_NN();
		DataSet training_set = build_dataset_from_db();
		for(int i = 2; i <= 8; i++) {
			Config.hidden_units_num = i;
			nn.learn(training_set);
			System.out.println("hidden units: " + i + "total error: " + nn.getLearningRule().getTotalNetworkError());
		}


	}

	public void test_learning_rate(NeuralNetwork<BackPropagation> nn) throws IOException {
		build_config_from_db();
		nn = initialise_NN();
		DataSet training_set = build_dataset_from_db();
		Config.hidden_units_num = 4;
		for(double i = 0.05; i < 0.5; i = i + 0.05) {
			nn.getLearningRule().setLearningRate(i);
			nn.learn(training_set);
			System.out.println("learning rate: " + i + "total error: " + nn.getLearningRule().getTotalNetworkError());
		}
	}

	public void test_max_error(NeuralNetwork<BackPropagation> nn) throws IOException {
		build_config_from_db();
		nn = initialise_NN();
//		DataSet training_set = build_dataset_from_db();
//		for(double i = 0.1; i >= 0.0001; ) {
//
//		}
	}

	public double get_total_error() {
		if(nn != null)
			return nn.getLearningRule().getTotalNetworkError();
		else
			return -1; 
	}


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
	
	
	public void print_concept_table(Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
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
	}

	public void add_new_concept(String new_concept, String new_question) throws SQLException {
		new_concept = new_concept.toLowerCase();

		if (concept_exists(Config.answer_list)) {
			System.out.println("concept already exists!");
			return;
		}
		else {
			ArrayList<Integer> answer_list = Config.answer_list;
			answer_list.add(1);
			
			Config.answer_list = answer_list;
			Config.concept_labels.add(new_concept);
			Config.concept_paths.add(answer_list);
			Config.input_units_num += 1;
			Config.output_units_num += 1;
			
			System.out.println("adding new information to database......");
			update_question_db(new_question);
			update_concept_db(new_concept);
			System.out.println("adding successful!");
//			print_question_table(question_connection);
//			print_concept_table(concept_connection);
			
			retrain();
			question_connection.close();
			concept_connection.close();
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

	private void update_concept_db(String concept_label) {
		Statement statement;
		try {
			statement = concept_connection.createStatement();
			ResultSet result_set = statement.executeQuery("SELECT * FROM concept");
			while(result_set.next()) {
				String label = result_set.getString("name");
				String path = result_set.getString("path");
				path += " 0";
				String encoding = result_set.getString("encoding");
				encoding += " 0";
				String query = "update concept set path = ?, encoding = ? where name = ?";
				
				PreparedStatement prepare_statement = concept_connection.prepareStatement(query);
				prepare_statement.setString(1, path);
				prepare_statement.setString(2, encoding);
				prepare_statement.setString(3, label);
				prepare_statement.executeUpdate();
				prepare_statement.close();
			}
			PreparedStatement prepared_statement = concept_connection.prepareStatement("INSERT INTO concept VALUES(?,?,?)");
			prepared_statement.setString(1, concept_label);
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
			prepared_statement.setString(2, answer_path);
			prepared_statement.setString(3, encoding);
			prepared_statement.executeUpdate();
			prepared_statement.close();
			
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

	private void update_question_db(String question) {
		try {
			int new_id = Config.answer_list.size() + 1;
			PreparedStatement prepared_statement = question_connection.prepareStatement("INSERT INTO question VALUES(?,?)");
			prepared_statement.setInt(1, new_id);
			prepared_statement.setString(2, question);
			prepared_statement.executeUpdate();
			prepared_statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	private void retrain() {
//		build_config_from_db();
		initialise_NN();
		DataSet training_set = build_dataset_from_db();
		System.out.println("Retraining starts ...");
		//		System.out.println(training_set.getInputSize());
		//		System.out.println(nn.getInputsCount());
		//		System.out.println(training_set);
		nn.learn(training_set);
		System.out.println("Retraining completed.");

		nn.save(Config.model_filepath);
	}

	private boolean concept_exists(ArrayList<Integer> answer_list) {
		ArrayList<ArrayList<Integer>> concept_paths = Config.concept_paths;
		for (ArrayList<Integer> path: concept_paths) {
			if(path.equals(answer_list)) {
				return true;
			}
		}
		return false;
	}

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
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private DataSet build_dataset_from_db() {
		DataSet training_set = new DataSet(Config.input_units_num, Config.output_units_num);
		Statement statement;
		try {
			statement = concept_connection.createStatement();
			ResultSet result_set = statement.executeQuery("SELECT * FROM concept");
			while(result_set.next()) {
				String input = result_set.getString("path");
				String output = result_set.getString("encoding");
				ArrayList<Double> inputs = parse_in_outputs(input);
				ArrayList<Double> outputs = parse_in_outputs(output);
				
//				System.out.println(inputs.size());
//				System.out.println(training_set.getInputSize());
				training_set.addRow(new DataSetRow(inputs, outputs));
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
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

	private NeuralNetwork<BackPropagation> initialise_NN() {
		nn = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, Config.input_units_num, Config.hidden_units_num, Config.output_units_num);
		nn.setLearningRule(new MomentumBackpropagation());
		MomentumBackpropagation learning_rule = (MomentumBackpropagation) nn.getLearningRule();
		learning_rule.setLearningRate(0.1);
		learning_rule.setMaxError(0.01);
		learning_rule.setMaxIterations(1000);
		//		nn.setLearningRule(learning_rule);
		return nn;
	}

	public double[] predict(ArrayList<Double> answer) {
//				System.out.println(answer.size());
//				System.out.println(nn.getInputsCount());
		DataSetRow row = new DataSetRow(answer);
		
		nn.setInput(row.getInput());
		nn.calculate();

		double[] output = nn.getOutput();
		return output;
	}

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}