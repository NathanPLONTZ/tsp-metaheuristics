package tsp.projects.competitor.common;

import java.util.Random;

/**
 * Lin-Kernighan local search driven by an iterated local search.
 *
 * <p>This is the general purpose solver of the project: nothing in it is tuned to
 * a particular instance, and it is the strongest method here on all three of
 * them. It reuses the constructions written for the earlier methods (convex hull
 * completed by GRASP, or nearest neighbour) as its starting tour.
 *
 * <h2>The Lin-Kernighan step</h2>
 *
 * <p>Lin-Kernighan improves a tour by <em>chains</em> of edge exchanges. A chain
 * anchored at a city <code>t1</code> repeats the following, up to
 * {@link #MAX_DEPTH} times:
 *
 * <ol>
 * <li>Break the edge (t1, t2), where t2 is the successor of t1.</li>
 * <li>Choose a candidate t3 near t2. The quantity
 * <code>d(t1,t2) - d(t2,t3)</code> is the <em>partial gain</em>; requiring it to
 * stay positive is the criterion that makes the search finite and directed.</li>
 * <li>Let t4 be the predecessor of t3. Breaking (t4, t3) and reconnecting yields
 * a single valid tour containing (t2, t3) and (t1, t4), and that reconnection is
 * exactly one segment reversal.</li>
 * <li>The new closing edge is (t1, t4). Since t4 is now the successor of t1, the
 * next round breaks that edge and the chain continues by itself.</li>
 * </ol>
 *
 * <p>The crucial detail is that a chain is allowed to pass through tours that are
 * <em>worse</em> than the one it started from, as long as the partial gain stays
 * positive. The cumulative real gain is recorded at every depth, and the chain is
 * rewound to whichever depth was best. A step therefore never lengthens the
 * tour, yet it can reach improvements no monotone descent would find. Plain
 * 2-opt is this chain truncated at depth one.
 *
 * <h2>Making it fast enough for 8192 cities</h2>
 *
 * <ul>
 * <li><b>Candidate lists.</b> Only the nearest {@link #CANDIDATES} cities are
 * considered as t3. An improving move nearly always introduces a short edge, so
 * scanning all n cities buys almost nothing for n times the cost.</li>
 * <li><b>Don't-look bits.</b> A city is re-examined only when one of its incident
 * edges actually changed. The queue is large on the first pass and tiny
 * afterwards.</li>
 * <li><b>Shorter-side reversal.</b> Reversing a segment or its complement gives
 * the same set of tour edges, so the implementation always reverses whichever is
 * shorter.</li>
 * <li><b>Localised restarts.</b> After a double bridge kick only the six cities
 * at the cut points, plus their candidates, are woken. An iterated local search
 * round therefore costs a fraction of a full pass.</li>
 * </ul>
 *
 * <p>The move set is 2-opt, which is depth one of the chain, the deeper sequential
 * exchanges the chain builds from it, and Or-opt. Richer basic moves exist and are
 * not implemented here.
 */
public final class LinKernighanSearch implements Solver
{
    /** Candidate neighbours considered per city. */
    private static final int CANDIDATES = 8;
    /** Maximum number of edge exchanges in one chain. */
    private static final int MAX_DEPTH = 6;
    /** Longest segment relocated by an Or-opt move. */
    private static final int MAX_OR_OPT_SEGMENT = 3;
    /** Guards against accepting a gain that is only floating point noise. */
    private static final double EPSILON = 1e-9;
    /** How often the time budget is checked inside the improvement loop. */
    private static final int TIME_CHECK_INTERVAL = 128;
    /** Largest block the double bridge kick displaces. See {@link #doubleBridge}. */
    private static final int KICK_WINDOW = 50;

    private final TspInstance instance;
    private final Random random;
    private final int size;
    private final int[][] candidates;

    /** Working tour: position to city. */
    private final int[] tour;
    /** Inverse of {@link #tour}: city to position. */
    private final int[] pos;
    /** Scratch buffer for the double bridge. */
    private final int[] scratch;

    /** Cities waiting to be examined, with a flag to keep the queue a set. */
    private final int[] queue;
    private final boolean[] queued;
    private int queueHead;
    private int queueTail;
    private int queueSize;

