package main;

import java.io.IOException;
import java.util.Random;

import chargingpath.Solution;
import chargingpath.Solver;
import element.Charger;
import problem.Parameters;
import problem.Problem;

public class MainTest {

	public static void main(String[] arg) throws IOException {

		String input = "data\\input\\grid2\\base_station_(250.0, 250.0)\\gr100_01_simulated.txt";
		Parameters.surveyTime = 72000;
		Parameters.alpha = 0.5;
		Parameters.maximalCycleTime = 25000;

		Problem.reset();
		Problem.loadData(input, 1.5);
		Problem.charger = new Charger(108000, 5, 1, 5);
		Parameters.rand = new Random(0);

		while (Problem.accTime < Parameters.surveyTime) {
			Solution result = Solver.solve();

			double[] chargingTime = new double[Problem.sensors.size()];
			for (int j = 0; j < chargingTime.length; j++) {
				chargingTime[j] = result.getTimeRate()[j] * result.getChargingTimeUB();
			}

			Problem.logSolution(result.getPath(), chargingTime);
			Problem.addCycleSolution(result.getPath(), chargingTime);

			break; // run only 1 charging cycle, comment this line to running for multiple cycles
		}

		System.out.println(Problem.deadInT.size());

	}
}
