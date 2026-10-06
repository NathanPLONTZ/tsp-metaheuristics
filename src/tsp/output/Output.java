package tsp.output;

/**
 * Base class for an output sink
 */
public abstract class Output
{
    /**
     * Writes a line break
     */
    public void println ()
    {
        this.println ("");
    }

    /**
     * @param object the object to write, followed by a line break
     */
    public void println (Object object)
    {
        this.print (object.toString () + "\n");
    }

    /**
     * @param object the object to write
     */
    public void print (Object object)
    {
        this.print (object.toString ());
    }
    
    protected abstract void print (String string);

    abstract void initialiaze ();
}
