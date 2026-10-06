package tsp.projects;

/**
 * Exception indiquand qu'un bot n'est pas valide
 */
public class InvalidProjectException extends Exception
{
	private static final long serialVersionUID = 8987635886310738477L;

	/**
     * Constructor...
     */
    public InvalidProjectException ()
    {
        super ("Bot invalide");
    }
    
    /**
     * @param message the message
     */
    public InvalidProjectException (String message)
    {
        super ("Bot invalide : " + message);
    }
}
