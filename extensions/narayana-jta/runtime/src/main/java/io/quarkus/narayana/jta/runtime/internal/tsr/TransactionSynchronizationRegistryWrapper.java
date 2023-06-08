package io.quarkus.narayana.jta.runtime.internal.tsr;

import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

/**
 * Agroal registers an interposed synchronization which validates that connections have been released.
 * Components such as hibernate release connections in an interposed synchronization.
 * Therefore, we must ensure that Agroal runs last.
 * <p>
 *
 * This wrapper re-orders interposed synchronizations as follows: [ARC, other, hibernate, Agroal].
 * <p>
 *
 * Synchronizations are placed into groups according to their package name and the groups are ordered, so this means
 * that all ARC synchs run first, then other synchs (ie not hibernate or Agroal) and then hibernate synchs and finally
 * Agroal synchs are ran last.
 * <p>
 *
 * See {@code AgroalOrderedLastSynchronizationList} for details of the re-ordering.
 * <p>
 *
 * Synchronizations in a given group are processed in the order in which they were registered.
 */
public class TransactionSynchronizationRegistryWrapper implements TransactionSynchronizationRegistry {

    private final Object key = new Object();
    private static final Logger LOG = Logger.getLogger(TransactionSynchronizationRegistryWrapper.class);

    private final TransactionSynchronizationRegistryImple tsr;
    private transient com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple delegate;

    public TransactionSynchronizationRegistryWrapper(
            TransactionSynchronizationRegistryImple transactionSynchronizationRegistryImple) {
        this.tsr = transactionSynchronizationRegistryImple;
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) throws IllegalStateException {
        AgroalOrderedLastSynchronizationList agroalOrderedLastSynchronization = (AgroalOrderedLastSynchronizationList) tsr
                .getResource(key);

        if (agroalOrderedLastSynchronization == null) {
            synchronized (key) {
                agroalOrderedLastSynchronization = (AgroalOrderedLastSynchronizationList) tsr.getResource(key);
                if (agroalOrderedLastSynchronization == null) {
                    agroalOrderedLastSynchronization = new AgroalOrderedLastSynchronizationList(this);

                    tsr.putResource(key, agroalOrderedLastSynchronization);
                    tsr.registerInterposedSynchronization(agroalOrderedLastSynchronization);
                }
            }
        }

        try {
            // add the synchronization to the list that does the reordering
            agroalOrderedLastSynchronization.registerInterposedSynchronization(sync);
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object getTransactionKey() {
        return tsr.getTransactionKey();
    }

    @Override
    public int getTransactionStatus() {
        return tsr.getTransactionStatus();
    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        return tsr.getRollbackOnly();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        tsr.setRollbackOnly();
    }

    @Override
    public Object getResource(Object key) throws IllegalStateException {
        return tsr.getResource(key);
    }

    @Override
    public void putResource(Object key, Object value) throws IllegalStateException {
        tsr.putResource(key, value);
    }
}
