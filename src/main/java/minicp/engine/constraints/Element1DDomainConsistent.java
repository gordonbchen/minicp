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

import minicp.cp.Factory;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;


/**
 *
 * Element Constraint modeling {@code array[y] = z}
 *
 */
public class Element1DDomainConsistent extends AbstractConstraint {

    private final int[] t;
    private final IntVar y;
    private final IntVar z;

    /**
     * Creates an element constraint {@code array[y] = z}
     *
     * @param array the array to index
     * @param y the index variable
     * @param z the result variable
     */

    // for each z in D(z), choose one particular y in D(y) (that maps to to z T[y]=z) to "watch"
    private final int[] WATCHED_Y;
    private final int zOffset;

    public Element1DDomainConsistent(int[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.t = array;
        this.y = y;
        this.z = z;

        // init the watched literals array based on the range of z
        this.zOffset = z.min();
        this.WATCHED_Y = new int[z.max() - z.min() + 1];

        // initialize to -1 (no watch)
        for (int i = 0; i < WATCHED_Y.length; ++i) { WATCHED_Y[i] = -1; }

        // for each index in D(y), assign it as the watch for T[i]
        // if T[i] is in D(z) and doesn't have a watch yet
        for (int i = y.min(); i <= y.max(); ++i) {
            if (i >= t.length) break;
            if ( y.contains(i) ) {
                int val = t[i];
                if ( z.contains(val) ) {
                    int watch_idx = val - zOffset;
                    // only assign if in bound and not watched
                    if (watch_idx >= 0 && watch_idx < WATCHED_Y.length && WATCHED_Y[watch_idx] == -1) {
                        WATCHED_Y[watch_idx] = i;
                    }
                }
            }
        }

    }

    @Override
    public void post() {
        //  throw new NotImplementedException("Element1D");

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
        // two filtering cases

        // 1. if T[i] not in D(z) then D(y) <-- D(y) \ {i}
        // >>> if the mapping of T[i] is no longer in the range, remove it

        // for all values v in D(z) set zSup(v) = the number of items
        // in the support (D(y)) that map to v
        // (+ WATCHED LITERAL condition)
        // 2. (SLIDES) if zSup(v) = 0 then D(z) <-- D(z) \ v
        // ^ add the watched literal condition, check if watched y still supports v
        // >>> if the "watched" y that maps to v is dead, find a new y
        // >>> that maps to v. If there are no more y's, kill v from D(z).

        // case 1 (filtering from the y direction)
        int yMin = y.min(), yMax = y.max();
        for (int i = yMin; i <= yMax; ++i) {
            if ( y.contains(i) ) {
                // if T[i] is not in D(z) or the index is out of bounds
                if ( !z.contains(t[i]) ) {
                    y.remove(i);
                }
            }
        }

        // case 2 (filtering from the z direction)
        int zMin = z.min(), zMax = z.max();
        for (int v = zMin; v <= zMax; ++v) {
            if ( z.contains(v) ) {

                int watch_idx = v - zOffset;
                int WATCHED_INDEX = WATCHED_Y[watch_idx];

                // check O(1) if watched value is still in D(y) and mapping to v
                // if false do no work
                if ( WATCHED_INDEX == -1 || !y.contains(WATCHED_INDEX) || t[WATCHED_INDEX] != v ) {
                    // we lost our watched value, so we have to find a new one in D(y)
                    // if we can't find it, we kill v
                    boolean found_new_watch = false;

                    // recalculate y bounds as some values might have been killed
                    // in case 1
                    int cur_YMin = y.min(), cur_YMax = y.max();
                    for (int i = cur_YMin; i <= cur_YMax; ++i) {
                        if ( y.contains(i) && t[i] == v ) {
                            found_new_watch = true;
                            // set the new watch
                            WATCHED_Y[watch_idx] = i;
                            break;
                        }
                    }

                    // if no new watch was found, kill v
                    if ( !found_new_watch ) { z.remove(v); }
                }
            }
        }


    }
}
