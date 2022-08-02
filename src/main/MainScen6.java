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

public class MainScen6 {

	public static void main(String[] args) throws IOException {
//		String args[] = new String[] { "data\\input\\small\\uniform\\u50", "data\\output", "n100", "0.5", "1.5",
//		"15000", "-1" };
		String file[] = new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "010" };

		double uemc[][] = { { 2, 54000 }, { 5, 81000 }, { 10, 108000 }, { 15, 162000 }, { 20, 216000 },
				{ 25, 324000 } };
		double pm = 1;
		double v = 5;
		int rep = 10;
		Parameters.surveyTime = 72000;
		Parameters.alpha = Double.parseDouble(args[3]);
		double xp = Double.parseDouble(args[4]);
		Parameters.maximalCycleTime = Double.parseDouble(args[5]);

		int dead[][][] = new int[uemc.length][file.length][rep];

		for (int k = 0; k < file.length; k++) {
			for (int i = 0; i < uemc.length; i++) {
				for (int iter = 0; iter < rep; iter++) {
					Problem.reset();
					Problem.loadData(args[0] + "_" + file[k] + "_simulated.txt", xp);
					Problem.charger = new Charger(uemc[i][1], v, pm, uemc[i][0]);
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

		for (int i = 0; i < uemc.length; i++) {
			for (int k = 0; k < file.length; k++) {
				outd.print(args[2] + "_" + file[k] + ".txt" + "\t" + Problem.deployedSensors + "\t" + uemc[i][0] + "\t"
						+ uemc[i][1] + "\t");
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
