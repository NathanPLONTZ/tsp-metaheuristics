package tsp.projects.competitor.genetic;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.projects.InvalidProjectException;
import tsp.projects.competitor.common.GeneticAlgorithm;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.SolverProject;
import tsp.projects.competitor.common.TspInstance;

/**
 * Genetic algorithm with PMX crossover and inversion mutation, over a population
 * seeded by nearest neighbour.
 *
 * <p>Inversion reverses a random stretch of the tour, which for a symmetric TSP
 * is exactly a 2-opt move: it changes two edges rather than the four a city swap
 * would change, and it is the standard mutation for this encoding.
 *
 * <p>Keeping the classical mutation here, rather than the ant colony rebuild used
 * by {@link tsp.projects.competitor.fourmilion.Fourmilion}, is what makes the two
 * methods worth comparing: the difference between them isolates what the ant
 * colony mutation actually contributes.
 */
public class Genetic extends SolverProject
{
    private static final int POPULATION_SIZE = 100;
    private static final double ELITE_SHARE = 0.25;
    private static final double MUTATION_RATE = 0.5;
    private static final int TOURNAMENT_SIZE = 10;

    public Genetic (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
        this.addAuthor ("Nathan Plontz");
        this.setMethodName ("Genetic");
    }

    @Override
    protected Solver createSolver (TspInstance instance, Random random)
    {
        return new GeneticAlgorithm (instance, random, new GeneticAlgorithm.Config (
                "Genetic", POPULATION_SIZE, ELITE_SHARE, MUTATION_RATE, TOURNAMENT_SIZE, 0.01,
                GeneticAlgorithm.MutationKind.INVERSION,
                GeneticAlgorithm.Seeding.NEAREST_NEIGHBOUR));
    }
}
