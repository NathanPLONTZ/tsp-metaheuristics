package tsp.projects.competitor.fourmilion;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.projects.InvalidProjectException;
import tsp.projects.competitor.common.GeneticAlgorithm;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.SolverProject;
import tsp.projects.competitor.common.TspInstance;

/**
 * Genetic algorithm whose mutation operator rebuilds a stretch of the tour with
 * an Ant Colony Optimisation rule, over a population built by GRASP with one
 * convex-hull-seeded individual.
 *
 * <p>This was the method submitted for the project. The idea is to replace the
 * blind random mutation of a standard genetic algorithm with one guided by both
 * distance and accumulated pheromone, so that a mutation tends to produce a
 * plausible sub-tour rather than a random shuffle.
 */
public class Fourmilion extends SolverProject
{
    private static final int POPULATION_SIZE = 125;
    private static final double ELITE_SHARE = 0.12;
    private static final double MUTATION_RATE = 0.33;
    private static final int TOURNAMENT_SIZE = 10;
    private static final double GRASP_ALPHA = 0.02;

    public Fourmilion (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
        this.addAuthor ("Nathan Plontz");
        this.setMethodName ("Fourmilion");
    }

    @Override
    protected Solver createSolver (TspInstance instance, Random random)
    {
        return new GeneticAlgorithm (instance, random, new GeneticAlgorithm.Config (
                "Fourmilion", POPULATION_SIZE, ELITE_SHARE, MUTATION_RATE, TOURNAMENT_SIZE,
                GRASP_ALPHA,
                GeneticAlgorithm.MutationKind.ANT_COLONY,
                GeneticAlgorithm.Seeding.GRASP_AND_HULL));
    }
}
