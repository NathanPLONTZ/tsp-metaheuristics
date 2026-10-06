package tsp.projects.competitor.common;

import java.util.Random;

/**
 * Generational genetic algorithm over permutation encoded tours, shared by the
 * plain genetic method and by the ant-colony hybrid.
 *
 * <p>One generation keeps an elite fraction unchanged, then fills the rest of the
 * population with PMX offspring of tournament-selected parents, mutating each
 * child with probability {@link Config#mutationRate}.
 *
 * <p>Two invariants in here are easy to get subtly wrong, and both fail silently
 * rather than loudly, so they are worth naming:
 *
 * <ul>
 * <li><b>The fitness array is never mutated to mark an individual as used.</b> It
 * would be tempting to write <code>fitness[eliteIndex] = Double.MAX_VALUE</code>
 * after copying the elite, to stop the same individual being selected twice. But
 * the tournament reads that same array afterwards, so the fittest individuals
 * would look like the worst candidates for parenthood and selection would
 * collapse towards random.</li>
 * <li><b>The mutation test is <code>random() &lt; mutationRate</code>.</b>
 * Comparing against <code>1 - mutationRate</code> instead silently inverts the
 * parameter's meaning.</li>
 * </ul>
 */
public final class GeneticAlgorithm implements Solver
{
    /** Which mutation operator a configuration uses. */
    public enum MutationKind
    {
        /** Reverse a random stretch: the classical 2-opt style mutation. */
        INVERSION,
        /** Rebuild a random stretch with an ant colony rule. */
        ANT_COLONY
    }

    /** Which construction seeds the initial population. */
    public enum Seeding
    {
        /** Nearest neighbour from random start cities. */
        NEAREST_NEIGHBOUR,
        /** GRASP, with one convex-hull-seeded individual. */
        GRASP_AND_HULL
    }

    /**
     * Parameters of a run. Kept together so the two genetic methods differ only
     * by their configuration and not by a second copy of the algorithm.
     */
    public static final class Config
    {
        public final String name;
        public final int populationSize;
        public final double eliteShare;
        public final double mutationRate;
        public final int tournamentSize;
        public final double graspAlpha;
        public final MutationKind mutation;
        public final Seeding seeding;

        public Config (String name, int populationSize, double eliteShare, double mutationRate,
                       int tournamentSize, double graspAlpha, MutationKind mutation, Seeding seeding)
        {
            this.name = name;
            this.populationSize = populationSize;
            this.eliteShare = eliteShare;
            this.mutationRate = mutationRate;
            this.tournamentSize = tournamentSize;
            this.graspAlpha = graspAlpha;
            this.mutation = mutation;
            this.seeding = seeding;
        }
    }

    /** Candidate neighbours used by the constructions and the ant colony rule. */
    private static final int CANDIDATES = 12;

    private final TspInstance instance;
    private final Random random;
    private final Config config;
    private final Mutation mutationOperator;

    private int[][] population;
    private double[] fitness;
    private int[] best;
    private double bestLength;

    public GeneticAlgorithm (TspInstance instance, Random random, Config config)
    {
        this.instance = instance;
        this.random = random;
        this.config = config;
        this.mutationOperator = config.mutation == MutationKind.ANT_COLONY
                ? new Mutation (instance, random, CANDIDATES)
                : null;
    }

    @Override
    public String name ()
    {
        return this.config.name;
    }

    @Override
    public void initialise (long deadline)
    {
        int size = this.config.populationSize;
        this.population = new int[size][];
        this.fitness = new double[size];

        for (int i = 0; i < size; i++)
        {
            this.population[i] = this.seed (i);
            this.fitness[i] = this.instance.tourLength (this.population[i]);
            if (System.nanoTime () > deadline || Thread.currentThread ().isInterrupted ())
            {
                // Out of time mid-population: fill the rest with copies so the
                // population stays well formed.
                for (int j = i + 1; j < size; j++)
                {
                    this.population[j] = this.population[i].clone ();
                    this.fitness[j] = this.fitness[i];
                }
                break;
            }
        }
        this.recordBest ();
    }

    private int[] seed (int index)
    {
        if (this.config.seeding == Seeding.GRASP_AND_HULL)
            return index == 0
                    ? Construction.convexHullThenGrasp (this.instance, this.random, this.config.graspAlpha)
                    : Construction.grasp (this.instance, this.random, this.config.graspAlpha);
        return Construction.nearestNeighbour (this.instance, this.random);
    }

    @Override
    public boolean iterate (long deadline)
    {
        int size = this.config.populationSize;
        int[][] offspring = new int[size][];
        double[] offspringFitness = new double[size];

        // Elitism: the shortest tours survive untouched.
        int[] elite = Selection.elite (this.fitness, this.config.eliteShare);
        int filled = Math.min (elite.length, size);
        for (int i = 0; i < filled; i++)
        {
            offspring[i] = this.population[elite[i]].clone ();
            offspringFitness[i] = this.fitness[elite[i]];
        }

        for (int i = filled; i < size; i += 2)
        {
            int[] mother = this.population[Selection.tournament (this.fitness, this.config.tournamentSize, this.random)];
            int[] father = this.population[Selection.tournament (this.fitness, this.config.tournamentSize, this.random)];

            int[] first = Crossover.pmx (mother, father, this.random);
            this.maybeMutate (first);
            offspring[i] = first;
            offspringFitness[i] = this.instance.tourLength (first);

            // Offspring come in pairs but the number of free slots may be odd.
            if (i + 1 < size)
            {
                int[] second = Crossover.pmx (father, mother, this.random);
                this.maybeMutate (second);
                offspring[i + 1] = second;
                offspringFitness[i + 1] = this.instance.tourLength (second);
            }

            if (System.nanoTime () > deadline || Thread.currentThread ().isInterrupted ())
            {
                // Abandon this generation rather than return a half built one.
                return false;
            }
        }

        this.population = offspring;
        this.fitness = offspringFitness;
        if (this.mutationOperator != null)
            this.mutationOperator.evaporate ();
        return this.recordBest ();
    }

    private void maybeMutate (int[] tour)
    {
        if (this.random.nextDouble () >= this.config.mutationRate)
            return;
        if (this.mutationOperator != null)
            this.mutationOperator.acoRebuildSegment (tour);
        else
            Mutation.inversion (tour, this.random);
    }

    /**
     * @return whether the best tour improved
     */
    private boolean recordBest ()
    {
        int index = Selection.best (this.fitness);
        if (this.best == null || this.fitness[index] < this.bestLength)
        {
            this.best = this.population[index].clone ();
            this.bestLength = this.fitness[index];
            return true;
        }
        return false;
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
