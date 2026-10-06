package tsp.projects;

import tsp.evaluation.Evaluation;


/**
 * Demonstration methods only.
 * A competing method extends CompetitorProject instead.
 */
public abstract class DemoProject extends Project {

	public DemoProject(Evaluation evaluation) throws InvalidProjectException {
		super(evaluation);
	}
}
