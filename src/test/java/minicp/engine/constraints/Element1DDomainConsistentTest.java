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

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.javagrader.Allow;
import org.javagrader.Forbid;
import org.javagrader.Grade;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.time.Duration;
import java.util.Random;
import java.util.stream.Stream;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static org.junit.jupiter.api.Assertions.*;

@Grade(cpuTimeout = 1)
@Forbid("minicp.engine.constraints.Element2D")
public class Element1DDomainConsistentTest {

    public static Stream<String> getSolver() {
        return Stream.of("Trailer", "Copier");
    }

    private static Solver solver(String type) {
        return type.equals("Copier") ? makeSolver(true) : makeSolver(false);
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void element1dInit(String solver) {
        try {

            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -20, 20);

            int[] T = new int[]{3, 2, 1, -1, 0};

            new Element1DDomainConsistent(T, y, z).post();

            assertEquals(0, y.min());
            assertEquals(4, y.max());


            assertEquals(-1, z.min());
            assertEquals(3, z.max());

            z.removeAbove(1);
            cp.fixPoint();

            assertEquals(2, y.min());


            y.remove(3);
            cp.fixPoint();

            assertEquals(1, z.max());
            assertEquals(0, z.min());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void element1dTest1(String solver) {
        try {

            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -20, 20);

            int[] T = new int[]{3, 2, 1, -1, 0};

            new Element1DDomainConsistent(T, y, z).post();

            assertEquals(0, y.min());
            assertEquals(4, y.max());


            assertEquals(-1, z.min());
            assertEquals(3, z.max());

            z.removeAbove(1);
            cp.fixPoint();

            assertEquals(2, y.min());


            y.remove(3);
            cp.fixPoint();

            assertEquals(1, z.max());
            assertEquals(0, z.min());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void element1dTest2(String solver) {
        try {

            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -20, 40);

            int[] T = new int[]{3, 2, 1, -1, 0};

            new Element1DDomainConsistent(T, y, z).post();

            DFSearch dfs = makeDfs(cp, firstFail(y, z));
            dfs.onSolution(() ->
                    assertEquals(T[y.min()], z.min())
            );
            SearchStatistics stats = dfs.solve();

            assertEquals(5, stats.numberOfSolutions());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void element1dTest3(String solver) {
        try {

            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, 0, 4);
            IntVar z = makeIntVar(cp, -1, 3);


            int[] T = new int[]{3, 2, 1, -1, 0};

            Element1DDomainConsistent element1D = new Element1DDomainConsistent(T, y, z);
            element1D.post();

            y.remove(3); //T[3]=-1
            y.remove(0); //T[0]=3

            element1D.propagate();

            assertEquals(0, z.min());
            assertEquals(2, z.max());
        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void element1dTest4(String solver) {
        try {

            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, 0, 4);
            IntVar z = makeIntVar(cp, -1, 3);

            int[] T = new int[]{3, 2, 1, -1, 0};

            Element1DDomainConsistent element1D = new Element1DDomainConsistent(T, y, z);
            element1D.post();

            z.remove(3); // new max is 2
            z.remove(-1); // new min is 0
            element1D.propagate();

            assertFalse(y.contains(0));
            assertFalse(y.contains(3));
        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    @Allow("java.lang.Thread")
    public void element1dTest6(String solver) {
        try {

            int n = 1_000_000;
            int w = 1000;
            Solver cp = solver(solver);
            IntVar y = makeIntVar(cp, n/2 - w/2, n/2 + w/2);
            IntVar z = makeIntVar(cp, 0, n-1);

            int[] T = IntStream.range(0, n).toArray();
            cp.post(new Element1DDomainConsistent(T, y, z));

            Random random = new Random(42);
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
                for (int i = 0 ; i < w ; ++i) {
                    if (random.nextBoolean())
                        cp.post(notEqual(y, y.min()));
                    else
                        cp.post(notEqual(y, y.max()));
                    // the array is sorted in increasing order here
                    assertEquals(T[y.min()], z.min());
                    assertEquals(T[y.max()], z.max());
                }
            }, "Are you using the StateInt low and up? You should use them to iterate over a part of the array instead of its entirety");

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testElement1D(String solver) {
        try {
            Solver cp = solver(solver);

            // D(y) = {0, 1, 2, ..., 10}
            IntVar y = makeIntVar(cp, 0, 10);

            // D(z) = {-67, ..., 67}
            IntVar z = makeIntVar(cp, -67, 67);

            // T: D(y) --> D(z)
            // such that T(y in D(y)) in D(z)
            // T[0] = 5, T[1] = 5, etc
            int[] T = new int[]{5, 5, 0, 3};

            cp.post(new Element1DDomainConsistent(T, y, z));

            // after initial propagation,
            // D(y) should become [0..3]
            // D(z) should become T = {0, 3, 5}
            assertEquals(T.length, y.size());
            assertEquals(3, z.size());
            assertEquals(0, z.min());
            assertEquals(5, z.max());

            // remove index 2 from D(y), T[2]=0 was only sup for value 0 in D(z)
            // so 0 should be pruned from D(z), {3,5} left
            y.remove(2);
            cp.fixPoint();

            assertEquals(2, z.size());
            assertEquals(3, z.min());
            assertEquals(5, z.max());

            // fix z=3, T[3]=3 is the only index that maps to 3, so D(y) should become {3}
            cp.post(equal(z, 3));
            cp.fixPoint();

            assertEquals(1, y.size());
            assertEquals(3, y.min());
            assertEquals(3, y.max());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }
}
