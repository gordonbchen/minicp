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
import minicp.engine.core.BoolVar;
import minicp.state.StateInt;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * Logical or constraint {@code  x1 or x2 or ... xn}
 * 
 * Watched literals paper: https://sites.cs.st-andrews.ac.uk/people/ipg1/papers/GentJeffersonMiguelCP06.pdf
 * For constraints of the form x1 + x2 + ... + xn >= c, where xi are bool vars, "watch" c+1 vars.
 * Then you only have to care if the vars you are watching get set to 0, instead of if any var changes.
 * If one gets set to 0, then try to switch the watch to another var.
 * If you can't find another var to switch to, either
 *  1. the other c watched vars are the solution.
 *  2. there is no valid solution.
 * wL and wR surround vars that can still be true.
 */
public class Or extends AbstractConstraint { // x1 or x2 or ... xn

    private final BoolVar[] x;
    private final int n;
    private StateInt wL; // watched literal left
    private StateInt wR; // watched literal right


    /**
     * Creates a logical or constraint: at least one variable is true:
     * {@code  x1 or x2 or ... xn}
     *
     * @param x the variables in the scope of the constraint
     */
    public Or(BoolVar[] x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        wL = getSolver().getStateManager().makeStateInt(0);
        wR = getSolver().getStateManager().makeStateInt(n - 1);
    }

    @Override
    public void post() {
        propagate();
    }


    @Override
    public void propagate() {
        if (x[wL.value()].isFixed()) {
            prop(wL, wR);
        }
        else if (x[wR.value()].isFixed()) {
            prop(wR, wL);
        }
        else {
            x[wL.value()].propagateOnFix(this);
            x[wR.value()].propagateOnFix(this);
        }
    }

    private void prop(StateInt fixed, StateInt other) {
        // Fixed to true. Nothing else can be done for other or's.
        if (x[fixed.value()].isTrue()) {
            setActive(false);
            return;
        }

        // Try to move the watch to another var.
        if (moveWatch(fixed)) { return; }

        // Could not move the watch. Either the other var can be set to true or no solutions.
        if (!x[other.value()].isFalse()) {
            x[other.value()].fix(true);
            setActive(false);
            return;
        }

        // Could not set other var to true. So everything is false, and there is no solution.
        throw INCONSISTENCY;
    }

    private boolean moveWatch(StateInt watch) {
        for (int i = wL.value() + 1; i < wR.value(); ++i) {
            if (!x[i].isFalse()) {
                watch.setValue(i);
                x[i].propagateOnFix(this);
                return true;
            }
        }
        return false;
    }
}
