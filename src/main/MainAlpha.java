package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import chargingpath.Solution;
import element.Charger;
import problem.Parameters;
import problem.Problem;

public class MainAlpha {

	public static void main(String[] arg) throws IOException {
		String args[] = new String[] { "data\\input\\grid2\\base_station_(250.0, 250.0)\\gr100", "data\\output", "n100", "-1", "1.5", "250000", "5"};
		String file[] = new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "010" };

		double alpha[] = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		double pm = 1;
		double v = 5;
		int rep = 10;
		double emc = 108000;
		Parameters.surveyTime = 72000;
		double xp = Double.parseDouble(args[4]);
		Parameters.maximalCycleTime = Double.parseDouble(args[5]);
		double u = Double.parseDouble(args[6]);

		int dead[][][] = new int[alpha.length][file.length][rep];
		for (int k = 0; k < file.length; k++) {
			for (int i = 0; i < alpha.length; i++) {
				Parameters.alpha = alpha[i];
				for (int iter = 0; iter < rep; iter++) {
					Problem.reset();
					Problem.loadData(args[0] + "_" + file[k] + "_simulated.txt", xp);
					Problem.charger = new Charger(emc, v, pm, u);
					Parameters.rand = new Random(iter);

					System.out.println("PROGRAM_RUNNING::SEED_" + iter + "::" + args[0] + "_" + file[k]);
					while (Problem.accTime < Parameters.surveyTime) {
						Solution result = chargingpath.Solver.solve();
						double[] chargingTime = new double[Problem.sensors.size()];
						for (int j = 0; j < chargingTime.length; j++) {
							chargingTime[j] = result.getTimeRate()[j] * result.getChargingTimeUB();
						}

//						Problem.logSolution(result.getPath(), chargingTime);
						Problem.addCycleSolution(result.getPath(), chargingTime);
					}

					dead[i][k][iter] = Problem.deadInT.size();
				}
			}
		}

		FileWriter fwd = new FileWriter(new File(args[1] + "/result.txt"), true);
		BufferedWriter bwd = new BufferedWriter(fwd);
		PrintWriter outd = new PrintWriter(bwd);

		for (int i = 0; i < alpha.length; i++) {
			for (int k = 0; k < file.length; k++) {
				outd.print(args[2] + "_" + file[k] + ".txt" + "\t" + Problem.deployedSensors + "\t" + alpha[i] + "\t"
						+ alpha[i] + "\t");
				double mean = 0;
				double best = 100.0;

				double deadRate[] = new double[rep];
				for (int j = 0; j < rep; j++) {
					outd.print(dead[i][k][j] + "\t");
				}

				for (int j = 0; j < rep; j++) {
					deadRate[j] = 100.0 * dead[i][k][j] / Problem.deployedSensors;
					outd.print(deadRate[j] + "\t");

					best = best > deadRate[j] ? deadRate[j] : best;
					mean += deadRate[j];
				}

				mean /= rep;
				outd.print(best + "\t" + mean + "\t");

				double std = 0;
				for (int j = 0; j < rep; j++) {
					std += (mean - deadRate[j]) * (mean - deadRate[j]);
				}
				std = Math.sqrt(std / rep);
				outd.print(std);
				outd.println();
			}
		}

		outd.close();
	}
}
