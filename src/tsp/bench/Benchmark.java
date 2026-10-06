package tsp.bench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import tsp.evaluation.Problem;
import tsp.projects.competitor.common.ConstructionSolver;
import tsp.projects.competitor.common.GeneticAlgorithm;
import tsp.projects.competitor.common.LinKernighanSearch;
import tsp.projects.competitor.common.Solver;
import tsp.projects.competitor.common.TspInstance;

/**
 * Headless benchmark runner: the numbers reported in the README come from here.
 *
 * <p>It deliberately bypasses the competition harness. That harness opens Swing
 * charts, logs to a file and evaluates through a shared singleton, none of which
 * belongs in a reproducible measurement. Because the algorithms are written
 * against {@link Solver} rather than against the framework, the exact same code
 * runs in both places.
 *
 * <p>Every run is seeded from the run index, so the whole table reproduces
 * byte for byte.
 *
 * <p>Usage: {@code java tsp.bench.Benchmark [secondsPerRun] [runsPerMethod]}
 */
public final class Benchmark
{
    /**
     * Best known tour lengths, used only to report a gap.
     *
     * <p>bier127 is the published TSPLIB optimum. gr666 is the value obtained
     * with the Concorde solver on these coordinates. pr8192 is provably optimal:
     * that instance is a comb of 4096 abscissas, each holding one city at y = 0
     * and one at y = 1, so any tour must cross the x range twice (2 * 8190) and
     * must change row at least twice (2 * 1), giving a lower bound of 16382 that
     * the obvious sweep attains.
     */
    private static final Map<String, Double> REFERENCE = new LinkedHashMap<> ();
    static
    {
        REFERENCE.put ("bier127", 118282.0);
        REFERENCE.put ("gr666", 3062.0);
        REFERENCE.put ("pr8192", 16382.0);
    }

    private Benchmark ()
    {
    }

    /** Builds a fresh solver for one run. */
    private interface Factory
    {
        Solver create (TspInstance instance, Random random);
    }

    private static Map<String, Factory> methods ()
    {
        Map<String, Factory> methods = new LinkedHashMap<> ();

        methods.put ("Greedy (nearest neighbour)", new Factory ()
        {
            @Override
            public Solver create (TspInstance instance, Random random)
            {
                return new ConstructionSolver (instance, random,
                        ConstructionSolver.Kind.NEAREST_NEIGHBOUR, 0, "Greedy");
            }
        });

        methods.put ("GRASP + convex hull", new Factory ()
        {
            @Override
            public Solver create (TspInstance instance, Random random)
            {
                return new ConstructionSolver (instance, random,
                        ConstructionSolver.Kind.HULL_AND_GRASP, 0.01, "GRASP");
            }
        });

        methods.put ("Genetic (PMX + inversion)", new Factory ()
        {
            @Override
            public Solver create (TspInstance instance, Random random)
            {
                return new GeneticAlgorithm (instance, random, new GeneticAlgorithm.Config (
                        "Genetic", 100, 0.25, 0.5, 10, 0.01,
                        GeneticAlgorithm.MutationKind.INVERSION,
                        GeneticAlgorithm.Seeding.NEAREST_NEIGHBOUR));
            }
        });

        methods.put ("Fourmilion (PMX + ACO mutation)", new Factory ()
        {
            @Override
            public Solver create (TspInstance instance, Random random)
            {
                return new GeneticAlgorithm (instance, random, new GeneticAlgorithm.Config (
                        "Fourmilion", 125, 0.12, 0.33, 10, 0.02,
                        GeneticAlgorithm.MutationKind.ANT_COLONY,
                        GeneticAlgorithm.Seeding.GRASP_AND_HULL));
            }
        });

        methods.put ("Lin-Kernighan + ILS", new Factory ()
        {
            @Override
            public Solver create (TspInstance instance, Random random)
            {
                return new LinKernighanSearch (instance, random);
            }
        });

        return methods;
    }

    public static void main (String[] args)
    {
        int seconds = args.length > 0 ? Integer.parseInt (args[0]) : 60;
        int runs = args.length > 1 ? Integer.parseInt (args[1]) : 10;

        ArrayList<Problem> problems = Problem.getProblems ();
        Map<String, Factory> methods = methods ();

        System.out.println ("Budget " + seconds + " s per run, " + runs + " runs per method");
        System.out.println ();
        System.out.println ("method,instance,cities,best,mean,worst,gap_best_percent,gap_mean_percent,valid");

        for (Map.Entry<String, Factory> method : methods.entrySet ())
            for (Problem problem : problems)
            {
                TspInstance instance = TspInstance.of (problem.getData ());
                double best = Double.MAX_VALUE;
                double worst = 0;
                double total = 0;
                boolean valid = true;

                for (int run = 0; run < runs; run++)
                {
                    Solver solver = method.getValue ().create (instance, new Random (seedFor (run)));
                    double length = execute (solver, seconds);
                    if (!instance.isValidTour (solver.bestTour ()))
                        valid = false;
                    best = Math.min (best, length);
                    worst = Math.max (worst, length);
                    total += length;
                }

                double mean = total / runs;
                Double reference = REFERENCE.get (problem.getName ());
                // Locale.ROOT, or a locale using decimal commas turns the CSV
                // into nonsense.
                String gapBest = reference == null ? ""
                        : String.format (Locale.ROOT, "%.2f", 100 * (best - reference) / reference);
                String gapMean = reference == null ? ""
                        : String.format (Locale.ROOT, "%.2f", 100 * (mean - reference) / reference);

                System.out.println (String.format (Locale.ROOT, "%s,%s,%d,%.1f,%.1f,%.1f,%s,%s,%s",
                        method.getKey (), problem.getName (), instance.size (),
                        best, mean, worst, gapBest, gapMean, valid));
            }
    }

    /**
     * A fixed seed per run index, so the table is reproducible.
     */
    private static long seedFor (int run)
    {
        return 982451653L * (run + 1);
    }

    /**
     * @return the length of the best tour the solver reached within the budget
     */
    private static double execute (Solver solver, int seconds)
    {
        long deadline = System.nanoTime () + seconds * 1_000_000_000L;
        solver.initialise (deadline);
        while (System.nanoTime () < deadline)
            solver.iterate (deadline);
        return solver.bestLength ();
    }
}
