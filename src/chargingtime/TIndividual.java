package chargingtime;

import java.util.PriorityQueue;

import chargingpath.Solution;
import element.Charger;
import element.Sensor;
import problem.Parameters;
import problem.Problem;

public class TIndividual implements Comparable {

	private static int counter;
	private int id;

	private double[] time;
	private double fitness;
	private double totalCycleTime;

	public TIndividual() {
		TIndividual.setCounter(TIndividual.getCounter() + 1);
		this.setId(getCounter());
		this.setTime(new double[Problem.sensors.size()]);
		this.setFitness(Double.MAX_VALUE);
	}

	public void initIndividual() {
		double sum = 0;
		for (int i = 0; i < time.length; i++) {
			time[i] = Parameters.rand.nextDouble();
			sum += time[i];
		}

		for (int i = 0; i < time.length - 1; i++) {
			time[i] /= sum;
		}
	}

	public double repairAndCalculateFitness() {

		Charger ch = Problem.charger;
		double maxTc = TSolver.path.getChargingTimeUB();
		double[] arriveTime = new double[time.length];
		Solution path = TSolver.path;
		int dead = 0;
		double sum = 0;

		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path.getPath(0)] / ch.getSpeed();
		for (int i = 0; i < time.length; i++) {
			if (time[i] < Parameters.MIN_CHARGING / ch.getU() / maxTc) {
				time[i] = 0;
			}
			sum += time[i];
		}

