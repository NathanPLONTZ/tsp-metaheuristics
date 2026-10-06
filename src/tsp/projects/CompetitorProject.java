package tsp.projects;

import tsp.evaluation.Evaluation;

/**
 * This is the class a competing method extends.
 */
public abstract class CompetitorProject extends Project {

	public CompetitorProject(Evaluation evaluation) throws InvalidProjectException {
		super(evaluation);
	}
}
