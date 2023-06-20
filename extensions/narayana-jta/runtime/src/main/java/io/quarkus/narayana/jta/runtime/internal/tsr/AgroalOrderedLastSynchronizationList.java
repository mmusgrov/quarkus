package io.quarkus.narayana.jta.runtime.internal.tsr;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

public class AgroalOrderedLastSynchronizationList implements Synchronization {
    private static final Logger LOGGER = Logger.getLogger(AgroalOrderedLastSynchronizationList.class);
    private static final String ADD_SYNC_ERROR = "Syncs are not allowed because the group of synchronizations to which this sync belongs has already ran";

    // Keep track of whether a synchronization group has been processed.
    // If a group of synchs has already been processed then do not allow further synchs to be registered in that group.
    // If a group of synchs is currently being processed then allow it to be registered.
    private enum ExecutionStatus {
        PENDING,
        RUNNING,
        FINISHED
    }

    private class SynchronizationGroup implements Synchronization {
        String packageName;
        List<Synchronization> synchs;
        ExecutionStatus status;

        public SynchronizationGroup(String packageName) {
            this.packageName = packageName;
            this.synchs = new ArrayList<>();
            this.status = ExecutionStatus.PENDING;
        }

        public void add(Synchronization synchronization) {
            if (status == ExecutionStatus.FINISHED) {
                // this group of syncs have already ran
                throw new IllegalStateException(ADD_SYNC_ERROR);
            }
            synchs.add(synchronization);
        }

        @Override
        public void beforeCompletion() {
            status = ExecutionStatus.RUNNING;
            // for (Iterator<Synchronization> it = synchs.iterator(); it.hasNext();) {
            // NB synchs can register synchs so cannot use enhanced for loops
            for (int i = 0; i < synchs.size(); i++) {
                Synchronization sync = synchs.get(i);

                try {
                    sync.beforeCompletion();
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debugf(
                                "The synchronization %s associated with tx key %s failed during beforeCompletion: %s",
                                sync, tsr.getTransactionKey(), e.getMessage());
                    }
                }
            }
            status = ExecutionStatus.FINISHED;
        }

        @Override
        public void afterCompletion(int status) {
            // The list should be iterated in reverse order
            for (int i = synchs.size(); i-- > 0;) {
                Synchronization sync = synchs.get(i);

                try {
                    sync.afterCompletion(status);
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debugf(
                                "The synchronization %s associated with tx key %s failed during afterCompletion(%d): %s",
                                sync, tsr.getTransactionKey(), status, e.getMessage());
                    }
                }
            }
        }
    }

    private final List<SynchronizationGroup> synchGroups = new ArrayList<>();
    private SynchronizationGroup otherSyncs;
    private final TransactionSynchronizationRegistry tsr;

    public void setSynchronizationGroupOrder(String... packagePrefixes) {
        for (var packagePrefix : packagePrefixes) {
            var synchronizationGroup = new SynchronizationGroup(packagePrefix);

            synchGroups.add(synchronizationGroup);

            if (packagePrefix.isEmpty()) {
                otherSyncs = synchronizationGroup;
            }
        }
    }

    public AgroalOrderedLastSynchronizationList(
            TransactionSynchronizationRegistryWrapper transactionSynchronizationRegistryWrapper) {
        this.tsr = transactionSynchronizationRegistryWrapper;

        // order the synchronization groups as follows [ARC, OTHER, HIBERNATE, AGROAL].
        // Agroal is last since it validates that connection wrappers are closed at the right time:
        setSynchronizationGroupOrder("io.quarkus.arc.impl", "", "org.hibernate", "io.agroal.narayana");
    }

    /**
     * This is only allowed at various points of the transaction lifecycle.
     *
     * @param synchronization The synchronization to register
     * @throws IllegalStateException In case the transaction was in a state that was not valid to register under
     * @throws SystemException In case the transaction status was not known
     */
    public void registerInterposedSynchronization(Synchronization synchronization)
            throws IllegalStateException, SystemException {
        int status = tsr.getTransactionStatus();

        switch (status) {
            case Status.STATUS_ACTIVE:
            case Status.STATUS_PREPARING:
                break;
            case Status.STATUS_MARKED_ROLLBACK:
                // do nothing; we can pretend like it was registered, but it'll never be run anyway.
                return;
            default:
                throw new IllegalStateException("Syncs are not allowed to be registered when the tx is in state " + status);
        }

        // add the synchronization to the group that matches this package, if there is no matching group then add it to the
        // catch-all group (otherSyncs)
        String packageName = synchronization.getClass().getName();
        SynchronizationGroup synchGroup = otherSyncs;

        for (SynchronizationGroup g : synchGroups) {
            if (packageName.startsWith(g.packageName)) {
                synchGroup = g;
                break;
            }
        }

        synchGroup.add(synchronization);
    }

    /**
     * Exceptions from Synchronizations that are registered with this TSR are not trapped for before completion. This is because
     * an error in a Sync here should result in the transaction rolling back.
     */
    @Override
    public void beforeCompletion() {
        // run each group of synchs according to the order they were added to the list
        for (SynchronizationGroup g : synchGroups) {
            g.beforeCompletion();
        }
    }

    @Override
    public void afterCompletion(int status) {
        // run each group of synchs according to the order they were added to the list
        for (SynchronizationGroup g : synchGroups) {
            g.afterCompletion(status);
        }
    }
}
