package io.quarkus.narayana.quarkus;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * test that interposed synchronizations are called in the correct order
 * See {@code io.quarkus.narayana.jta.runtime.internal.tsr.AgroalOrderedLastSynchronizationList} for
 * the implementation
 */
public class TSRTest {
    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @Inject
    Event<String> event;

    enum SYNCH_TYPES {
        ARC,
        OTHER
    };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TSRTest.ObservingBean.class));

    private static final List<String> synchronizationCallbacks = new ArrayList<>();

    @BeforeEach
    public void before() {
        synchronizationCallbacks.clear();
    }

    @Test
    public void test() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        tm.begin();

        // register a synchronization that registers more synchronizations during the beforeCompletion callback
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                synchronizationCallbacks.add(SYNCH_TYPES.OTHER.name());

                // and add two synchronizations belonging to the same "category"

                // these registrations should succeed since they belong to the same group that's already being processed
                // note that adding one for a group that has already ran would fail
                tsr.registerInterposedSynchronization(new NormalSynchronization());
                tsr.registerInterposedSynchronization(new NormalSynchronization());
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        // cause ARC to register a callback for transaction lifecycle events (see ObservingBean)
        event.fire("commit");

        tm.commit();

        /*
         * there should be four registered synchronizations:
         * - the first one added by this test
         * - the two added by the first callback during the beforeCompletion call
         * - and one for the before completion observer callback (the ARC synchronization)
         */
        Assertions.assertEquals(4, synchronizationCallbacks.size());
        // verify that the synchs in the ARC category are called before the other categories
        // even though normal ones were registered first
        Assertions.assertEquals(SYNCH_TYPES.ARC.name(), synchronizationCallbacks.get(0));
        Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(1));
        Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(2));
        Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(3));
    }

    @ApplicationScoped
    static class ObservingBean {
        public void observeBeforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) String payload) {
            synchronizationCallbacks.add(SYNCH_TYPES.ARC.name());
        }
    }

    private static class NormalSynchronization implements Synchronization {
        @Override
        public void beforeCompletion() {
            synchronizationCallbacks.add(SYNCH_TYPES.OTHER.name());
        }

        @Override
        public void afterCompletion(int status) {
        }
    }
}