		if (sum < 1.0) {
			double[] t = new double[time.length];
			double sumT = 0;
			for (int i = 0; i < time.length; i++) {
				Sensor s = Problem.getSensorById(path.getPath(i));
				double eArrive = s.getE0() - arriveTime[i] * s.getPi();
				double ub;
				if (eArrive <= Parameters.S_EMIN) {
					ub = 0;
				} else {
					ub = (ch.getU() - s.getPi() <= 0) ? 1
							: (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
				}
				
				t[i] = Parameters.rand.nextDouble() * (ub - time[i]);
				sumT += t[i];

				if (i < time.length - 1) {
					arriveTime[i + 1] = arriveTime[i]
							+ Problem.distance[path.getPath(i)][path.getPath(i + 1)] / ch.getSpeed() + time[i] * maxTc;
				}
			}

			if (sumT > 0) {
				for (int i = 0; i < time.length; i++) {
					t[i] = t[i] * (1.0 - sum) / sumT;
					time[i] += t[i];
				}
			}
		} else if (sum > 1.0) {
			for (int i = 0; i < time.length; i++) {
				time[i] /= sum;
			}
		}

		for (int i = 0; i < time.length; i++) {
			Sensor s = Problem.getSensorById(path.getPath(i));
			double eArrive = s.getE0() - arriveTime[i] * s.getPi();
			double ub = (ch.getU() - s.getPi() <= 0) ? 1 : (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
			if (eArrive < s.getEmin() || time[i] < Parameters.MIN_CHARGING / ch.getU() / maxTc) {
				time[i] = 0;
			} else if (time[i] > ub) {
				time[i] = ub;
			}

			if (i < time.length - 1) {
				arriveTime[i + 1] = arriveTime[i]
						+ Problem.distance[path.getPath(i)][path.getPath(i + 1)] / ch.getSpeed() + time[i] * maxTc;
			}
		}

		totalCycleTime = arriveTime[time.length - 1] + time[time.length - 1] * maxTc
				+ Problem.distance[path.getPath(time.length - 1)][Problem.serviceStation.getId()] / ch.getSpeed();
		double threshold = Problem.accTime + totalCycleTime >= Parameters.surveyTime ? 0 : 600;
		PriorityQueue<Double> deltaE = new PriorityQueue<Double>();

		for (int i = 0; i < time.length; i++) {
			Sensor s = Problem.getSensorById(path.getPath(i));
			double eRemain = s.getE0() - arriveTime[i] * s.getPi();
			double eAfterCharge = eRemain + time[i] * maxTc * (ch.getU() - s.getPi());
			double eAfterRound = s.getE0() - totalCycleTime * s.getPi() + time[i] * maxTc * ch.getU();

			if (eRemain < s.getEmin() - 1e-5
					&& Problem.accTime + (s.getE0() - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				dead++;
			} else if (eAfterRound < s.getEmin() + threshold && Problem.accTime + arriveTime[i] + time[i] * maxTc
					+ Math.max(0, eAfterCharge - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				dead++;
			} else {
				// delta = E_after - E_before
				double delta = time[i] * maxTc * ch.getU() - totalCycleTime * s.getPi();
				if (delta < 0) {
					deltaE.add(delta);
				}
			}
		}

		double maxDec = 0;
		if (deltaE.size() > 0) {
			int k = Math.min(deltaE.size(), Math.max(1, Math.min(10, (int) (0.05 * time.length))));
			for (int i = 0; i < k; i++) {
				maxDec += deltaE.remove();
			}
			maxDec /= (-1.0 * k);
		}

		if (Problem.accTime + totalCycleTime >= Parameters.surveyTime) {
			return dead / (1.0 * time.length);
		} else {
			return Parameters.alpha * dead / (1.0 * time.length)
					+ (1 - Parameters.alpha) * maxDec / (Parameters.S_EMAX - Parameters.S_EMIN);
		}
	}

	public double repairAndCalculateFitness2() {

		Charger ch = Problem.charger;
		double maxTc = TSolver.path.getChargingTimeUB();
		double[] arriveTime = new double[time.length];
		Solution path = TSolver.path;
		int dead = 0;
		double sum = 0;

		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path.getPath(0)] / ch.getSpeed();
		for (int i = 0; i < time.length; i++) {
			Sensor s = Problem.getSensorById(path.getPath(i));
			double eArrive = s.getE0() - arriveTime[i] * s.getPi();
			double ub = (ch.getU() - s.getPi() <= 0) ? 1 : (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
			if (eArrive < s.getEmin() || time[i] < Parameters.MIN_CHARGING / ch.getU() / maxTc) {
				time[i] = 0;
			} else if (time[i] > ub) {
				time[i] = ub;
			}
			sum += time[i];

			if (i < time.length - 1) {
				arriveTime[i + 1] = arriveTime[i]
						+ Problem.distance[path.getPath(i)][path.getPath(i + 1)] / ch.getSpeed() + time[i] * maxTc;
			}
		}

		if (sum < 1) {
			double[] t = new double[time.length];
			double sumT = 0;
			for (int i = 0; i < time.length; i++) {
				Sensor s = Problem.getSensorById(path.getPath(i));
				double eArrive = s.getE0() - arriveTime[i] * s.getPi();
				double ub = (ch.getU() - s.getPi() <= 0) ? 1
						: (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
				if (eArrive <= Parameters.S_EMIN) {
					ub = 0;
				}
				t[i] = Parameters.rand.nextDouble() * (ub - time[i]);
				sumT += t[i];

				if (i < time.length - 1) {
					arriveTime[i + 1] = arriveTime[i]
							+ Problem.distance[path.getPath(i)][path.getPath(i + 1)] / ch.getSpeed() + time[i] * maxTc;
				}
			}

			if (sumT > 0) {
				for (int i = 0; i < time.length; i++) {
					t[i] = t[i] * (1.0 - sum) / sumT;
					time[i] += t[i];
				}
			}
		} else if (sum > 1) {
			for (int i = 0; i < time.length; i++) {
				time[i] /= sum;
			}
		}

		for (int i = 0; i < time.length; i++) {
			Sensor s = Problem.getSensorById(path.getPath(i));
			double eArrive = s.getE0() - arriveTime[i] * s.getPi();
			double ub = (ch.getU() - s.getPi() <= 0) ? 1 : (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
			if (eArrive < s.getEmin() || time[i] < Parameters.MIN_CHARGING / ch.getU() / maxTc) {
				time[i] = 0;
			} else if (time[i] > ub) {
				time[i] = ub;
			}

			if (i < time.length - 1) {
				arriveTime[i + 1] = arriveTime[i]
						+ Problem.distance[path.getPath(i)][path.getPath(i + 1)] / ch.getSpeed() + time[i] * maxTc;
			}
		}

		totalCycleTime = arriveTime[time.length - 1] + time[time.length - 1] * maxTc
				+ Problem.distance[path.getPath(time.length - 1)][Problem.serviceStation.getId()] / ch.getSpeed();
		double threshold = Problem.accTime + totalCycleTime >= Parameters.surveyTime ? 0 : 600;
		PriorityQueue<Double> deltaE = new PriorityQueue<Double>();

		for (int i = 0; i < time.length; i++) {
			Sensor s = Problem.getSensorById(path.getPath(i));
			double eRemain = s.getE0() - arriveTime[i] * s.getPi();
			double eAfterCharge = eRemain + time[i] * maxTc * (ch.getU() - s.getPi());
			double eAfterRound = s.getE0() - totalCycleTime * s.getPi() + time[i] * maxTc * ch.getU();

			if (eRemain < s.getEmin() - 1e-5
					&& Problem.accTime + (s.getE0() - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				dead++;
			} else if (eAfterRound < s.getEmin() + threshold && Problem.accTime + arriveTime[i] + time[i] * maxTc
					+ Math.max(0, eAfterCharge - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				dead++;
			} else {
				// delta = E_after - E_before
				double delta = time[i] * maxTc * ch.getU() - totalCycleTime * s.getPi();
				if (delta < 0) {
					deltaE.add(delta);
				}
			}
		}

		double maxDec = 0;
		if (deltaE.size() > 0) {
			int k = Math.min(deltaE.size(), Math.max(1, Math.min(10, (int) (0.05 * time.length))));
			for (int i = 0; i < k; i++) {
				maxDec += deltaE.remove();
			}
			maxDec /= (-1.0 * k);
		}

		if (Problem.accTime + totalCycleTime >= Parameters.surveyTime) {
			return dead / (1.0 * time.length);
		} else {
			return Parameters.alpha * dead / (1.0 * time.length)
					+ (1 - Parameters.alpha) * maxDec / (Parameters.S_EMAX - Parameters.S_EMIN);
		}
	}

	public static int getCounter() {
		return counter;
	}

	public static void setCounter(int counter) {
		TIndividual.counter = counter;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double[] getTime() {
		return time;
	}

	public double getTime(int index) {
		return time[index];
	}

	public void setTime(int index, double value) {
		time[index] = value;
	}

	public void setTime(double[] time) {
		this.time = time;
	}

	public double getFitness() {
		return fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	public TIndividual clone() {
		TIndividual cpy = new TIndividual();
		cpy.setTime(this.getTime().clone());
		return cpy;
	}

	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return Double.valueOf(this.getFitness()).compareTo(((TIndividual) o).getFitness());
	}

	public double getTotalCycleTime() {
		return totalCycleTime;
	}

	public void setTotalCycleTime(double totalCycleTime) {
		this.totalCycleTime = totalCycleTime;
	}
}
