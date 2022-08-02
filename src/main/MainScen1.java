package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import chargingpath.Solution;
import chargingpath.Solver;
import element.Charger;
import problem.Parameters;
import problem.Problem;

public class MainScen1 {

	public static void main(String[] arg) throws IOException {
		String args[] = new String[] { "data\\input\\grid2\\base_station_(250.0, 250.0)\\gr25", "data\\output", "gr25", "0.5", "1.5",
				"15000", "25" };
		String file[] = new String[] { "01", "02", "03", "04", "05", "06", "07", "08", "09", "010" };

		double v = 5;
		double emc = 108000;
		double pm = 1;
		int rep = 10;
		Parameters.surveyTime = 72000;
		Parameters.alpha = Double.parseDouble(args[3]);
		System.out.println(Parameters.alpha);
		double xp = Double.parseDouble(args[4]);
		Parameters.maximalCycleTime = Double.parseDouble(args[5]);
		double u = Double.parseDouble(args[6]);

		for (int k = 0; k < file.length; k++) {
			int dead[] = new int[rep];
			double runtime[] = new double[rep];
			for (int iter = 0; iter < rep; iter++) {
				Problem.reset();
				Problem.loadData(args[0] + "_" + file[k] + "_simulated.txt", xp);
				Problem.charger = new Charger(emc, v, pm, u);
				Parameters.rand = new Random(iter);
				int count = 0;

				System.out.println("PROGRAM_RUNNING::SEED_" + iter + "::" + args[0] + "_" + file[k]);
				long t0 = System.currentTimeMillis();
				while (Problem.accTime < Parameters.surveyTime) {
					count++;
					Solution result = Solver.solve();
					double[] chargingTime = new double[Problem.sensors.size()];
					for (int j = 0; j < chargingTime.length; j++) {
						chargingTime[j] = result.getTimeRate()[j] * result.getChargingTimeUB();
					}

//					Problem.logSolution(result.getPath(), chargingTime);
					Problem.addCycleSolution(result.getPath(), chargingTime);
					break;
				}

				runtime[iter] = ((System.currentTimeMillis() - t0) / (1.0 * count));
				dead[iter] = Problem.deadInT.size();
				System.out.println(runtime[iter]);
			}

			FileWriter fwd = new FileWriter(new File(args[1] + "/result.txt"), true);
			BufferedWriter bwd = new BufferedWriter(fwd);
			PrintWriter outd = new PrintWriter(bwd);
			outd.print(args[2] + "_" + file[k] + ".txt" + "\t" + Problem.deployedSensors + "\t");

			double mean = 0;
			double best = 100.0;
			double deadRate[] = new double[rep];

			for (int j = 0; j < rep; j++) {
				outd.print(dead[j] + "\t");
			}

			for (int j = 0; j < rep; j++) {
				deadRate[j] = 100.0 * dead[j] / Problem.deployedSensors;
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
			outd.close();

			FileWriter ftime = new FileWriter(new File(args[1] + "/runtime.txt"), true);
			BufferedWriter bwtime = new BufferedWriter(ftime);
			PrintWriter outdtime = new PrintWriter(bwtime);
			outdtime.print(args[2] + "_" + file[k] + ".txt" + "\t" + Problem.deployedSensors + "\t");

			double meantime = 0;
			double besttime = Double.MAX_VALUE;

			for (int j = 0; j < rep; j++) {
				outdtime.print(runtime[j] + "\t");
			}

			for (int j = 0; j < rep; j++) {
				besttime = besttime > runtime[j] ? runtime[j] : besttime;
				meantime += runtime[j];
			}

			meantime /= rep;
			outdtime.print(besttime + "\t" + meantime + "\t");

			outdtime.println();
			outdtime.close();
		}
	}
}
