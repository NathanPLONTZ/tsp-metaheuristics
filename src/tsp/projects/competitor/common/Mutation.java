package tsp.projects.competitor.common;

import java.util.Random;

/**
 * Mutation and perturbation operators for permutation encoded tours.
 *
 * <p>The interesting one is {@link #acoRebuildSegment}, which replaces a random
 * stretch of the tour by one rebuilt with an Ant Colony Optimisation rule. It
 * gives the genetic search a mutation that is informed by both distance and
 * accumulated pheromone instead of being purely random.
 *
 * <p>Three design points keep it affordable and correct at scale:
 *
 * <ul>
 * <li><b>Pheromones are sparse.</b> They are stored only for candidate edges,
 * giving n * k values. A full city-by-city matrix would need about 512 MiB on the
 * 8192-city instance, on top of everything else.</li>
 * <li><b>Evaporation is per generation, not per mutation.</b> Walking the
 * pheromone store on every individual mutation is pure overhead; the decay only
 * needs to happen once per generation.</li>
 * <li><b>The rebuild is anchored outside the segment.</b> Anchoring on a city
 * that belongs to the stretch being rebuilt makes the first probability
 * computation divide by a zero distance, which yields an infinite weight and
 * re-selects that same city every time.</li>
 * </ul>
 *
 * <p>Instances of this class hold the pheromone state, which keeps it out of
 * static fields and stops it leaking between independent runs.
 */
public final class Mutation
{
    private static final double PHEROMONE_INITIAL = 1e-5;
    private static final double DEPOSIT = 500;
    private static final double EVAPORATION_RATE = 0.6;
    private static final double PHEROMONE_WEIGHT = 1.0;
    private static final double HEURISTIC_WEIGHT = 7.0;

    /** Shortest stretch worth rebuilding. */
    private static final int MIN_SEGMENT = 5;
    /**
     * Longest stretch worth rebuilding. The rebuild is quadratic in the segment
     * length, so leaving it unbounded would let a single mutation cost tens of
     * millions of operations on a large instance.
     */
    private static final int MAX_SEGMENT = 60;

    private final TspInstance instance;
    private final Random random;
    private final int[][] candidates;
    private final double[][] pheromones;

    public Mutation (TspInstance instance, Random random, int candidateCount)
    {
        this.instance = instance;
        this.random = random;
        this.candidates = instance.neighbourLists (candidateCount);
        this.pheromones = new double[instance.size ()][];
        for (int city = 0; city < instance.size (); city++)
        {
            this.pheromones[city] = new double[this.candidates[city].length];
            for (int k = 0; k < this.pheromones[city].length; k++)
                this.pheromones[city][k] = PHEROMONE_INITIAL;
        }
    }

    /**
     * Evaporates every candidate edge. Call once per generation.
     */
    public void evaporate ()
    {
        for (double[] row : this.pheromones)
            for (int k = 0; k < row.length; k++)
                row[k] *= 1.0 - EVAPORATION_RATE;
    }

    /**
     * Rebuilds a random stretch of the tour with an ACO construction rule, then
     * reinforces the resulting tour with pheromone.
     *
     * @param tour modified in place; stays a valid permutation
     */
    public void acoRebuildSegment (int[] tour)
    {
        int size = tour.length;
        if (size < MIN_SEGMENT + 2)
            return;

        int maxSegment = Math.min (MAX_SEGMENT, size - 2);
        int segmentLength = MIN_SEGMENT + this.random.nextInt (maxSegment - MIN_SEGMENT + 1);
        int start = 1 + this.random.nextInt (size - segmentLength);

        // Cities to reorder, held in a plain array so that removing one is a
        // constant time swap instead of a linear List.remove.
        int[] pool = new int[segmentLength];
        System.arraycopy (tour, start, pool, 0, segmentLength);
        int remaining = segmentLength;

        int current = tour[start - 1];
        double[] weights = new double[segmentLength];
        for (int position = start; position < start + segmentLength; position++)
        {
            int picked = this.pick (current, pool, remaining, weights);
            int city = pool[picked];
            tour[position] = city;
            pool[picked] = pool[remaining - 1];
            remaining--;
            current = city;
        }
        this.deposit (tour);
    }

