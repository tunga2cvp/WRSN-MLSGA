package chargingtime;

import java.util.ArrayList;
import java.util.Collections;

import problem.Parameters;
import problem.Problem;

public class TPopulation {

	private ArrayList<TIndividual> individuals;

	public TPopulation() {
		individuals = new ArrayList<TIndividual>();
	}

	public void initPopulation(int size) {
		this.getIndividuals().clear();
		while (this.getIndividuals().size() < size) {
			TIndividual indiv = new TIndividual();
			indiv.initIndividual();
			indiv.setFitness(indiv.repairAndCalculateFitness());
			this.getIndividuals().add(indiv);
		}
	}

	public ArrayList<TIndividual> crossover(TIndividual par1, TIndividual par2) {
		return this.SBX(par1, par2);
	}

	public TIndividual mutation(TIndividual indiv, int generation) {
		return this.polynomialMutation(indiv, generation);
	}

	@SuppressWarnings("unchecked")
	public void executeSelection() {
		Collections.sort(this.getIndividuals());
		while (this.getIndividuals().size() > Parameters.T_POP_SIZE) {
			this.getIndividuals().remove(this.getIndividuals().size() - 1);
		}
	}

	public ArrayList<TIndividual> SBX(TIndividual par1, TIndividual par2) {
		TIndividual c1 = new TIndividual();
		TIndividual c2 = new TIndividual();

		int n = Problem.sensors.size();
		double alpha, beta, betaq;
		double nc = 1.0, lb = 0., ub = 1.0;

		for (int i = 0; i < n; i++) {
			double g1 = par1.getTime(i);
			double g2 = par2.getTime(i);

			if (Math.abs(g1 - g2) > 1e-9 && Parameters.rand.nextBoolean()) {
				double y1 = g1 < g2 ? g1 : g2;
				double y2 = g1 < g2 ? g2 : g1;

				double rand = Parameters.rand.nextDouble();
				beta = 1.0 + (2.0 * (y1 - lb) / (y2 - y1));
				alpha = 2.0 - Math.pow(beta, -(nc + 1.0));

				if (rand <= (1.0 / alpha)) {
					betaq = Math.pow((rand * alpha), (1.0 / (nc + 1.0)));
				} else {
					betaq = Math.pow((1.0 / (2.0 - rand * alpha)), (1.0 / (nc + 1.0)));
				}
				c1.setTime(i, 0.5 * (y1 + y2 - betaq * (y2 - y1)));

				beta = 1.0 + (2.0 * (ub - y2) / (y2 - y1));
				alpha = 2.0 - Math.pow(beta, -(nc + 1.0));
				if (rand <= (1.0 / alpha)) {
					betaq = Math.pow((rand * alpha), (1.0 / (nc + 1.0)));
				} else {
					betaq = Math.pow((1.0 / (2.0 - rand * alpha)), (1.0 / (nc + 1.0)));
				}
				c2.setTime(i, 0.5 * (y1 + y2 + betaq * (y2 - y1)));

				if (c1.getTime(i) < lb) {
					c1.setTime(i, lb);
				}
				if (c2.getTime(i) < lb) {
					c2.setTime(i, lb);
				}
				if (c1.getTime(i) > ub) {
					c1.setTime(i, ub);
				}
				if (c2.getTime(i) > ub) {
					c2.setTime(i, ub);
				}

			} else {
				c1.setTime(i, g1);
				c2.setTime(i, g2);
			}

		}

		ArrayList<TIndividual> result = new ArrayList<TIndividual>();
		result.add(c1);
		result.add(c2);

		return result;
	}

	private TIndividual polynomialMutation(TIndividual indiv, int generation) {
		int n = Problem.sensors.size();
		double nm = 100 + generation, lb = 0, ub = 1, rand, alpha, alphaq, y;

		TIndividual child = new TIndividual();
		for (int i = 0; i < n; i++) {
			rand = Parameters.rand.nextDouble();
			alpha = Math.min(indiv.getTime(i) - lb, ub - indiv.getTime(i)) / (ub - lb);
			alphaq = rand <= 0.5 ? Math.pow(2 * rand + (1 - 2 * rand) * Math.pow(1 - alpha, nm + 1), 1.0 / (nm + 1)) - 1
					: 1 - Math.pow(2 * (1 - rand) + 2 * (rand - 0.5) * Math.pow(1 - alpha, nm + 1), 1.0 / (nm + 1));
			y = indiv.getTime(i) + alphaq * (ub - lb);
			y = y < lb ? lb : y;
			y = y > ub ? ub : y;

			child.setTime(i, y);
		}

		return child;
	}

	public ArrayList<TIndividual> getIndividuals() {
		return individuals;
	}

	public void setIndividuals(ArrayList<TIndividual> individuals) {
		this.individuals = individuals;
	}

	public void addIndividuals(ArrayList<TIndividual> indivs) {
		this.individuals.addAll(indivs);
	}

	public TIndividual getIndividual(int index) {
		return individuals.get(index);
	}
}
