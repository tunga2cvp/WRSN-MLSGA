package chargingtime;

import java.util.ArrayList;
import java.util.Collections;

import chargingpath.Solution;
import problem.Parameters;

public class TSolver {

	public static Solution path;
	private TPopulation pop;
	private TIndividual best;
	private int gen;

	public TSolver(Solution path) {
		TSolver.path = path;
	}

	@SuppressWarnings("unchecked")
	public TIndividual solve() {

		pop = new TPopulation();
		pop.initPopulation(Parameters.T_POP_SIZE);
		Collections.sort(pop.getIndividuals());
		best = pop.getIndividual(0);

		gen = 1;
		int k = Parameters.T_POP_SIZE / 2;
		while (gen++ < Parameters.T_GENERATIONS && (best == null || best.getFitness() > 0)) {
			ArrayList<TIndividual> offspring = this.reproduction();

			for (int i = 0; i < k; i++) {
				offspring.add(pop.getIndividual(i));
			}

			pop.getIndividuals().clear();
			pop.addIndividuals(offspring);
			pop.executeSelection();

			if (best.getFitness() > pop.getIndividual(0).getFitness()) {
				best = pop.getIndividual(0);
			}

//			System.out.println(gen + "\t" + best.getFitness());
		}

		return best;
	}

	private ArrayList<TIndividual> reproduction() {
		ArrayList<TIndividual> offspring = new ArrayList<TIndividual>();
		ArrayList<TIndividual> matingPool = new ArrayList<TIndividual>();
		matingPool.addAll(pop.getIndividuals());

		TIndividual p1, p2, par1, par2;
		while (offspring.size() < Parameters.T_POP_SIZE) {
			p1 = matingPool.remove(Parameters.rand.nextInt(matingPool.size()));
			p2 = matingPool.remove(Parameters.rand.nextInt(matingPool.size()));
			if (p1.getFitness() <= p2.getFitness()) {
				par1 = p1;
				matingPool.add(p2);
			} else {
				par1 = p2;
				matingPool.add(p1);
			}

			p1 = matingPool.remove(Parameters.rand.nextInt(matingPool.size()));
			p2 = matingPool.remove(Parameters.rand.nextInt(matingPool.size()));
			if (p1.getFitness() <= p2.getFitness()) {
				par2 = p1;
				matingPool.add(p2);
			} else {
				par2 = p2;
				matingPool.add(p1);
			}

			matingPool.add(par1);
			matingPool.add(par2);

			ArrayList<TIndividual> child = pop.crossover(par1, par2);
			for (TIndividual indiv : child) {
				if (Parameters.rand.nextDouble() < Parameters.T_MUTATION_RATIO) {
					TIndividual mutated = pop.mutation(indiv, gen);
					mutated.setFitness(mutated.repairAndCalculateFitness());
					offspring.add(mutated);
				} else {
					indiv.setFitness(indiv.repairAndCalculateFitness());
					offspring.add(indiv);
				}
			}
		}

		return offspring;
	}

	public TPopulation getPop() {
		return pop;
	}

	public void setPop(TPopulation pop) {
		this.pop = pop;
	}

	public boolean updateBest(TIndividual indiv) {
		if (best == null || best.getFitness() > indiv.getFitness()) {
			best = indiv;
			return true;
		} else {
			return false;
		}
	}
}
