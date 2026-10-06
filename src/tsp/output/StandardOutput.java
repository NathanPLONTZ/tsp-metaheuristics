package tsp.output;

/**
 * Writes output to the console
 * Classe singleton
 */
public class StandardOutput extends Output
{
    private static StandardOutput instance = null;
    
    private StandardOutput ()
    {        
    }
    
    /**
     * @return the singleton instance
     */
    public static StandardOutput getInstance ()
    {
        if (StandardOutput.instance == null)
            StandardOutput.instance = new StandardOutput ();
        return StandardOutput.instance;
    }
    
    @Override
    public void print (String string)
    {
        System.out.print (string);
    }

    @Override
    public void initialiaze ()
    {
    }
}
