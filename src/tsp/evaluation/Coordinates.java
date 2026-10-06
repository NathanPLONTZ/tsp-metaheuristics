package tsp.evaluation;

/**
 * A 2D point, with the Euclidean distance between two of them
 */
public final class Coordinates
{
	private double x, y;
	
	/**
	 * @param x the abscissa
	 * @param y the ordinate
	 */
	public Coordinates (double x, double y)
	{
		this.x = x;
		this.y = y;
	}
	
	/**
	 * @param c another point
	 * @return the distance between the two points
	 */
	public double distance (Coordinates c)
	{
		double dx = this.x - c.x;
		double dy = this.y - c.y;
		return Math.sqrt (dx * dx + dy * dy);
	}
	
	/**
	 * @return the x coordinate
	 */
	public double getX ()
	{
		return this.x;
	}
	
	/**
	 * @return the y coordinate
	 */
	public double getY ()
	{
		return this.y;
	}
}
