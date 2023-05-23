package io.quarkus.narayana.quarkus;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class TSRTest {
    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    boolean innerSyncCalled = false;

    @Test
    public void test() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                tsr.registerInterposedSynchronization(new Synchronization() {

                    @Override
                    public void beforeCompletion() {
                        innerSyncCalled = true;
                    }

                    @Override
                    public void afterCompletion(int status) {
                    }
                });
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        tm.commit();

        assertTrue(innerSyncCalled);
    }

    @Test
    public void test2() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        tm.begin();

        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                tsr.registerInterposedSynchronization(new Synchronization() {

                    @Override
                    public void beforeCompletion() {
                        System.out.printf("beforeCompletion%n");
                        innerSyncCalled = true;
                    }

                    @Override
                    public void afterCompletion(int status) {
                        System.out.printf("afterCompletion%n");
                    }
                });
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        tm.commit();

        assertTrue(innerSyncCalled);
    }
}
