package tsp.projects.competitor.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Parent selection and elitism for the genetic solvers.
 *
 * <p>Fitness is a tour length throughout this package, so <em>lower is
 * better</em>. That is worth stating explicitly: getting the direction wrong here
 * is silent, and it degrades a genetic algorithm into something close to a random
 * walk rather than making it fail outright.
 */
public final class Selection
{
    private Selection ()
    {
    }

    /**
     * Tournament selection: draw <code>tournamentSize</code> individuals at
     * random and return the index of the shortest tour among them.
     *
     * <p>Note that the running winner is seeded with an actual contestant, not
     * with index 0. Seeding it with a fixed index biases selection towards that
     * one individual whenever it happens to beat the rest of the draw.
     *
     * @return an index into the population
     */
    public static int tournament (double[] fitness, int tournamentSize, Random random)
    {
        int populationSize = fitness.length;
        int winner = random.nextInt (populationSize);
        for (int i = 1; i < tournamentSize; i++)
        {
            int contestant = random.nextInt (populationSize);
            if (fitness[contestant] < fitness[winner])
                winner = contestant;
        }
        return winner;
    }

    /**
     * Indices of the fittest share of the population, shortest tour first.
     *
     * <p>The ordering is ascending, because shorter is fitter. The indices are
     * read off a sorted copy rather than removed from a working list as it is
     * traversed, which would shift the list under the loop.
     *
     * @param fitness tour length of each individual
     * @param share fraction of the population to keep, in (0, 1]
     * @return the selected indices, best first
     */
    public static int[] elite (final double[] fitness, double share)
    {
        int populationSize = fitness.length;
        int count = Math.max (1, Math.min (populationSize, (int) Math.ceil (populationSize * share)));

        Integer[] order = new Integer[populationSize];
        for (int i = 0; i < populationSize; i++)
            order[i] = i;

        Arrays.sort (order, new Comparator<Integer> ()
        {
            @Override
            public int compare (Integer first, Integer second)
            {
                return Double.compare (fitness[first], fitness[second]);
            }
        });

        int[] selected = new int[count];
        for (int i = 0; i < count; i++)
            selected[i] = order[i];
        return selected;
    }

    /**
     * @param fitness tour length of each individual
     * @return the index of the shortest tour in the population
     */
    public static int best (double[] fitness)
    {
        int best = 0;
        for (int i = 1; i < fitness.length; i++)
            if (fitness[i] < fitness[best])
                best = i;
        return best;
    }
}
