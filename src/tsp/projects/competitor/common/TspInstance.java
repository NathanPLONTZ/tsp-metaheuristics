package tsp.projects.competitor.common;

/**
 * A Euclidean TSP instance: the city coordinates plus the derived data every
 * solver in this package needs (distances and candidate neighbour lists).
 *
 * <p>This class deliberately has no dependency on the evaluation framework so
 * that the algorithms can also be driven by the headless benchmark runner.
 *
 * <p>Distances are cached in a full matrix only for small instances. A matrix
 * for the 8192-city instance would need 8192 * 8192 * 8 bytes (about 512 MiB),
 * which is why larger instances compute distances on demand instead.
 */
public final class TspInstance
{
    /** Above this number of cities the distance matrix is not materialised. */
    private static final int MATRIX_LIMIT = 2500;

    private final int size;
    private final double[] xs;
    private final double[] ys;
    private final double[][] distances;

    private int[][] neighbours;
    private int neighbourCount;

    public TspInstance (double[] xs, double[] ys)
    {
        if (xs.length != ys.length)
            throw new IllegalArgumentException ("Coordinate arrays must have the same length");
        this.size = xs.length;
        this.xs = xs.clone ();
        this.ys = ys.clone ();
        this.distances = this.size <= MATRIX_LIMIT ? this.buildMatrix () : null;
    }

    /**
     * @param coordinates one {x, y} pair per city
     */
    public static TspInstance of (double[][] coordinates)
    {
        double[] xs = new double[coordinates.length];
        double[] ys = new double[coordinates.length];
        for (int i = 0; i < coordinates.length; i++)
        {
            xs[i] = coordinates[i][0];
            ys[i] = coordinates[i][1];
        }
        return new TspInstance (xs, ys);
    }

    private double[][] buildMatrix ()
    {
        double[][] matrix = new double[this.size][this.size];
        for (int i = 0; i < this.size; i++)
            for (int j = i + 1; j < this.size; j++)
            {
                double d = this.compute (i, j);
                matrix[i][j] = d;
                matrix[j][i] = d;
            }
        return matrix;
    }

    private double compute (int a, int b)
    {
        double dx = this.xs[a] - this.xs[b];
        double dy = this.ys[a] - this.ys[b];
        return Math.sqrt (dx * dx + dy * dy);
    }

    public int size ()
    {
        return this.size;
    }

    public double x (int city)
    {
        return this.xs[city];
    }

    public double y (int city)
    {
        return this.ys[city];
    }

    /**
     * @return the Euclidean distance between two cities
     */
    public double distance (int a, int b)
    {
        return this.distances != null ? this.distances[a][b] : this.compute (a, b);
    }

    /**
     * @param tour a permutation of the cities
     * @return the length of the closed tour
     */
    public double tourLength (int[] tour)
    {
        double length = 0;
        for (int i = 0; i < tour.length - 1; i++)
            length += this.distance (tour[i], tour[i + 1]);
        return length + this.distance (tour[tour.length - 1], tour[0]);
    }

    /**
     * @param tour a candidate solution
     * @return whether the tour visits every city exactly once
     */
    public boolean isValidTour (int[] tour)
    {
        if (tour.length != this.size)
            return false;
        boolean[] seen = new boolean[this.size];
        for (int city : tour)
        {
            if (city < 0 || city >= this.size || seen[city])
                return false;
            seen[city] = true;
        }
        return true;
    }

    /**
     * Candidate list of the <code>count</code> nearest cities of each city,
     * ordered from the closest outwards. Local searches only consider edges
     * towards these candidates, which is what makes them tractable on large
     * instances: an improving 2-opt move almost always introduces a short edge.
     *
     * <p>The lists are built once and reused; asking for a larger count rebuilds
     * them.
     *
     * @param count how many neighbours per city
     */
    public int[][] neighbourLists (int count)
    {
        int wanted = Math.min (count, this.size - 1);
        if (this.neighbours == null || this.neighbourCount < wanted)
        {
            this.neighbours = this.buildNeighbourLists (wanted);
            this.neighbourCount = wanted;
        }
        return this.neighbours;
    }

    /**
     * Selects the nearest cities with a bounded max-heap, which keeps the cost
     * at O(n^2 log k) instead of sorting every row in full.
     */
    private int[][] buildNeighbourLists (int count)
    {
        int[][] lists = new int[this.size][count];
        int[] heapCity = new int[count + 1];
        double[] heapDist = new double[count + 1];
        for (int i = 0; i < this.size; i++)
        {
            int heapSize = 0;
            for (int j = 0; j < this.size; j++)
            {
                if (j == i)
                    continue;
                double d = this.distance (i, j);
                if (heapSize < count)
                {
                    heapCity[heapSize] = j;
                    heapDist[heapSize] = d;
                    heapSize++;
                    siftUp (heapCity, heapDist, heapSize - 1);
                }
                else if (d < heapDist[0])
                {
                    heapCity[0] = j;
                    heapDist[0] = d;
                    siftDown (heapCity, heapDist, heapSize);
                }
            }
            // Draining the max-heap yields the neighbours furthest-first.
            for (int k = heapSize - 1; k >= 0; k--)
            {
                lists[i][k] = heapCity[0];
                heapSize--;
                heapCity[0] = heapCity[heapSize];
                heapDist[0] = heapDist[heapSize];
                siftDown (heapCity, heapDist, heapSize);
            }
        }
        return lists;
    }

    private static void siftUp (int[] cities, double[] dists, int index)
    {
        while (index > 0)
        {
            int parent = (index - 1) / 2;
            if (dists[parent] >= dists[index])
                break;
            swap (cities, dists, parent, index);
            index = parent;
        }
    }

    private static void siftDown (int[] cities, double[] dists, int size)
    {
        int index = 0;
        while (true)
        {
            int left = 2 * index + 1;
            if (left >= size)
                break;
            int largest = left;
            int right = left + 1;
            if (right < size && dists[right] > dists[left])
                largest = right;
            if (dists[index] >= dists[largest])
                break;
            swap (cities, dists, index, largest);
            index = largest;
        }
    }

    private static void swap (int[] cities, double[] dists, int a, int b)
    {
        int city = cities[a];
        cities[a] = cities[b];
        cities[b] = city;
        double dist = dists[a];
        dists[a] = dists[b];
        dists[b] = dist;
    }
}
