package tsp.projects.competitor.common;

import java.util.Random;

/**
 * Repeatedly builds tours with a construction heuristic and keeps the shortest.
 *
 * <p>This is what the greedy and GRASP methods amount to: there is no local
 * search, so the only way they improve is by getting luckier on the next
 * restart. They serve as the baselines the metaheuristics have to beat.
 *
 * <p>A purely deterministic construction would gain nothing from restarting, so
 * the nearest neighbour variant starts from a random city each time.
 */
public final class ConstructionSolver implements Solver
{
    /** Which construction to restart. */
    public enum Kind
    {
        /** Nearest neighbour from a random start city. */
        NEAREST_NEIGHBOUR,
        /** GRASP with a restricted candidate list. */
        GRASP,
        /** Convex hull seed completed by GRASP. */
        HULL_AND_GRASP
    }

    private final TspInstance instance;
    private final Random random;
    private final Kind kind;
    private final double alpha;
    private final String name;

    private int[] best;
    private double bestLength;

    /**
     * @param alpha GRASP greediness in [0, 1]; ignored by nearest neighbour
     */
    public ConstructionSolver (TspInstance instance, Random random, Kind kind, double alpha, String name)
    {
        this.instance = instance;
        this.random = random;
        this.kind = kind;
        this.alpha = alpha;
        this.name = name;
    }

    @Override
    public String name ()
    {
        return this.name;
    }

    @Override
    public void initialise (long deadline)
    {
        this.best = this.build ();
        this.bestLength = this.instance.tourLength (this.best);
    }

    @Override
    public boolean iterate (long deadline)
    {
        int[] candidate = this.build ();
        double length = this.instance.tourLength (candidate);
        if (length < this.bestLength)
        {
            this.best = candidate;
            this.bestLength = length;
            return true;
        }
        return false;
    }

    private int[] build ()
    {
        switch (this.kind)
        {
            case GRASP:
                return Construction.grasp (this.instance, this.random, this.alpha);
            case HULL_AND_GRASP:
                return Construction.convexHullThenGrasp (this.instance, this.random, this.alpha);
            case NEAREST_NEIGHBOUR:
            default:
                return Construction.nearestNeighbour (this.instance, this.random);
        }
    }

    @Override
    public int[] bestTour ()
    {
        return this.best.clone ();
    }

    @Override
    public double bestLength ()
    {
        return this.bestLength;
    }
}
