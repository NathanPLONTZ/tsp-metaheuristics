package tsp.projects.competitor.common;

/**
 * A tour improvement method, expressed independently of the evaluation
 * framework.
 *
 * <p>Keeping the algorithms behind this interface means the same code is used by
 * the competition harness, through a thin {@code CompetitorProject} adapter, and
 * by the headless benchmark runner that produces the figures in the README,
 * instead of each algorithm existing twice in two slightly different versions.
 *
 * <p>Every method is deadline driven rather than iteration driven, because the
 * project is scored on a fixed time budget.
 */
public interface Solver
{
    /**
     * @return a short human readable name, used in reports
     */
    String name ();

    /**
     * Builds the first solution. Must leave {@link #bestTour} usable.
     *
     * @param deadline value of {@link System#nanoTime()} after which to stop
     */
    void initialise (long deadline);

    /**
     * Performs one round of search.
     *
     * @param deadline value of {@link System#nanoTime()} after which to stop
     * @return whether the best tour improved
     */
    boolean iterate (long deadline);

    /**
     * @return a copy of the shortest tour found so far
     */
    int[] bestTour ();

    /**
     * @return the length of the shortest tour found so far
     */
    double bestLength ();
}
