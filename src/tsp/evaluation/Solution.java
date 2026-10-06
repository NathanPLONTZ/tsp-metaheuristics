package tsp.evaluation;

import java.util.ArrayList;

/**
 * A result: the method name, its authors, the instance name and the score
 */
public final class Solution implements Comparable <Solution>
{
    private String name;
    private ArrayList <String> authors;
    private String problemName;
    private double evaluation;

    /**
     * Constructor
     * @param authors the authors
     * @param name the method name
     * @param problemName the instance name
     * @param evaluation the score
     */
    public Solution (ArrayList <String> authors, String name, String problemName, double evaluation)
    {
        this.authors = authors;
        this.name = name;
        this.problemName = problemName;
        this.evaluation = evaluation;
    }

    /**
     * Constructor: the mean over several runs
     * @param solutions the runs to average
     */
    public Solution (ArrayList <Solution> solutions)
    {
        this.authors = solutions.get (0).authors;
        this.name = solutions.get (0).name;
        this.problemName = solutions.get (0).problemName;
        this.evaluation = 0;
        for (Solution solution : solutions)
            this.evaluation += solution.evaluation;
        this.evaluation /= solutions.size ();
    }

    private static void normalize (ArrayList <Solution> solutions)
    {
        double min = Double.MAX_VALUE;
        for (Solution solution: solutions)
            if (solution.evaluation < min)
                min = solution.evaluation;
        for (Solution solution: solutions)
            solution.evaluation = 100 * (solution.evaluation - min) / min;
    }

    /**
     * Aggregates and normalises results so methods can be compared
     * @param solutions every method's result on every instance
     * @return the aggregated, normalised scores
     */
    public static ArrayList <Solution> aggregate (ArrayList <ArrayList <Solution>> solutions)
    {
        for (ArrayList <Solution> s: solutions)
            Solution.normalize (s);
        ArrayList <Solution> agg = new ArrayList <Solution> ();
        for (int i = 0; i < solutions.get (0).size (); i++)
        {
            ArrayList <Solution> projectSolutions = new ArrayList <Solution> ();
            for (ArrayList <Solution> s: solutions)
                projectSolutions.add (s.get (i));
            agg.add (new Solution (projectSolutions));
        }
        return agg;
    }

    /**
     * @return the score
     */
    public double getEvaluation ()
    {
        return this.evaluation;
    }

    @Override
    public String toString ()
    {
        String string = "Projet " + this.name + "\n";
        for (int i = 0; i < this.authors.size (); i++)
            string += this.authors.get (i) + "\n";
        string += "Score: " + this.evaluation;
        return string;
    }

    @Override
    public int compareTo (Solution solution)
    {
        int res = 0;
        if (this.evaluation < solution.evaluation)
            res = -1;
        else if (this.evaluation > solution.evaluation)
            res = 1;			
        return res;
    }
}
