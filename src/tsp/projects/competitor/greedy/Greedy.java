package tsp.projects.competitor.greedy;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.projects.InvalidProjectException;
import tsp.projects.competitor.common.ConstructionSolver;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.SolverProject;
import tsp.projects.competitor.common.TspInstance;

/**
 * Nearest neighbour construction, restarted from a new random city on every
 * round and keeping the shortest tour found.
 *
 * <p>This is the baseline. Any metaheuristic in this repository that cannot beat
 * it is not earning its complexity.
 */
public class Greedy extends SolverProject
{
    public Greedy (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
        this.addAuthor ("Nathan Plontz");
        this.setMethodName ("Greedy");
    }

    @Override
    protected Solver createSolver (TspInstance instance, Random random)
    {
        return new ConstructionSolver (instance, random,
                ConstructionSolver.Kind.NEAREST_NEIGHBOUR, 0, "Greedy");
    }
}
