package tsp.projects.competitor.common;

import java.util.Random;

/**
 * Permutation crossover operators.
 *
 * <p>Shared by the genetic solvers.
 */
public final class Crossover
{
    private Crossover ()
    {
    }

    /**
     * Partially Mapped Crossover (PMX).
     *
     * <p>A slice of the first parent is copied verbatim into the offspring. The
     * remaining positions take the second parent's cities, except where that
     * would duplicate a city already present in the copied slice: those are
     * resolved by following the position mapping induced by the slice. The
     * result is always a valid permutation.
     *
     * <p>Both cut points are drawn from <code>nextInt(length)</code>. Drawing the
     * second from <code>nextInt(length - 1)</code> would quietly exclude the last
     * position of the tour from ever belonging to the slice.
     *
     * @return a new permutation of the same length as the parents
     */
    public static int[] pmx (int[] parent1, int[] parent2, Random random)
    {
        int length = parent1.length;
        int[] offspring = new int[length];

        int start = random.nextInt (length);
        int end = random.nextInt (length);
        if (end < start)
        {
            int swap = start;
            start = end;
            end = swap;
        }

        // mapping[c] is the city parent2 holds at the position where parent1
        // holds c. It is only meaningful for cities inside the slice, which is
        // exactly where it gets read.
        int[] mapping = new int[length];
        boolean[] inSlice = new boolean[length];
        for (int i = start; i <= end; i++)
        {
            offspring[i] = parent1[i];
            inSlice[parent1[i]] = true;
            mapping[parent1[i]] = parent2[i];
        }

        for (int i = 0; i < length; i++)
        {
            if (i >= start && i <= end)
                continue;
            int city = parent2[i];
            // The city parent2 proposes may already sit in the copied slice. If
            // so, take whatever parent2 holds at that city's position in parent1,
            // and repeat. The chain always leaves the slice: it is a walk along a
            // bijection between two distinct city sets, so it cannot close on
            // itself before exiting.
            while (inSlice[city])
                city = mapping[city];
            offspring[i] = city;
        }
        return offspring;
    }

    /**
     * Order Crossover (OX).
     *
     * <p>Keeps a slice of the first parent and fills the rest with the remaining
     * cities in the order they appear in the second parent. Unlike PMX it
     * preserves relative order rather than absolute positions, which tends to
     * suit the TSP better because a tour is defined by adjacency, not position.
     *
     * @return a new permutation of the same length as the parents
     */
    public static int[] ox (int[] parent1, int[] parent2, Random random)
    {
        int length = parent1.length;
        int[] offspring = new int[length];
        boolean[] taken = new boolean[length];

        int start = random.nextInt (length);
        int end = random.nextInt (length);
        if (end < start)
        {
            int swap = start;
            start = end;
            end = swap;
        }

        for (int i = start; i <= end; i++)
        {
            offspring[i] = parent1[i];
            taken[parent1[i]] = true;
        }

        int write = (end + 1) % length;
        for (int step = 0; step < length; step++)
        {
            int city = parent2[(end + 1 + step) % length];
            if (taken[city])
                continue;
            offspring[write] = city;
            write = (write + 1) % length;
        }
        return offspring;
    }
}