    /**
     * Roulette wheel over the remaining cities, weighted by pheromone and by the
     * inverse of the distance.
     *
     * @return an index into <code>pool</code>
     */
    private int pick (int from, int[] pool, int remaining, double[] weights)
    {
        double total = 0;
        for (int i = 0; i < remaining; i++)
        {
            double distance = this.instance.distance (from, pool[i]);
            // from never belongs to the pool, so the distance is only zero when
            // two cities share coordinates; clamp to keep the weight finite.
            double heuristic = distance > 0 ? 1.0 / distance : 1e9;
            double weight = Math.pow (this.pheromoneOf (from, pool[i]), PHEROMONE_WEIGHT)
                          * Math.pow (heuristic, HEURISTIC_WEIGHT);
            weights[i] = weight;
            total += weight;
        }

        if (total <= 0 || Double.isNaN (total) || Double.isInfinite (total))
            return this.random.nextInt (remaining);

        double target = this.random.nextDouble () * total;
        double cumulative = 0;
        for (int i = 0; i < remaining; i++)
        {
            cumulative += weights[i];
            if (cumulative >= target)
                return i;
        }
        return remaining - 1;
    }

    /**
     * Lays down pheromone on the tour's edges, inversely to its length.
     * Deposits on edges outside the candidate lists are dropped, since those
     * edges are never considered when choosing a city.
     */
    private void deposit (int[] tour)
    {
        double amount = DEPOSIT / this.instance.tourLength (tour);
        for (int i = 0; i < tour.length; i++)
        {
            int a = tour[i];
            int b = tour[(i + 1) % tour.length];
            this.addPheromone (a, b, amount);
            this.addPheromone (b, a, amount);
        }
    }

    private double pheromoneOf (int from, int to)
    {
        int[] row = this.candidates[from];
        for (int k = 0; k < row.length; k++)
            if (row[k] == to)
                return this.pheromones[from][k];
        return PHEROMONE_INITIAL;
    }

    private void addPheromone (int from, int to, double amount)
    {
        int[] row = this.candidates[from];
        for (int k = 0; k < row.length; k++)
            if (row[k] == to)
            {
                this.pheromones[from][k] += amount;
                return;
            }
    }

    /**
     * Exchanges two cities drawn at random.
     *
     * @param tour modified in place
     */
    public static void swap (int[] tour, Random random)
    {
        int first = random.nextInt (tour.length);
        int second = random.nextInt (tour.length);
        int city = tour[first];
        tour[first] = tour[second];
        tour[second] = city;
    }

    /**
     * Reverses a random stretch of the tour. For a symmetric TSP this is the
     * 2-opt move, so it is a far better random mutation than a swap: it changes
     * two edges instead of four.
     *
     * @param tour modified in place
     */
    public static void inversion (int[] tour, Random random)
    {
        int start = random.nextInt (tour.length);
        int end = random.nextInt (tour.length);
        if (end < start)
        {
            int swap = start;
            start = end;
            end = swap;
        }
        while (start < end)
        {
            int city = tour[start];
            tour[start] = tour[end];
            tour[end] = city;
            start++;
            end--;
        }
    }

    /**
     * Double bridge: the classic 4-opt perturbation used to escape a local
     * optimum. It cuts the tour into four parts and reorders them as A C B D.
     *
     * <p>No sequence of 2-opt or Or-opt moves can undo it in one step, which is
     * exactly what an iterated local search needs: the kick survives the next
     * descent instead of being immediately reversed.
     *
     * @return a new tour; the argument is left untouched
     */
    public static int[] doubleBridge (int[] tour, Random random)
    {
        int size = tour.length;
        if (size < 8)
        {
            int[] copy = tour.clone ();
            inversion (copy, random);
            return copy;
        }

        // Three distinct cut points, sorted.
        int first = 1 + random.nextInt (size - 3);
        int second = first + 1 + random.nextInt (size - first - 2);
        int third = second + 1 + random.nextInt (size - second - 1);

        int[] result = new int[size];
        int write = 0;
        System.arraycopy (tour, 0, result, write, first);
        write += first;
        System.arraycopy (tour, second, result, write, third - second);
        write += third - second;
        System.arraycopy (tour, first, result, write, second - first);
        write += second - first;
        System.arraycopy (tour, third, result, write, size - third);
        return result;
    }
}
