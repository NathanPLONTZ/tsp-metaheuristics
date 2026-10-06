package tsp.projects.competitor.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tour construction heuristics: the starting points handed to the local
 * searches and to the population based methods.
 *
 * <p>Every heuristic here uses the candidate neighbour lists rather than
 * scanning all remaining cities at each step. A scan per step is quadratic per
 * construction and dominates the runtime on the 8192-city instance, whereas the
 * candidate lists give the same tours in practice at a fraction of the cost. A
 * linear scan is kept only as a fallback, for when every candidate of a city has
 * already been visited.
 */
public final class Construction
{
    /** How many candidate neighbours the constructions look at. */
    private static final int CANDIDATES = 12;

    private Construction ()
    {
    }

    /**
     * Nearest neighbour construction from a random start city.
     *
     * @return a tour visiting every city once
     */
    public static int[] nearestNeighbour (TspInstance instance, Random random)
    {
        return nearestNeighbour (instance, random.nextInt (instance.size ()));
    }

    /**
     * Nearest neighbour construction from a given start city.
     */
    public static int[] nearestNeighbour (TspInstance instance, int start)
    {
        int size = instance.size ();
        int[][] candidates = instance.neighbourLists (CANDIDATES);
        boolean[] used = new boolean[size];
        int[] tour = new int[size];

        tour[0] = start;
        used[start] = true;
        for (int i = 1; i < size; i++)
        {
            int current = tour[i - 1];
            int next = nearestUnused (instance, candidates[current], used, current);
            tour[i] = next;
            used[next] = true;
        }
        return tour;
    }

    /**
     * GRASP construction: at every step the next city is drawn uniformly from a
     * restricted candidate list holding the cities whose distance is within
     * <code>alpha</code> of the spread between the closest and furthest
     * candidate. With alpha = 0 this is nearest neighbour; larger values trade
     * tour quality for diversity.
     *
     * @param alpha greediness in [0, 1]
     */
    public static int[] grasp (TspInstance instance, Random random, double alpha)
    {
        int size = instance.size ();
        int[][] candidates = instance.neighbourLists (CANDIDATES);
        boolean[] used = new boolean[size];
        int[] tour = new int[size];
        List<Integer> restricted = new ArrayList<> (CANDIDATES);

        int start = random.nextInt (size);
        tour[0] = start;
        used[start] = true;

        for (int i = 1; i < size; i++)
        {
            int current = tour[i - 1];
            restricted.clear ();

            double min = Double.MAX_VALUE;
            double max = 0;
            for (int candidate : candidates[current])
                if (!used[candidate])
                {
                    double distance = instance.distance (current, candidate);
                    if (distance < min)
                        min = distance;
                    if (distance > max)
                        max = distance;
                }

            if (min == Double.MAX_VALUE)
            {
                // Every candidate is taken; fall back to a full scan.
                tour[i] = nearestUnused (instance, candidates[current], used, current);
            }
            else
            {
                double threshold = min + alpha * (max - min);
                for (int candidate : candidates[current])
                    if (!used[candidate] && instance.distance (current, candidate) <= threshold)
                        restricted.add (candidate);
                tour[i] = restricted.get (random.nextInt (restricted.size ()));
            }
            used[tour[i]] = true;
        }
        return tour;
    }

    /**
     * Convex hull seed completed greedily: the hull cities keep their cyclic
     * order and the interior cities are appended by nearest neighbour.
     */
    public static int[] convexHullThenGreedy (TspInstance instance)
    {
        int size = instance.size ();
        int[] cities = new int[size];
        for (int i = 0; i < size; i++)
            cities[i] = i;

        int[] hull = ConvexHull.compute (instance, cities);
        if (hull.length == size)
            return hull;

        int[][] candidates = instance.neighbourLists (CANDIDATES);
        boolean[] used = new boolean[size];
        int[] tour = new int[size];
        for (int i = 0; i < hull.length; i++)
        {
            tour[i] = hull[i];
            used[hull[i]] = true;
        }
        for (int i = hull.length; i < size; i++)
        {
            int current = tour[i - 1];
            int next = nearestUnused (instance, candidates[current], used, current);
            tour[i] = next;
            used[next] = true;
        }
        return tour;
    }

    /**
     * Convex hull seed completed by a randomised GRASP step.
     *
     * @param alpha greediness in [0, 1]
     */
    public static int[] convexHullThenGrasp (TspInstance instance, Random random, double alpha)
    {
        int size = instance.size ();
        int[] cities = new int[size];
        for (int i = 0; i < size; i++)
            cities[i] = i;

        int[] hull = ConvexHull.compute (instance, cities);
        if (hull.length == size)
            return hull;

        int[][] candidates = instance.neighbourLists (CANDIDATES);
        boolean[] used = new boolean[size];
        int[] tour = new int[size];
        for (int i = 0; i < hull.length; i++)
        {
            tour[i] = hull[i];
            used[hull[i]] = true;
        }

        List<Integer> restricted = new ArrayList<> (CANDIDATES);
        for (int i = hull.length; i < size; i++)
        {
            int current = tour[i - 1];
            restricted.clear ();

            double min = Double.MAX_VALUE;
            double max = 0;
            for (int candidate : candidates[current])
                if (!used[candidate])
                {
                    double distance = instance.distance (current, candidate);
                    if (distance < min)
                        min = distance;
                    if (distance > max)
                        max = distance;
                }

            if (min == Double.MAX_VALUE)
                tour[i] = nearestUnused (instance, candidates[current], used, current);
            else
            {
                double threshold = min + alpha * (max - min);
                for (int candidate : candidates[current])
                    if (!used[candidate] && instance.distance (current, candidate) <= threshold)
                        restricted.add (candidate);
                tour[i] = restricted.get (random.nextInt (restricted.size ()));
            }
            used[tour[i]] = true;
        }
        return tour;
    }

    /**
     * Closest unvisited city, looking at the candidate list first and only
     * scanning every city when all candidates are already used.
     */
    private static int nearestUnused (TspInstance instance, int[] candidates, boolean[] used, int from)
    {
        for (int candidate : candidates)
            if (!used[candidate])
                return candidate;

        int nearest = -1;
        double best = Double.MAX_VALUE;
        for (int city = 0; city < used.length; city++)
            if (!used[city])
            {
                double distance = instance.distance (from, city);
                if (distance < best)
                {
                    best = distance;
                    nearest = city;
                }
            }
        return nearest;
    }
}
