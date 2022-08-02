package chargingpath;

import java.util.ArrayList;

import chargingtime.TIndividual;
import chargingtime.TSolver;
import problem.Parameters;

public class Solver {

	public static Solution solve() {

		Solution.setCounter(0);
		Solution bestSolution = null;

		int iter = 1;
		double min_fitness = Double.MAX_VALUE;
		while (Solution.getCounter() < Parameters.MAX_EVAL && (bestSolution == null || bestSolution.getFitness() > 0)) {
			// construction phase
			Solution solution = new Solution();

			solution.knearstNeighboursInit(2 + Parameters.rand.nextInt(Parameters.RCL_SIZE - 1));
			solution.setFitness(solution.calculateGreedyFitness());

			// local search phase
			while (Solution.getCounter() < Parameters.MAX_EVAL) {
				Solution bestImpr = null; // best improvement
				double minFitness = solution.getFitness();
				ArrayList<Solution> neighbors = solution
						.getNeighbours(Math.min(30, Parameters.MAX_EVAL - Solution.getCounter()));

				for (Solution nei : neighbors) {
					nei.setFitness(nei.calculateGreedyFitness());
					if (nei.getFitness() < minFitness) {
						minFitness = nei.getFitness();
						bestImpr = nei;
					}
				}

				if (bestImpr != null) {
					solution = bestImpr;
//					System.out.println(Solution.getCounter() + "\t" + solution.getFitness());
				} else {
					break;
				}
			}

			TSolver tsolver = new TSolver(solution);
			TIndividual time = tsolver.solve();

			if (bestSolution == null || bestSolution.getProblemFitness() > time.getFitness()) {
				solution.setTimeRate(time.getTime().clone());
				solution.setProblemFitness(time.getFitness());
				solution.updateChargingResult();
				bestSolution = solution;
			}

			System.out.println("iter = " + iter++ + ", fitness = " + time.getFitness());
			min_fitness = min_fitness > time.getFitness() ? time.getFitness() : min_fitness;

		}
		
		System.out.println("best fitness = " + min_fitness);

		return bestSolution;
	}

}