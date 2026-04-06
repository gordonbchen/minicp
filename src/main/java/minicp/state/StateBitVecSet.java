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

package minicp.state;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Class to represent a bit-set that can be saved and restored through
 * the {@link StateManager#saveState()} / {@link StateManager#restoreState()}
 */
public class StateBitVecSet {
    private int ofs;
    private int n;
    private StateInt min;
    private StateInt max;
    private StateInt size;
    private final State<Long>[] words;

    /**
     * Creates a StateBitVecSet with n bits, initially all set
     *
     * @param sm the state manager
     * @param n  the number of bits
     */
    public StateBitVecSet(StateManager sm, int n, int ofs) {
        this.ofs = ofs;
        this.n = n;

        min = sm.makeStateInt(0);
        max = sm.makeStateInt(n-1);
        size = sm.makeStateInt(n);
        
        int nWords = (n + 63) >>> 6; // divided by 64
        words = new State[nWords];
        Arrays.setAll(words, i -> sm.makeStateRef(0xFFFFFFFFFFFFFFFFL));
    }

    // Internal bit vector functions.
    /**
     * Sets the bit at the specified index to true
     *
     * @param i the bit to set
     */
    public void set(int i) {
        long v = 1L << i;  // << is a cyclic shift, (1L << 64) == 1L
        State<Long> w = words[i >>> 6];
        w.setValue(w.value() | v);
    }

    /**
     * Sets the bit at the specified index to false
     *
     * @param i the bit to set
     */
    public void clear(int i) {
        long v = 1L << i;
        State<Long> w = words[i >>> 6];
        w.setValue(w.value() & ~v);
    }

    /**
     * Gives the bit at the specified index
     *
     * @param i the bit to return
     * @return true if bit at index i is set
     */
    public boolean get(int i) {
        State<Long> w = words[i >>> 6];
        return (w.value() & 1L << i) != 0L;
    }

    /**
     * Removes the given value from the set.
     *
     * @param val the value to remove.
     * @return true if val was in the set, false otherwise
     */
    public boolean remove(int val) {
        if (!contains(val)) { return false; }
        val -= ofs;
        clear(val);
        size.decrement();
        updateBoundsValRemoved(val);
        return true;
    }

    // Functions for BitVecDomain.
    public int size() {
        return size.value();
    }

    /**
     * Returns an array with the values present in the set.
     *
     * @return an array representation of the values present in the set
     */
    public int[] toArray() {
        int[] res = new int[size()];
        fillArray(res);
        return res;
    }

    public int fillArray(int[] dest) {
        int s = size.value();
        assert dest.length >= s;

        int j = 0;
        for (int i = 0; i < n; ++i) {
            if (get(i)) {
                dest[j] = i + ofs;
                ++j;
            }
        }
        return s;
    }

    private boolean isEmpty() {
        return size.value() == 0;
    }

    public int min() {
        if (isEmpty()) { throw new NoSuchElementException(); }
        return min.value() + ofs;
    }

    public int max() {
        if (isEmpty()) { throw new NoSuchElementException(); }
        return max.value() + ofs;
    }

    public boolean contains(int val) {
        val -= ofs;
        return internalContains(val);
    }

    private boolean internalContains(int val) {
        if (val < 0 || val >= n) { return false; }
        return get(val);
    }


    private void updateBoundsValRemoved(int val) {
        updateMaxValRemoved(val);
        updateMinValRemoved(val);
    }

    private void updateMaxValRemoved(int val) {
        if (!isEmpty() && max.value() == val) {
            assert (!internalContains(val));
            //the maximum was removed, search the new one
            for (int v = val - 1; v >= min.value(); v--) {
                if (internalContains(v)) {
                    max.setValue(v);
                    return;
                }
            }
        }
    }

    private void updateMinValRemoved(int val) {
        if (!isEmpty() && min.value() == val) {
            assert (!internalContains(val));
            //the minimum was removed, search the new one
            for (int v = val + 1; v <= max.value(); v++) {
                if (internalContains(v)) {
                    min.setValue(v);
                    return;
                }
            }
        }
    }

    /**
     * Removes all the element from the set except the given value.
     *
     * @param v is an element in the set
     */
    public void removeAllBut(int v) {
        assert (contains(v));
        v -= ofs;
        removeAll();
        set(v);
        min.setValue(v);
        max.setValue(v);
        size.setValue(1);
    }

    /**
     * Removes all the values in the set.
     */
    public void removeAll() {
        for (int i = 0; i < words.length; ++i) {
            words[i].setValue(0L);
        }
        size.setValue(0);
    }

    /**
     * Remove all the values less than the given value from the set
     *
     * @param value a value such that all the ones smaller are removed
     */
    public void removeBelow(int value) {
        if (max() < value) {
            removeAll();
        } else {
            for (int v = min(); v < value; v++) {
                remove(v);
            }
        }
    }

    /**
     * Remove all the values larger than the given value from the set
     *
     * @param value a value such that all the ones greater are removed
     */
    public void removeAbove(int value) {
        if (min() > value) {
            removeAll();
        } else {
            int max = max();
            for (int v = max; v > value; v--) {
                remove(v);
            }
        }
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("{");
        for (int i = 0; i < size(); i++) {
            if (get(i)) {
                b.append(i + ofs);
                b.append(',');
            }
        }
        b.append("}");
        return b.toString();
    }
}
