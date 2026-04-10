package minicp.engine.constraints;

import minicp.engine.SolverTest;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.javagrader.Grade;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;

public class Element1DDomainConsistentTest extends SolverTest {

    @ParameterizedTest
    @MethodSource("getSolver")
    public void testElement1D(Solver cp) {
        try {
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
