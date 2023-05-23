package io.quarkus.narayana.jta.runtime.internal.tsr;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

/**
 * This class was added to:
 *
 * 1. workaround an issue discussed in https://java.net/jira/browse/JTA_SPEC-4 whereby the Jakarta Connectors Synchronization(s)
 * need to be
 * called after the Jakarta Persistence Synchronization(s). Currently the implementation orders Jakarta Connectors relative to
 * all interposed Synchronizations,
 * if this is not desirable it would be possible to modify this class to store just the Jakarta Persistence and Jakarta
 * Connectors syncs and the other syncs
 * can simply be passed to a delegate (would need the reference to this in the constructor).
 *
 * 2. During afterCompletion the Jakarta Connectors synchronizations should be called last as that allows Jakarta Connectors to
 * detect connection leaks from
 * frameworks that have not closed the Jakarta Connectors managed resources. This is described in (for example)
 * http://docs.oracle.com/javaee/5/api/javax/transaction/TransactionSynchronizationRegistry
 * .html#registerInterposedSynchronization(jakarta.transaction.Synchronization) where it says that during afterCompletion
 * "Resources can be closed but no transactional work can be performed with them"
 */
public class AgroalOrderedLastSynchronizationList implements Synchronization {
    private static final Logger LOGGER = Logger.getLogger(AgroalOrderedLastSynchronizationList.class);
    private static final String ARC_PKG_NAME = "io.quarkus.arc.impl";
    private static final String AGROAL_PKG_NAME = "io.agroal.narayana";
    private final List<Synchronization> agroalSyncs = new ArrayList<Synchronization>();
    private final List<Synchronization> otherSyncs = new ArrayList<Synchronization>();
    private final List<Synchronization> arqSyncs = new ArrayList<Synchronization>();
    private final TransactionSynchronizationRegistry tsr;

    public AgroalOrderedLastSynchronizationList(
            TransactionSynchronizationRegistryWrapper transactionSynchronizationRegistryWrapper) {
        this.tsr = transactionSynchronizationRegistryWrapper;
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

        if (synchronization.getClass().getName().startsWith(AGROAL_PKG_NAME)) {
            agroalSyncs.add(synchronization);
        } else if (synchronization.getClass().getName().startsWith(ARC_PKG_NAME)) {
            arqSyncs.add(synchronization);
        } else {
            otherSyncs.add(synchronization);
        }
    }

    /**
     * Exceptions from Synchronizations that are registered with this TSR are not trapped for before completion. This is because
     * an error in a Sync here should result in the transaction rolling back.
     */
    @Override
    public void beforeCompletion() {
        // run the ARC syncs first and the Agroal syncs last
        runBeforeSynchs(arqSyncs);
        runBeforeSynchs(otherSyncs);
        runBeforeSynchs(agroalSyncs);
    }

    @Override
    public void afterCompletion(int status) {
        runAfterSynchs(arqSyncs, status);
        runAfterSynchs(otherSyncs, status);
        runAfterSynchs(agroalSyncs, status); // Agroal validates that connection (wrappers) are closed at the right time
    }

    private void runBeforeSynchs(List<Synchronization> synchs) {
        for (Synchronization sync : synchs) {
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
    }

    private void runAfterSynchs(List<Synchronization> synchs, int status) {
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
