package problem;

import java.util.Random;

public class Parameters {
	
	public static double surveyTime;
	public static double maximalCycleTime;
	public static double alpha; // control parameter of objective function
	public static Random rand;
	
	public static final double S_EMAX = 10800;	// sensor energy capacity
	public static final double S_EMIN = 540;	// sensor minimum energy level
	public static final double MIN_CHARGING = 0;

	public static final int MAX_EVAL = 25000;	// number of evaluations
	public static final int RCL_SIZE = 10;
	
	public static final int T_POP_SIZE = 100;	// population size
	public static final int T_GENERATIONS = 50;	// max number of generation
	public static final double T_MUTATION_RATIO = 0.05;	// mutation ration
}
