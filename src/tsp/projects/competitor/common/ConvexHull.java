package tsp.projects.competitor.common;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Graham scan convex hull.
 *
 * <p>The hull is a useful tour seed: the cities on the convex hull appear in the
 * same relative order in any optimal Euclidean tour, so starting from the hull
 * and inserting the interior cities never fights that ordering.
 *
 * <p>Shared by every construction that wants a hull seed.
 */
public final class ConvexHull
{
    private ConvexHull ()
    {
    }

    /**
     * @param instance the problem
     * @param cities the subset of city ids to consider
     * @return the city ids on the convex hull, in counter-clockwise order
     */
    public static int[] compute (TspInstance instance, int[] cities)
    {
        if (cities.length < 3)
            return cities.clone ();

        int pivot = lowestLeftmost (instance, cities);
        Integer[] sorted = sortByPolarAngle (instance, cities, pivot);

        int[] stack = new int[sorted.length];
        int top = 0;
        for (Integer boxed : sorted)
        {
            int city = boxed;
            // Drop the previous point while it makes a non-left turn.
            while (top >= 2 && orientation (instance, stack[top - 2], stack[top - 1], city) <= 0)
                top--;
            stack[top++] = city;
        }
        return Arrays.copyOf (stack, top);
    }

    /**
     * @return the id of the lowest city, the leftmost one breaking ties
     */
    private static int lowestLeftmost (TspInstance instance, int[] cities)
    {
        int best = cities[0];
        for (int city : cities)
        {
            double y = instance.y (city);
            double bestY = instance.y (best);
            if (y < bestY || (y == bestY && instance.x (city) < instance.x (best)))
                best = city;
        }
        return best;
    }

    private static Integer[] sortByPolarAngle (TspInstance instance, int[] cities, final int pivot)
    {
        Integer[] sorted = new Integer[cities.length];
        for (int i = 0; i < cities.length; i++)
            sorted[i] = cities[i];

        final double px = instance.x (pivot);
        final double py = instance.y (pivot);
        final TspInstance problem = instance;

        Arrays.sort (sorted, new Comparator<Integer> ()
        {
            @Override
            public int compare (Integer first, Integer second)
            {
                double angle1 = Math.atan2 (problem.y (first) - py, problem.x (first) - px);
                double angle2 = Math.atan2 (problem.y (second) - py, problem.x (second) - px);
                if (angle1 != angle2)
                    return Double.compare (angle1, angle2);
                // Collinear with the pivot: the closer city comes first so the
                // scan walks outwards and discards the inner ones.
                return Double.compare (problem.distance (pivot, first), problem.distance (pivot, second));
            }
        });
        return sorted;
    }

    /**
     * @return a positive value if a-b-c turns left, negative if it turns right,
     *         zero if the three cities are collinear
     */
    private static double orientation (TspInstance instance, int a, int b, int c)
    {
        double cross = (instance.x (b) - instance.x (a)) * (instance.y (c) - instance.y (a))
                     - (instance.y (b) - instance.y (a)) * (instance.x (c) - instance.x (a));
        if (cross > 0)
            return 1;
        return cross < 0 ? -1 : 0;
    }
}
