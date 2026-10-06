package tsp.projects.competitor.grasp;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.projects.InvalidProjectException;
import tsp.projects.competitor.common.ConstructionSolver;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.SolverProject;
import tsp.projects.competitor.common.TspInstance;

/**
 * GRASP: a randomised greedy construction, restarted and keeping the best tour.
 *
 * <p>The convex hull is used as the seed. Cities on the hull appear in the same
 * relative order in any optimal Euclidean tour, so fixing them first costs
 * nothing and gives the randomised completion a sensible skeleton to work from.
 *
 * <p>Alpha controls greediness: at 0.01 the restricted candidate list is almost
 * always a single city, which keeps the tours short but the restarts similar.
 */
public class Grasp extends SolverProject
{
    private static final double ALPHA = 0.01;

    public Grasp (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
        this.addAuthor ("Nathan Plontz");
        this.setMethodName ("GRASP + convex hull");
    }

    @Override
    protected Solver createSolver (TspInstance instance, Random random)
    {
        return new ConstructionSolver (instance, random,
                ConstructionSolver.Kind.HULL_AND_GRASP, ALPHA, "GRASP");
    }
}
