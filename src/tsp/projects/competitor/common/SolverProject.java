package tsp.projects.competitor.common;

import java.util.Random;

import tsp.evaluation.Evaluation;
import tsp.evaluation.Path;
import tsp.projects.CompetitorProject;
import tsp.projects.InvalidProjectException;

/**
 * Bridges a {@link Solver} to the competition harness.
 *
 * <p>Each method then needs nothing but a constructor and a factory call, instead
 * of its own copy of the initialise / loop / evaluate plumbing.
 *
 * <p>The harness enforces the real time limit by interrupting the worker thread,
 * so the deadlines handed to the solver are only an upper bound that keeps
 * {@link #loop} returning often enough for the best solution to be reported as it
 * improves.
 */
public abstract class SolverProject extends CompetitorProject
{
    /** Upper bound on the first descent, which is the expensive one. */
    private static final long INITIALISE_SLICE_NANOS = 55_000_000_000L;
    /** Upper bound on a single search round. */
    private static final long LOOP_SLICE_NANOS = 5_000_000_000L;

    private Solver solver;

    protected SolverProject (Evaluation evaluation) throws InvalidProjectException
    {
        super (evaluation);
    }

    /**
     * @param instance the problem, already wrapped with its derived data
     * @param random the source of randomness for this run
     * @return the solver this project runs
     */
    protected abstract Solver createSolver (TspInstance instance, Random random);

    @Override
    public void initialization ()
    {
        TspInstance instance = TspInstance.of (this.problem.getData ());
        this.solver = this.createSolver (instance, new Random ());
        this.solver.initialise (System.nanoTime () + INITIALISE_SLICE_NANOS);
        this.publish ();
    }

    @Override
    public void loop ()
    {
        if (this.solver.iterate (System.nanoTime () + LOOP_SLICE_NANOS))
            this.publish ();
    }

    private void publish ()
    {
        this.evaluation.evaluate (new Path (this.solver.bestTour ()));
    }
}
