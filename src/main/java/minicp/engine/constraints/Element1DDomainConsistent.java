/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */


package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;


/**
 *
 * Element Constraint modeling {@code array[y] = z}
 *
 */
public class Element1DDomainConsistent extends AbstractConstraint {

    private final int[] t;
    private final IntVar y;
    private final IntVar z;

    // For each z in D(z), choose one particular y in D(y) such that T[y] = z.
    // y is then the index that we watch to ensure that z is supported.
    private final int[] watchedY;
    private final int zOffset;

    /**
     * Creates an element constraint {@code array[y] = z}
     *
     * @param array the array to index
     * @param y the index variable
     * @param z the result variable
     */
    public Element1DDomainConsistent(int[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.t = array;
        this.y = y;
        this.z = z;

        // init the watched literals array based on the range of z
        this.zOffset = z.min();
        this.watchedY = new int[z.max() - z.min() + 1];

        // initialize to -1 (no watch)
        for (int i = 0; i < watchedY.length; ++i) { watchedY[i] = -1; }

        for (int i = Math.max(0, y.min()); i < Math.min(t.length, y.max() + 1); ++i) {
            if (t[i] < z.min() || t[i] > z.max()) { continue; }
            int watch_idx = t[i] - zOffset;
            if (y.contains(i) && watchedY[watch_idx] == -1) {
                watchedY[watch_idx] = i;
            }
        }
    }

    @Override
    public void post() {
        // remove out of bounds
        y.removeBelow(0);
        y.removeAbove(t.length - 1);

        // run propagation every time D(z) or D(y) changes
        y.propagateOnDomainChange(this);
        z.propagateOnDomainChange(this);

        propagate();
    }

    @Override
    public void propagate() {
        if (y.isFixed()) {
            z.fix(t[y.min()]);
            setActive(false);
            return;
        }

        for (int i = y.min(); i <= y.max(); ++i) {
            // value(s) removed. remove all idxs with the removed values.
            if (y.contains(i) && !z.contains(t[i])) {
                y.remove(i);
            }
        }
        if (y.isFixed()) {
            z.fix(t[y.min()]);
            setActive(false);
            return;
        }

        for (int v = z.min(); v <= z.max(); ++v) {
            if (!z.contains(v)) { continue; }

            // index(es) removed. check watch.
            int old_watch_idx = watchedY[v - zOffset];
           
            // Never support to begin with.
            if (old_watch_idx == -1) {
                z.remove(v);
                continue;
            }

            if (!y.contains(old_watch_idx)) {
                // Find new watch or kill value.
                int new_idx = -1;
                // TODO: either backtrack on watchedY to only look to right?
                for (int i = y.min(); i <= y.max(); ++i) {
                    if (y.contains(i) && t[i] == v) {
                        new_idx = i;
                        break;
                    }
                }

                // No new watch found. Die.
                if (new_idx == -1) {
                    z.remove(v);
                    continue;
                }

                // New watch found. Set to new value.
                watchedY[v - zOffset] = new_idx;
            }
        }
    }
}
