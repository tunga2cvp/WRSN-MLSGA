package chargingpath;

import java.util.ArrayList;
import java.util.Collections;

import element.Charger;
import element.Sensor;
import problem.Parameters;
import problem.Problem;

public class Solution {

	private static long counter;
	private long id;

	private ArrayList<Integer> path;	// charging path
	private double movingTime;	// MC moving time
	private double fitness;	 // fitness cua charging path

	private double[] timeRate; // chuoi thoi gian sac
	private int deadInT;	//
	private double tcUB; // t charge upper bound
	private double problemFitness; // fitness cua solution cuoi cung
	private double totalCycleTime;

	public Solution() {
		Solution.setCounter(Solution.getCounter() + 1);
		this.setId(Solution.getCounter());
		this.setPath(new ArrayList<Integer>());
		this.setFitness(Double.MAX_VALUE);
	}

	public double calculateGreedyFitness() {
		int n = Problem.sensors.size();
		double[] arriveTime = new double[path.size()];
		arriveTime[0] = 
				Problem.distance[Problem.serviceStation.getId()][path.get(0)] / Problem.charger.getSpeed();
		for (int i = 1; i < n; i++) {
			arriveTime[i] = arriveTime[i - 1]
					+ Problem.distance[path.get(i - 1)][path.get(i)] / Problem.charger.getSpeed();
		}

		double[] tmp = new double[n];
		double f1 = 0, f2 = 0;
		for (int i = 0; i < n; i++) {
			Sensor s = Problem.getSensorById(path.get(i));
			tmp[i] = arriveTime[i] * s.getPi() / s.getE0();
			f1 += tmp[i];
		}

		double mean = f1 / n;
		for (int i = 0; i < n; i++) {
			f2 += Math.abs(mean - tmp[i]);
		}
		return 0.5 * f1 + 0.5 * f2;
	}
	
