package tsp.evaluation;

import tsp.run.MonitorChart;
import tsp.run.PathChart;

/**
 * Scores a path against a TSP instance
 */
public final class Evaluation
{
	private double bestEvaluation;
	private Problem problem;
	
	/**
	 * Constructor
	 * @param problem the instance, a list of cities
	 */
	public Evaluation (Problem problem)
	{
		this.setBestEvaluation (Double.MAX_VALUE);
		this.problem = problem;
	}
	
	/**
	 * @param path a path
	 * @return whether the path is valid
	 */
	public boolean isValid (Path path)
	{
		return this.isValid (path.getPath ());
	}
	
	/**
	 * @param path a path
	 * @return whether the path is valid
	 */
	public boolean isValid (int [] path)
	{
		boolean valid = path.length == this.problem.getLength ();
		if (valid)
		{
			boolean [] exists = new boolean [path.length];
			for (int i = 0; i < exists.length; i++)
				exists [i] = false;
			for (int i : path)
				if (i < exists.length)
					exists [i] = true;
				else
					valid = false;
			for (int i = 0; i < exists.length; i++)
				if (exists [i] == false)
					valid = false;
		}
		return valid;
	}
	
	/**
	 * @param path the path to score
	 * @return the distance travelled, or Double.MAX_VALUE if the path is invalid or time has run out
	 */
	public double evaluate (Path path)
	{
	    double evaluation = this.quickEvaluateHidden (path);
	    if (evaluation < this.getBestEvaluation())
	    {
	    	if (this.isValid (path))
	    	{
	    		if (!Thread.currentThread ().isInterrupted ())
	    		{
	    			this.setBestEvaluation (evaluation);
	    			PathChart.getInstance().changePath (path);
	    		}
	    	}
	    }
		MonitorChart.getInstance().addData (evaluation, this.getBestEvaluation());
	    return evaluation;
	}
	
    private double quickEvaluateHidden (Path path)
    {
        double evaluation = 0;
        int [] p = path.getPath ();
        Coordinates c1 = this.problem.getCoordinates (p [0]);
        Coordinates c2 = null;
        for (int i = 1; i < p.length; i++)
        {
            c2 = this.problem.getCoordinates (p [i]);
            evaluation += c1.distance (c2);
            c1 = c2;
        }
        c2 = this.problem.getCoordinates (p [0]);
        evaluation += c1.distance (c2);
        return evaluation;
    }
    
    /**
     * @param path the path to score
     * @return the distance travelled, WITHOUT checking that the path is valid
     * and WITHOUT updating the best distance found
     */
    public double quickEvaluate (Path path)
    {
        double evaluation = this.quickEvaluateHidden (path);
        MonitorChart.getInstance().addData (evaluation, this.getBestEvaluation());
        return evaluation;
    }

	/**
	 * @return the TSP instance
	 */
	public Problem getProblem ()
	{
		return this.problem;
	}

	/**
	 * @return the score of the best solution found
	 */
	public double getBestEvaluation ()
	{
		return this.bestEvaluation;
	}

	private void setBestEvaluation (double bestEvaluation)
	{
		this.bestEvaluation = bestEvaluation;
	}
}
