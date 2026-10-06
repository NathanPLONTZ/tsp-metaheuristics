package tsp.projects;

import java.util.ArrayList;

import tsp.evaluation.Evaluation;
import tsp.evaluation.Problem;
import tsp.evaluation.Solution;

/**
 * A metaheuristic for the TSP
 */
public abstract class Project implements Runnable
{
	protected Evaluation evaluation;
	protected Problem problem;
    private String name;
    private ArrayList <String> authors;

	/**
	 * Constructor
	 * @param evaluation the scoring function
	 * @throws InvalidProjectException
	 */
	public Project (Evaluation evaluation) throws InvalidProjectException
	{
		this.evaluation = evaluation;
		this.problem = evaluation.getProblem ();
        this.name = "";
        this.authors = new ArrayList <String> ();
	}

    /**
     * Names the method.
     * Must be called from the subclass constructor.
     * @param name the method name
     */
    protected void setMethodName (String name)
    {
        this.name = name;
    }

    /**
     * Sets every author at once.
     * @param names the authors' names
     * @throws InvalidProjectException if there are more than two authors
     */
    protected void setAuthors (String... names) throws InvalidProjectException
    {
        if (names.length > 2)
            throw new InvalidProjectException ("Too many authors");
        else
        {
            this.authors = new ArrayList <String> ();
            for (String name: names)
                this.authors.add (name);
        }
    }

    /**
     * Adds one author.
     * @param name the author's name
     * @throws InvalidProjectException if there are more than two authors
     */
    protected void addAuthor (String name) throws InvalidProjectException
    {
        if (this.authors.size () < 2)
            this.authors.add (name);
        else
            throw new InvalidProjectException ("Too many authors");
    }

	/**
	 * @return the best solution this method found
	 */
	public Solution getSolution ()
	{
		return new Solution (this.authors, this.name, this.problem.getName (), this.evaluation.getBestEvaluation ());
	}

	/**
	 * Runs once, before the main loop.
	 */
	public abstract void initialization ();

	/**
	 * The main loop, repeated until the time budget runs out.
	 */
	public abstract void loop ();

	@Override
	public void run ()
	{
		System.setProperty("sun.misc.enableTrace", "true");
		try
		{
			this.initialization ();
		}
		catch (Exception e)
		{
			System.out.println ("Erreur durant le chargement:" + e);
		}
		while (!Thread.currentThread ().isInterrupted ())
			try
			{
				this.loop ();
			}
			catch (Exception e)
			{
				System.out.println("Exception:");
				System.out.println(e.toString());
				System.out.println(e.getLocalizedMessage());
				System.out.println(e.getMessage());
				System.out.println(e.getCause());
				System.out.println();
			}
	}
}
