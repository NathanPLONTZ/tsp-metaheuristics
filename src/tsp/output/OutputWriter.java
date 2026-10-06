package tsp.output;

import java.util.ArrayList;

/**
 * Fans output out to any number of sinks
 */
public class OutputWriter
{
    private ArrayList <Output> outputs;
    private ArrayList <Output> debug;

    /**
     * Constructor...
     */
    public OutputWriter ()
    {
        this.outputs = new ArrayList <Output> ();
        this.debug = new ArrayList <Output> ();
    }
    
    /**
     * Adds an output sink
     * @param output the sink to add
     */
    public void addOutput (Output output)
    {
        output.initialiaze ();
        this.outputs.add (output);
    }
    
    /**
     * Adds a debug output sink
     * @param output the sink to add
     */
    public void addDebug (Output output)
    {
        output.initialiaze ();
        this.debug.add (output);
    }
    
    /**
     * Writes a line break
     */
    public void print ()
    {
        for (Output output: this.outputs)
            output.println ();
        for (Output debug: this.debug)
            debug.println ();
    }
    
    protected void printDebug ()
    {
        for (Output debug: this.debug)
            debug.println ();
    }
    
    /**
     * Writes a message
     * @param object the object to write
     */
    public void print (Object object)
    {
        for (Output output: this.outputs)
            output.print (object);
        for (Output debug: this.debug)
            debug.print (object);
    }
    
    /**
     * Writes a message followed by a line break
     * @param object the object to write
     */
    public void println (Object object)
    {
        for (Output output: this.outputs)
            output.println (object);
        for (Output debug: this.debug)
            debug.println (object);
    }

    /**
     * Writes a debug message
     * @param object the object to write
     */
    public void printDebug (Object object)
    {
        for (Output debug: this.debug)
            debug.print (object);
    }

    /**
     * Writes a debug message followed by a line break
     * @param object the object to write
     */
    public void printlnDebug (Object object)
    {
        for (Output debug: this.debug)
            debug.println (object);
    }
}