	public void updateChargingResult() {
		Charger ch = Problem.charger;
		int n = path.size();
		double maxTc = this.getChargingTimeUB();

		double arriveTime[] = new double[path.size()];
		arriveTime[0] = Problem.distance[Problem.serviceStation.getId()][path.get(0)] / ch.getSpeed();
		for (int i = 0; i < n; i++) {
			Sensor s = Problem.getSensorById(path.get(i));
			double eArrive = s.getE0() - arriveTime[i] * s.getPi();
			double ub = (ch.getU() - s.getPi() <= 0) ? 1 : (s.getEmax() - eArrive) / (ch.getU() - s.getPi()) / maxTc;
			if (eArrive < s.getEmin() || timeRate[i] < Parameters.MIN_CHARGING / ch.getU() / maxTc) {
				timeRate[i] = 0;
			} else if (timeRate[i] > ub) {
				timeRate[i] = ub;
			}

			if (i < n - 1) {
				arriveTime[i + 1] = arriveTime[i] + Problem.distance[path.get(i)][path.get(i+1)] / ch.getSpeed()
						+ timeRate[i] * maxTc;
			}
		}

		totalCycleTime = arriveTime[n - 1] + timeRate[n - 1] * maxTc
				+ Problem.distance[path.get(n-1)][Problem.serviceStation.getId()] / ch.getSpeed();
		this.setDeadInT(0);
		for (int i = 0; i < n; i++) {
			Sensor s = Problem.getSensorById(path.get(i));
			double eRemain = s.getE0() - arriveTime[i] * s.getPi();
			double eAfterCharge = eRemain + timeRate[i] * maxTc * (ch.getU() - s.getPi());
			double eAfterRound = s.getE0() - totalCycleTime * s.getPi() + timeRate[i] * maxTc * ch.getU();

			if (eRemain < s.getEmin() - 1e-5
					&& Problem.accTime + (s.getE0() - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				this.setDeadInT(this.getDeadInT()+1);
			} else if (eAfterRound < s.getEmin() && Problem.accTime + arriveTime[i] + timeRate[i] * maxTc
					+ Math.max(0, eAfterCharge - s.getEmin()) / s.getPi() < Parameters.surveyTime) {
				this.setDeadInT(this.getDeadInT()+1);
			} 
		}
	}

	public void randomInit() {
		ArrayList<Sensor> pool = new ArrayList<Sensor>();
		pool.addAll(Problem.sensors);
		while (!pool.isEmpty()) {
			path.add(pool.remove(Parameters.rand.nextInt(pool.size())).getId());
		}
	}

	@SuppressWarnings("unchecked")
	public void lifeTimeBasedInit() {
		ArrayList<Sensor> pool = new ArrayList<Sensor>();
		pool.addAll(Problem.sensors);
		Collections.sort(pool);
		for (Sensor s : pool) {
			path.add(s.getId());
		}
	}

	public void knearstNeighboursInit(int k) {
		final int NEIGHBOUR = k;
		ArrayList<Sensor> pool = new ArrayList<Sensor>();
		pool.addAll(Problem.sensors);

		path.add(Problem.serviceStation.getId());
		ArrayList<Integer> neighbours = new ArrayList<Integer>();

		while (!pool.isEmpty()) {
			while (neighbours.size() < NEIGHBOUR && pool.size() > neighbours.size()) {
				Sensor nei = null;
				for (Sensor s : pool) {
					if (!neighbours.contains(s.getId())) {
						if (nei == null || Problem.distance[path.get(path.size() - 1)][nei
								.getId()] > Problem.distance[path.get(path.size() - 1)][s.getId()]) {
							nei = s;
						}
					}
				}
				neighbours.add(nei.getId());
			}

			int next = neighbours.get(Parameters.rand.nextInt(neighbours.size()));
			path.add(next);
			pool.remove(Problem.getSensorById(next));
			neighbours.clear();
		}
		path.remove(0);
	}

	public ArrayList<Solution> getNeighbours(long size) {
		ArrayList<Solution> neis = new ArrayList<Solution>();
		int n = Problem.sensors.size();
		while (neis.size() < size) {
			int i1 = Parameters.rand.nextInt(n);
			int i2 = Parameters.rand.nextInt(n);

			if (Parameters.rand.nextBoolean()) {
				neis.add(this.getNeighbourBySwap(i1, i2));
			} else {
				neis.add(this.getNeighbourByReinsert(i1, i2));
			}
		}
		return neis;
	}
	// charging time upper bound
	public double getChargingTimeUB() {
		if (tcUB == 0) {
			Charger ch = Problem.charger;
			double maxTc = (ch.getE0() - this.getMovingTime() * ch.getPm()) / ch.getU();
			tcUB = Math.min(maxTc, Parameters.maximalCycleTime);
			if (Problem.sumP > ch.getU()) {
				double ub = (Problem.sumE0 - path.size() * Parameters.S_EMIN - this.getMovingTime() * Problem.sumP)
						/ (Problem.sumP - ch.getU());
				tcUB = Math.min(tcUB, ub);
			} else if (Problem.sumP < ch.getU()) {
				double ub = (path.size() * Parameters.S_EMAX - Problem.sumE0 + this.getMovingTime() * Problem.sumP)
						/ (ch.getU() - Problem.sumP);
				tcUB = Math.min(tcUB, ub);
			}
		}
		return tcUB;
	}

	public Solution getNeighbourBySwap(int index1, int index2) {
		Solution indiv = this.clonePath();
		indiv.swap(index1, index2);
		return indiv;
	}

	public Solution getNeighbourByReinsert(int oldIndex, int newIndex) {
		Solution indiv = this.clonePath();
		indiv.reinsert(oldIndex, newIndex);
		return indiv;
	}

	public void setPath(int index, int value) {
		if (path.size() == index) {
			path.add(value);
			return;
		}
		path.set(index, value);
	}

	public int getPath(int index) {
		return path.get(index);
	}

	public void swap(int index1, int index2) {
		if (index1 == index2)
			return;

		int p1 = path.get(index1), p2 = path.get(index2);
		path.set(index1, p2);
		path.set(index2, p1);
	}

	public void reinsert(int oldIdx, int newIdx) {
		if (oldIdx == newIdx)
			return;

		int tmp = path.get(oldIdx);
		if (oldIdx > newIdx) {
			path.remove(oldIdx);
			path.add(newIdx, tmp);
		} else {
			path.add(newIdx + 1, tmp);
			path.remove(oldIdx);
		}
	}

	public static long getCounter() {
		return counter;
	}

	public static void setCounter(long counter) {
		Solution.counter = counter;
	}

	public void setPath(int[] path) {
		this.path.clear();
		for (int i : path) {
			this.path.add(i);
		}
	}

	public void setPath(ArrayList<Integer> path) {
		this.path = path;
	}

	public int[] getPath() {
		int[] result = new int[this.path.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = path.get(i);
		}
		return result;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Solution clonePath() {
		Solution cpy = new Solution();
		for (int i = 0; i < path.size(); i++) {
			cpy.setPath(i, path.get(i));
		}
		return cpy;
	}

	public int indexOf(int item) {
		return path.indexOf(item);
	}

	public double getTotaldistance() {
		// path here does not contain the service station
		double result = 0;
		int n = path.size();
		result += Problem.distance[path.get(n - 1)][Problem.serviceStation.getId()];
		result += Problem.distance[Problem.serviceStation.getId()][path.get(0)];
		for (int i = 1; i < n; i++) {
			result += Problem.distance[path.get(i - 1)][path.get(i)];
		}
		return result;
	}

	public double getMovingTime() {
		if (this.movingTime == 0) {
			this.setMovingTime(this.getTotaldistance() / Problem.charger.getSpeed());
		}
		return movingTime;
	}

	public void setMovingTime(double movingTime) {
		this.movingTime = movingTime;
	}

	public String toString() {
		String result = "ID = " + this.getId() + ", moving time = " + this.getMovingTime() + ", tour: ";
		for (int i : path) {
			result += i + " ";
		}
		return result;
	}

	public double getFitness() {
		return fitness;
	}

	public void setFitness(double problemFitness) {
		this.fitness = problemFitness;
	}

	public double getTotalCycleTime() {
		return totalCycleTime;
	}

	public void setTotalCycleTime(double totalCycleTime) {
		this.totalCycleTime = totalCycleTime;
	}

	public double getProblemFitness() {
		return problemFitness;
	}

	public void setProblemFitness(double problemFitness) {
		this.problemFitness = problemFitness;
	}

	public double[] getTimeRate() {
		return timeRate;
	}

	public void setTimeRate(double[] timeRate) {
		this.timeRate = timeRate;
	}

	public double getTimeRate(int index) {
		return timeRate[index];
	}

	public void setTimeRate(int index, double val) {
		this.timeRate[index] = val;
	}

	public int getDeadInT() {
		return deadInT;
	}

	public void setDeadInT(int deadInT) {
		this.deadInT = deadInT;
	}
}
