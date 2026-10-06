package tsp.evaluation;

import java.util.Random;

/**
 * An order in which to visit the cities
 * Nothing here guarantees that the path is a valid permutation
 */
public final class Path
{
	private int [] path;
	
	/**
	 * Constructor
	 * @param path the ordered list of cities
	 */
	public Path (Path path)
	{
		this.path = new int [path.path.length];
		for (int i = 0; i < this.path.length; i++)
			this.path [i] = path.path [i];
	}
	
	/**
	 * Constructor
	 * @param path the ordered list of cities
	 */
	public Path (int [] path)
	{
		this.path = path;
	}
	
	/**
	 * Constructor: a random path of the given length
	 * @param length the number of cities
	 */
	public Path (int length)
	{
		this.path = Path.getRandomPath (length);
	}
	
	/**
	 * @param length the number of cities
	 * @return a random path of the given length
	 */
	public static int [] getRandomPath (int length)
	{
		int [] path = new int [length];
		for (int i = 0; i < length; i++)
			path [i] = i;
		Random random = new Random ();
		for (int i = length - 1; i > 0; i--)
		{
			int j = random.nextInt (i + 1);
			int tmp = path [i];
			path [i] = path [j];
			path [j] = tmp;
		}
		return path;
	}
	
	/**
	 * @return the ordered list of cities
	 */
	public int [] getPath ()
	{
		return this.path;
	}
	
	/**
	 * @return the ordered list of cities
	 */
	public int [] getCopyPath ()
	{
		int [] path = new int [this.path.length];
		for (int i = 0; i < this.path.length; i++)
			path [i] = this.path [i];
		return path;
	}
	
	@Override
	public String toString ()
	{
		String string = Integer.toString (this.path [0]);
		for (int i = 1; i < this.path.length; i++)
			string += ";" + this.path [i];
		return string;
	}
}
