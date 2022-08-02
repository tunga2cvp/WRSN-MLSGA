package problem;

import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import element.Charger;
import element.Node;
import element.Sensor;

public class Problem {

	public static ArrayList<Sensor> sensors;
	public static Charger charger;
	public static Node serviceStation;
	public static double[][] distance;
	public static HashMap<Integer, Sensor> _map;

	public static int maxId;
	public static double minP;
	public static double sumP;
	public static double sumE0;
	public static int deployedSensors;

	public static double accTime;
	public static ArrayList<int[]> path;
	public static ArrayList<double[]> chargingTime;

	public static Set<Integer> deadList;
	public static Set<Integer> deadInT;

	public static void reset() {
		accTime = 0;
		path = new ArrayList<int[]>();
		chargingTime = new ArrayList<double[]>();
		Problem.sensors = new ArrayList<Sensor>();
		_map = new HashMap<Integer, Sensor>();
		deadList = new HashSet<Integer>();
		deadInT = new HashSet<Integer>();
		sumP = 0;
		sumE0 = 0;
	}

	public static void loadData(String fileName, double pmult) {
		try {
			Scanner in = new Scanner(new File(fileName));

			int id = 0;
			Problem.serviceStation = new Node(id, in.nextDouble(), in.nextDouble());
			double pmin = Double.MAX_VALUE;

			while (in.hasNext()) {
				double x = in.nextDouble();
				double y = in.nextDouble();
				double pi = in.nextDouble();
				double e0 = in.nextDouble();
				pi = Math.max(1e-9, pi);
				Sensor sensor = new Sensor(++id, x, y, e0, pi * pmult, Parameters.S_EMAX, Parameters.S_EMIN);
				Problem.sensors.add(sensor);
				_map.put(sensor.getId(), sensor);

				pmin = pi < pmin && pi > 0 ? pi : pmin;
				sumP += pi;
				sumE0 += e0;
			}

			Problem.maxId = id;
			Problem.minP = pmin;
			deployedSensors = sensors.size();

			// calculate distance matrix
			ArrayList<Node> nodes = new ArrayList<Node>();
			nodes.add(Problem.serviceStation);
			nodes.addAll(sensors);
			distance = new double[Problem.maxId + 1][Problem.maxId + 1];
			for (Node p : nodes) {
				for (Node q : nodes) {
					distance[p.getId()][q.getId()] = p.getDistance(q);
				}
			}

			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static double getSumP() {
		return sumP;
	}

	public static double getTotalDistance(int[] path) {
		double result = 0;
		int n = path.length;
		result += distance[path[n - 1]][serviceStation.getId()];
		result += distance[serviceStation.getId()][path[0]];
		for (int i = 1; i < n; i++) {
			result += distance[path[i - 1]][path[i]];
		}
		return result;
	}

	public static double getTotalTimeTravel(int[] path) {
		return Problem.getTotalDistance(path) / charger.getSpeed();
	}

	public static double getMaxTotalChargingTime(int[] path) {
		double totalDistance = Problem.getTotalDistance(path);
		return (charger.getE0() - totalDistance * charger.getPm() / charger.getSpeed()) / charger.getU();
	}

	public static Sensor getSensorById(int id) {
		Sensor s = _map.get(id);
		if (s == null) {
			System.err.println("Error! Sensor id not exists.");
		}
		return s;
	}

	public static void addCycleSolution(int path[], double[] chargingTime) {
		int leng = path.length;
		double[] arriveTime = new double[leng];

		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path[0]] / charger.getSpeed();
		for (int i = 1; i < leng; i++) {
			arriveTime[i] = arriveTime[i - 1] + chargingTime[i - 1]
					+ Problem.distance[path[i - 1]][path[i]] / charger.getSpeed();
		}
		double totalCycleTime = arriveTime[leng - 1] + chargingTime[leng - 1]
				+ Problem.distance[path[leng - 1]][Problem.serviceStation.getId()] / charger.getSpeed();

		sumE0 = 0;
		sumP = 0;
		for (int i = 0; i < leng; i++) {
			Sensor s = Problem.getSensorById(path[i]);
			double eArrive = Math.max(0, s.getE0() - arriveTime[i] * s.getPi());
			double eExtra = chargingTime[i] * charger.getU();
			double eAfterCharge = Math.max(0, eArrive + eExtra - s.getPi() * chargingTime[i]);
			double eAfterRound = Math.max(0, s.getE0() + eExtra - totalCycleTime * s.getPi());

			if (!deadList.contains(s.getId()) && (eArrive < s.getEmin() - 1e-5 || eAfterRound < s.getEmin() - 1e-5)) {
				deadList.add(s.getId());
				sensors.remove(s);

				if (!deadInT.contains(s.getId()) && eArrive < s.getEmin() - 1e-5
						&& accTime + (s.getE0() - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
					deadInT.add(s.getId());
				}

				if (!deadInT.contains(s.getId()) && eAfterRound < s.getEmin() - 1e-5
						&& accTime + arriveTime[i] + chargingTime[i]
								+ Math.max(0, eAfterCharge - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
					deadInT.add(s.getId());
				}
			} else {
				sumE0 += eAfterRound;
				sumP += s.getPi();
			}

			s.setE0(eAfterRound);
		}

		Problem.path.add(path);
		Problem.chargingTime.add(chargingTime);
		Problem.accTime += totalCycleTime;

	}

	public static void logSolution(int[] path, double[] time) {
		int leng = path.length;
		double arriveTime[] = new double[leng];
		ArrayList<Integer> dead = new ArrayList<Integer>();
		double tmove = 0;
		double tc = 0;

		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path[0]] / charger.getSpeed();
		tmove += arriveTime[0];
		for (int i = 1; i < leng; i++) {
			arriveTime[i] = arriveTime[i - 1] + time[i - 1]
					+ Problem.distance[path[i - 1]][path[i]] / charger.getSpeed();
			tmove += Problem.distance[path[i - 1]][path[i]] / charger.getSpeed();
			tc += time[i - 1];
		}
		tmove += Problem.distance[path[leng - 1]][Problem.serviceStation.getId()] / charger.getSpeed();
		tc += time[leng - 1];

		double totalCycleTime = tmove + tc;

		for (int i = 0; i < leng; i++) {
			Sensor s = Problem.getSensorById(path[i]);
			double eArrive = Math.max(0, s.getE0() - arriveTime[i] * s.getPi());
			double eExtra = time[i] * charger.getU();
			double eAfter = Math.max(0, s.getE0() + eExtra - totalCycleTime * s.getPi());
			s.setEArrive(eArrive);
			s.setEExtra(eExtra);
			s.setEAfterCharge(Math.max(0, eArrive + eExtra - s.getPi() * time[i]));
			s.setEAfterRound(eAfter);

			if (!dead.contains(s.getId()) && (eArrive < s.getEmin() - 1e-5 || eAfter < s.getEmin() - 1e-5)) {
				dead.add(s.getId());
			}
		}

		System.out.println("Problem::logSolution: " + new Date());
		System.out.println("Path length: " + path.length);
		System.out.println("Number of dead sensor(s): " + dead.size());
		if (dead.size() > 0) {
			System.out.print("Dead sensor(s): ");
			for (int i = 0; i < dead.size(); i++) {
				System.out.print(dead.get(i) + " ");
			}
			System.out.println();
		}
		System.out.println("Total cycle time: " + totalCycleTime);
		double Et = tmove * charger.getPm();
		System.out.println("Energy travel: " + Et);
		double Ec = tc * charger.getU();
		System.out.println("Energy charging: " + Ec);
		System.out.println("Total energy used: " + (Ec + Et));
		System.out.print("Tour: ");
		for (int i = 0; i < path.length; i++) {
			System.out.print(path[i] + " ");
		}
		System.out.print("\nCharging time: ");
		for (int i = 0; i < path.length; i++) {
			System.out.print(time[i] + " ");
		}
		System.out.println();

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		System.out.println("Sensor | P | E_before_charge | E_extra | E_after_charge | E_after_round");
		for (int i = 0; i < path.length; i++) {
			Sensor sensor = Problem.getSensorById(path[i]);
			System.out.print(sensor.getId() + " | ");
			System.out.print(formatter.format(sensor.getPi()) + " | ");
			System.out.print(formatter.format(sensor.getEArrive()) + " | ");
			System.out.print(formatter.format(sensor.getEExtra()) + " | ");
			System.out.print(formatter.format(sensor.getEAfterCharge()) + " | ");
			System.out.println(formatter.format(sensor.getEAfterRound()));
		}
	}

	public static void logSolution(int[] path, double[] time, PrintWriter out) {
		int leng = path.length;
		double arriveTime[] = new double[leng];
		ArrayList<Integer> dead = new ArrayList<Integer>();
		double tmove = 0;
		double tc = 0;

		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path[0]] / charger.getSpeed();
		tmove += arriveTime[0];
		for (int i = 1; i < leng; i++) {
			arriveTime[i] = arriveTime[i - 1] + time[i - 1]
					+ Problem.distance[path[i - 1]][path[i]] / charger.getSpeed();
			tmove += Problem.distance[path[i - 1]][path[i]] / charger.getSpeed();
			tc += time[i - 1];
		}
		tmove += Problem.distance[path[leng - 1]][Problem.serviceStation.getId()] / charger.getSpeed();
		tc += time[leng - 1];

		double totalCycleTime = tmove + tc;

		for (int i = 0; i < leng; i++) {
			Sensor s = Problem.getSensorById(path[i]);
			double eArrive = Math.max(0, s.getE0() - arriveTime[i] * s.getPi());
			double eExtra = time[i] * charger.getU();
			double eAfter = Math.max(0, s.getE0() + eExtra - totalCycleTime * s.getPi());
			s.setEArrive(eArrive);
			s.setEExtra(eExtra);
			s.setEAfterCharge(Math.max(0, eArrive + eExtra - s.getPi() * time[i]));
			s.setEAfterRound(eAfter);

			if (!dead.contains(s.getId()) && (eArrive < s.getEmin() - 1e-5 || eAfter < s.getEmin() - 1e-5)) {
				dead.add(s.getId());
			}
		}

		System.out.println("Problem::logSolution: " + new Date());
		out.println("Problem::logSolution: " + new Date());
		System.out.println("Path length: " + path.length);
		out.println("Path length: " + path.length);
		System.out.println("Number of dead sensor(s): " + dead.size());
		out.println("Number of dead sensor(s): " + dead.size());
		if (dead.size() > 0) {
			System.out.print("Dead sensor(s): ");
			out.print("Dead sensor(s): ");
			for (int i = 0; i < dead.size(); i++) {
				System.out.print(dead.get(i) + " ");
				out.print(dead.get(i) + " ");
			}
			System.out.println();
			out.println();
		}
		System.out.println("Total cycle time: " + totalCycleTime);
		out.println("Total cycle time: " + totalCycleTime);
		double Et = tmove * charger.getPm();
		System.out.println("Energy travel: " + Et);
		out.println("Energy travel: " + Et);
		double Ec = tc * charger.getU();
		System.out.println("Energy charging: " + Ec);
		out.println("Energy charging: " + Ec);
		System.out.println("Total energy used: " + (Ec + Et));
		out.println("Total energy used: " + (Ec + Et));
		System.out.print("Tour: ");
		out.print("Tour: ");
		for (int i = 0; i < path.length; i++) {
			System.out.print(path[i] + " ");
			out.print(path[i] + " ");
		}
		System.out.print("\nCharging time: ");
		out.print("\nCharging time: ");
		for (int i = 0; i < path.length; i++) {
			System.out.print(time[i] + " ");
			out.print(time[i] + " ");
		}
		System.out.println();
		out.println();

		DecimalFormat formatter = new DecimalFormat("#0.0000");
		System.out.println("Sensor | P | E_before_charge | E_extra | E_after_charge | E_after_round");
		out.println("Sensor | P | E_before_charge | E_extra | E_after_charge | E_after_round");
		for (int i = 0; i < path.length; i++) {
			Sensor sensor = Problem.getSensorById(path[i]);
			System.out.print(sensor.getId() + " | ");
			System.out.print(formatter.format(sensor.getPi()) + " | ");
			System.out.print(formatter.format(sensor.getEArrive()) + " | ");
			System.out.print(formatter.format(sensor.getEExtra()) + " | ");
			System.out.print(formatter.format(sensor.getEAfterCharge()) + " | ");
			System.out.println(formatter.format(sensor.getEAfterRound()));

			out.print(sensor.getId() + " | ");
			out.print(formatter.format(sensor.getPi()) + " | ");
			out.print(formatter.format(sensor.getEArrive()) + " | ");
			out.print(formatter.format(sensor.getEExtra()) + " | ");
			out.print(formatter.format(sensor.getEAfterCharge()) + " | ");
			out.println(formatter.format(sensor.getEAfterRound()));
		}
	}

}
