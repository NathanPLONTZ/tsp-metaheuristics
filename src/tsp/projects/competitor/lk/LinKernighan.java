package tsp.projects.competitor.lk;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.projects.InvalidProjectException;
import tsp.projects.competitor.common.LinKernighanSearch;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.SolverProject;
import tsp.projects.competitor.common.TspInstance;

/**
 * Lin-Kernighan local search wrapped in an iterated local search.
 *
 * <p>The strongest method here, and the only one that is competitive on all three
 * instances at once. The algorithm itself is documented in
 * {@link LinKernighanSearch}.
 */
public class LinKernighan extends SolverProject
{
    public LinKernighan (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
        this.addAuthor ("Nathan Plontz");
        this.setMethodName ("Lin-Kernighan + ILS");
    }

    @Override
    protected Solver createSolver (TspInstance instance, Random random)
    {
        return new LinKernighanSearch (instance, random);
    }
}