    /** Reversals applied by the chain in progress, so it can be rewound. */
    private final int[] chainFrom = new int[MAX_DEPTH];
    private final int[] chainTo = new int[MAX_DEPTH];
    /** Cities already used in the chain in progress, to stop it from cycling. */
    private final boolean[] chainUsed;
    private final int[] chainUsedList = new int[2 * MAX_DEPTH + 2];

    private int[] best;
    private double bestLength;

    /**
     * Length of the working tour, maintained incrementally.
     *
     * <p>Recomputing it from scratch each round meant 8192 square roots per
     * iterated local search round, which dominated everything else on the large
     * instance. Every move here knows its own gain, so the length is simply
     * adjusted by that gain. It is recomputed exactly whenever a new best tour is
     * adopted, which keeps floating point drift from accumulating.
     */
    private double currentLength;

    public LinKernighanSearch (TspInstance instance, Random random)
    {
        this.instance = instance;
        this.random = random;
        this.size = instance.size ();
        this.candidates = instance.neighbourLists (CANDIDATES);
        this.tour = new int[this.size];
        this.pos = new int[this.size];
        this.scratch = new int[this.size];
        this.queue = new int[this.size];
        this.queued = new boolean[this.size];
        this.chainUsed = new boolean[this.size];
    }

    /**
     * Builds the starting tour with the convex hull construction written for the
     * earlier methods, then brings it to a local optimum.
     *
     * <p>The choice of construction barely matters once the chain search runs:
     * the local optima reached from a convex hull seed and from a nearest
     * neighbour seed differ by well under one percent. Reusing the existing
     * construction keeps the comparison with the earlier methods honest.
     */
    @Override
    public void initialise (long deadline)
    {
        this.initialise (Construction.convexHullThenGreedy (this.instance), deadline);
    }

    @Override
    public String name ()
    {
        return "Lin-Kernighan + ILS";
    }

    /**
     * Brings a starting tour to a local optimum and adopts it as the best tour.
     *
     * @param startTour the tour to start from; not modified
     * @param deadline value of {@link System#nanoTime()} after which to stop
     */
    public void initialise (int[] startTour, long deadline)
    {
        this.load (startTour);
        this.currentLength = this.instance.tourLength (this.tour);
        this.queueAll ();
        this.improve (deadline);
        this.best = this.tour.clone ();
        this.bestLength = this.instance.tourLength (this.best);
        this.currentLength = this.bestLength;
    }

    /**
     * One iterated local search round: perturb the best tour with a double
     * bridge, re-optimise around the cut points, and keep the result if shorter.
     *
     * @param deadline value of {@link System#nanoTime()} after which to stop
     * @return whether the best tour improved
     */
    public boolean iterate (long deadline)
    {
        this.load (this.best);
        this.currentLength = this.bestLength;
        this.drainQueue ();
        this.doubleBridge ();
        this.improve (deadline);

        if (this.currentLength < this.bestLength - EPSILON)
        {
            System.arraycopy (this.tour, 0, this.best, 0, this.size);
            // Recompute exactly on adoption so the incremental bookkeeping cannot
            // drift over hundreds of thousands of rounds.
            this.bestLength = this.instance.tourLength (this.best);
            this.currentLength = this.bestLength;
            return true;
        }
        return false;
    }

    /**
     * @return a copy of the shortest tour found so far
     */
    public int[] bestTour ()
    {
        return this.best.clone ();
    }

    /**
     * @return the length of the shortest tour found so far
     */
    public double bestLength ()
    {
        return this.bestLength;
    }

    /**
     * Runs the local search until no city is left to examine, or time runs out.
     */
    private void improve (long deadline)
    {
        int sinceTimeCheck = 0;
        while (this.queueSize > 0)
        {
            if (++sinceTimeCheck >= TIME_CHECK_INTERVAL)
            {
                sinceTimeCheck = 0;
                if (System.nanoTime () > deadline || Thread.currentThread ().isInterrupted ())
                    return;
            }
            int city = this.pop ();
            if (!this.lkStep (city))
                this.orOptStep (city);
        }
    }

