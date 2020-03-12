import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.learning.BackPropagation;

public class Test {
	ArrayList<ArrayList<Integer>> concept_paths;
	BuildModel model;
	Scanner in;
	NeuralNetwork<BackPropagation> nn;
	
	final int yes = 1;
	final int no = 0;


	private void initialise() throws IOException {
		model = new BuildModel();
		model.test_run();
	}

	
	
	public Test() throws IOException {
		initialise();
	}
	
	public static void main(String[] args) throws IOException {
		Test test = new Test();
	
		
	}

}