    // ------------------------------------------------------------------
    // Lin-Kernighan chain
    // ------------------------------------------------------------------

    /**
     * Attempts a chain anchored at <code>t1</code> in both tour directions.
     *
     * @return whether the tour was shortened
     */
    private boolean lkStep (int t1)
    {
        return this.lkChain (t1, true) || this.lkChain (t1, false);
    }

    /**
     * @param forward whether t2 is the successor or the predecessor of t1;
     *        running both directions keeps the move set symmetric
     * @return whether the tour was shortened
     */
    private boolean lkChain (int t1, boolean forward)
    {
        double cumulative = 0;
        double bestGain = 0;
        int depth = 0;
        int bestDepth = 0;
        int usedCount = 0;

        this.chainUsed[t1] = true;
        this.chainUsedList[usedCount++] = t1;

        while (depth < MAX_DEPTH)
        {
            int t2 = forward ? this.next (t1) : this.previous (t1);
            double broken = this.instance.distance (t1, t2);

            // Pick the candidate with the largest partial gain. This is the
            // Lin-Kernighan criterion, and unlike a plain steepest descent it
            // happily accepts a step that lengthens the tour right now.
            int chosen = -1;
            int chosenT4 = -1;
            double chosenPartial = EPSILON;
            double chosenGain = 0;

            for (int t3 : this.candidates[t2])
            {
                if (t3 == t1 || t3 == t2 || this.chainUsed[t3])
                    continue;

                double added = this.instance.distance (t2, t3);
                double partial = broken - added;
                // Candidates are ordered by distance, so once the added edge is
                // too long no later candidate can qualify either.
                if (partial <= EPSILON)
                    break;

                int t4 = forward ? this.previous (t3) : this.next (t3);
                if (t4 == t1 || t4 == t2 || this.chainUsed[t4])
                    continue;

                if (partial > chosenPartial)
                {
                    chosenPartial = partial;
                    chosen = t3;
                    chosenT4 = t4;
                    chosenGain = broken + this.instance.distance (t4, t3)
                               - added - this.instance.distance (t1, t4);
                }
            }

            if (chosen < 0)
                break;

            // Reconnecting is one reversal of the stretch running from t2 to t4.
            int from = forward ? this.pos[t2] : this.pos[chosenT4];
            int to = forward ? this.pos[chosenT4] : this.pos[t2];
            this.reverse (from, to);
            this.chainFrom[depth] = from;
            this.chainTo[depth] = to;
            depth++;

            this.chainUsed[chosen] = true;
            this.chainUsedList[usedCount++] = chosen;
            this.chainUsed[chosenT4] = true;
            this.chainUsedList[usedCount++] = chosenT4;

            cumulative += chosenGain;
            if (cumulative > bestGain + EPSILON)
            {
                bestGain = cumulative;
                bestDepth = depth;
            }
        }

        // Rewind to the most profitable depth. Reversing the same position range
        // a second time is an exact undo, so this restores the tour precisely.
        for (int level = depth - 1; level >= bestDepth; level--)
            this.reverse (this.chainFrom[level], this.chainTo[level]);

        for (int i = 0; i < usedCount; i++)
            this.chainUsed[this.chainUsedList[i]] = false;

        if (bestGain > EPSILON)
        {
            this.currentLength -= bestGain;
            // Wake everything whose neighbourhood the accepted prefix disturbed.
            for (int level = 0; level < bestDepth; level++)
            {
                this.touch (this.tour[this.chainFrom[level]]);
                this.touch (this.tour[this.chainTo[level]]);
            }
            this.touch (t1);
            this.touch (this.next (t1));
            this.touch (this.previous (t1));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Or-opt
    // ------------------------------------------------------------------

    /**
     * Relocates a short run of cities starting at <code>city</code> elsewhere in
     * the tour, in either orientation.
     *
     * <p>Or-opt complements the chain above: moving a run of one to three cities
     * is a 3-opt move that a sequence of reversals does not reach cheaply.
     *
     * @return whether the tour was shortened
     */
    private boolean orOptStep (int city)
    {
        for (int length = 1; length <= MAX_OR_OPT_SEGMENT && length < this.size - 2; length++)
        {
            int start = this.pos[city];
            int end = start + length - 1;
            // Keeping the segment and its two anchors off the array ends means
            // the splice below never has to wrap around.
            if (start < 1 || end > this.size - 2)
                continue;

            int first = this.tour[start];
            int last = this.tour[end];
            int before = this.tour[start - 1];
            int after = this.tour[end + 1];

            double removed = this.instance.distance (before, first)
                           + this.instance.distance (last, after)
                           - this.instance.distance (before, after);
            if (removed <= EPSILON)
                continue;

            for (int anchor : this.candidates[first])
            {
                int anchorPos = this.pos[anchor];
                if (anchorPos >= start - 1 && anchorPos <= end)
                    continue;
                if (anchorPos == this.size - 1)
                    continue;

                int anchorNext = this.tour[anchorPos + 1];
                double base = this.instance.distance (anchor, anchorNext);
                double straight = this.instance.distance (anchor, first)
                                + this.instance.distance (last, anchorNext) - base;
                double reversed = this.instance.distance (anchor, last)
                                + this.instance.distance (first, anchorNext) - base;

                boolean flip = reversed < straight;
                double inserted = flip ? reversed : straight;

                if (removed - inserted > EPSILON)
                {
                    this.currentLength -= removed - inserted;
                    this.relocate (start, length, anchorPos, flip);
                    this.touch (before);
                    this.touch (after);
                    this.touch (first);
                    this.touch (last);
                    this.touch (anchor);
                    this.touch (anchorNext);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Moves the <code>length</code> cities at <code>start</code> so they sit just
     * after position <code>anchorPos</code>.
     *
     * @param flip whether to insert the run back to front
     */
    private void relocate (int start, int length, int anchorPos, boolean flip)
    {
        int[] segment = new int[length];
        System.arraycopy (this.tour, start, segment, 0, length);
        if (flip)
            for (int i = 0; i < length / 2; i++)
            {
                int swap = segment[i];
                segment[i] = segment[length - 1 - i];
                segment[length - 1 - i] = swap;
            }

        int end = start + length - 1;
        if (anchorPos > end)
        {
            int span = anchorPos - end;
            System.arraycopy (this.tour, end + 1, this.tour, start, span);
            System.arraycopy (segment, 0, this.tour, start + span, length);
            this.reindex (start, anchorPos);
        }
        else
        {
            int span = start - 1 - anchorPos;
            System.arraycopy (this.tour, anchorPos + 1, this.tour, anchorPos + 1 + length, span);
            System.arraycopy (segment, 0, this.tour, anchorPos + 1, length);
            this.reindex (anchorPos + 1, end);
        }
    }

    private void reindex (int from, int to)
    {
        for (int position = from; position <= to; position++)
            this.pos[this.tour[position]] = position;
    }

    // ------------------------------------------------------------------
    // Perturbation
    // ------------------------------------------------------------------

    /**
     * Double bridge: cuts the tour into four parts A B C D and reassembles them
     * as A C B D, then wakes only the cities whose edges that changed.
     *
     * <p>This 4-opt move is the standard iterated local search kick because no
     * single 2-opt or Or-opt move undoes it, so the perturbation survives the
     * next descent instead of being immediately reversed.
     */
    private void doubleBridge ()
    {
        if (this.size < 8)
        {
            Mutation.inversion (this.tour, this.random);
            this.load (this.tour);
            this.currentLength = this.instance.tourLength (this.tour);
            this.queueAll ();
            return;
        }

        // The two middle blocks are kept short on purpose. With cut points drawn
        // uniformly, a double bridge on a large tour splices together parts that
        // are far apart, creating three very long edges; repairing them wakes a
        // large share of the tour and usually just undoes the kick. Bounding the
        // blocks keeps the perturbation local, so a round costs little and the
        // search actually accumulates progress.
        int window = Math.min (KICK_WINDOW, this.size - 3);
        int first = 1 + this.random.nextInt (this.size - 3);
        int second = first + 1 + this.random.nextInt (Math.min (window, this.size - first - 2));
        int third = second + 1 + this.random.nextInt (Math.min (window, this.size - second - 1));

        // The six cities bounding the three cuts are the only ones whose tour
        // neighbours change.
        int aEnd = this.tour[first - 1];
        int bStart = this.tour[first];
        int bEnd = this.tour[second - 1];
        int cStart = this.tour[second];
        int cEnd = this.tour[third - 1];
        int dStart = this.tour[third];
        int[] affected = {aEnd, bStart, bEnd, cStart, cEnd, dStart};

        // Three edges leave, three arrive, so the length change is O(1).
        this.currentLength += this.instance.distance (aEnd, cStart)
                            + this.instance.distance (cEnd, bStart)
                            + this.instance.distance (bEnd, dStart)
                            - this.instance.distance (aEnd, bStart)
                            - this.instance.distance (bEnd, cStart)
                            - this.instance.distance (cEnd, dStart);

        int write = 0;
        System.arraycopy (this.tour, 0, this.scratch, write, first);
        write += first;
        System.arraycopy (this.tour, second, this.scratch, write, third - second);
        write += third - second;
        System.arraycopy (this.tour, first, this.scratch, write, second - first);
        write += second - first;
        System.arraycopy (this.tour, third, this.scratch, write, this.size - third);

        this.load (this.scratch);

        for (int city : affected)
        {
            this.touch (city);
            for (int candidate : this.candidates[city])
                this.touch (candidate);
        }
    }

    // ------------------------------------------------------------------
    // Tour representation
    // ------------------------------------------------------------------

    private void load (int[] source)
    {
        if (source != this.tour)
            System.arraycopy (source, 0, this.tour, 0, this.size);
        for (int position = 0; position < this.size; position++)
            this.pos[this.tour[position]] = position;
    }

    private int next (int city)
    {
        int position = this.pos[city] + 1;
        return this.tour[position == this.size ? 0 : position];
    }

    private int previous (int city)
    {
        int position = this.pos[city] - 1;
        return this.tour[position < 0 ? this.size - 1 : position];
    }

    /**
     * Reverses the cities at positions <code>from</code> to <code>to</code>,
     * wrapping around the end of the array when needed.
     *
     * <p>Reversing a segment and reversing its complement produce the same set of
     * tour edges, so this always reverses whichever of the two is shorter. The
     * choice depends only on the arguments, which is what makes a second call
     * with the same arguments an exact undo; the chain rewind relies on that.
     */
    private void reverse (int from, int to)
    {
        int inner = (to - from + this.size) % this.size + 1;
        if (inner * 2 > this.size)
        {
            int newFrom = (to + 1) % this.size;
            int newTo = (from - 1 + this.size) % this.size;
            from = newFrom;
            to = newTo;
            inner = this.size - inner;
        }
        for (int step = 0; step < inner / 2; step++)
        {
            int left = (from + step) % this.size;
            int right = (to - step + this.size) % this.size;
            int leftCity = this.tour[left];
            int rightCity = this.tour[right];
            this.tour[left] = rightCity;
            this.tour[right] = leftCity;
            this.pos[rightCity] = left;
            this.pos[leftCity] = right;
        }
    }

    // ------------------------------------------------------------------
    // Don't-look bits
    // ------------------------------------------------------------------

    private void queueAll ()
    {
        this.drainQueue ();
        for (int position = 0; position < this.size; position++)
            this.touch (this.tour[position]);
    }

    private void touch (int city)
    {
        if (this.queued[city])
            return;
        this.queued[city] = true;
        this.queue[this.queueTail] = city;
        this.queueTail = (this.queueTail + 1) % this.size;
        this.queueSize++;
    }

    private int pop ()
    {
        int city = this.queue[this.queueHead];
        this.queueHead = (this.queueHead + 1) % this.size;
        this.queueSize--;
        this.queued[city] = false;
        return city;
    }

    /**
     * Empties the queue in time proportional to what is left in it.
     *
     * <p>After {@link #improve} runs to completion the queue is already empty, so
     * this normally costs nothing. It only has work to do when the previous round
     * was cut short by the deadline. Clearing the whole flag array instead would
     * cost O(n) on every single round.
     */
    private void drainQueue ()
    {
        while (this.queueSize > 0)
            this.pop ();
        this.queueHead = 0;
        this.queueTail = 0;
    }
}
