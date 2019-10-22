package io.quarkus.narayana.jta.runtime;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NarayanaJtaRecorder {

    private static final CoordinatorEnvironmentBean coordinatorEnvironmentBean = new CoordinatorEnvironmentBean();
    private static final RecoveryEnvironmentBean recoveryEnvironmentBean = new RecoveryEnvironmentBean();
    private static final CoreEnvironmentBean coreEnvironmentBean = new CoreEnvironmentBean();
    private static final JTAEnvironmentBean jtaEnvironmentBean = new JTAEnvironmentBean();
    private static final ObjectStoreEnvironmentBean objectStoreEnvironmentBean = new ObjectStoreEnvironmentBean();

    private void initDefaultPropertyBeans() {
        BeanPopulator.setBeanInstanceIfAbsent(coordinatorEnvironmentBean.getClass().getName(), coordinatorEnvironmentBean);
        BeanPopulator.setBeanInstanceIfAbsent(recoveryEnvironmentBean.getClass().getName(), recoveryEnvironmentBean);
        BeanPopulator.setBeanInstanceIfAbsent(coreEnvironmentBean.getClass().getName(), coreEnvironmentBean);
        BeanPopulator.setBeanInstanceIfAbsent(jtaEnvironmentBean.getClass().getName(), jtaEnvironmentBean);

        BeanPopulator.setBeanInstanceIfAbsent(objectStoreEnvironmentBean.getClass().getName(), objectStoreEnvironmentBean);
        BeanPopulator.setBeanInstanceIfAbsent(objectStoreEnvironmentBean.getClass().getName() + ":stateStore",
                objectStoreEnvironmentBean);
        BeanPopulator.setBeanInstanceIfAbsent(objectStoreEnvironmentBean.getClass().getName() + ":communicationStore",
                objectStoreEnvironmentBean);
    }

    public void setNodeName(final TransactionManagerConfiguration transactions) {
        try {
            arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(transactions.nodeName);
            TxControl.setXANodeName(transactions.xaNodeName.orElse(transactions.nodeName));
        } catch (CoreEnvironmentBeanException e) {
            e.printStackTrace();
        }
    }

    public void setDefaultTimeout(TransactionManagerConfiguration transactions) {
        transactions.defaultTransactionTimeout.ifPresent(defaultTimeout -> {
            TxControl.setDefaultTimeout((int) defaultTimeout.getSeconds());
        });
    }

    public void disableTransactionStatusManager() {
        arjPropertyManager.getCoordinatorEnvironmentBean()
                .setTransactionStatusManagerEnable(false);
    }

    public void setConfig(TransactionManagerConfiguration transactions, NarayanaQuarkusConfiguration config) {
        // directly set the Narayana property beans to avoid the need to parse property files
        initDefaultPropertyBeans();

        // This must be done before setNodeName as the code in setNodeName will create a TSM based on the value of this
        disableTransactionStatusManager();
        setNodeName(transactions);
        setDefaultTimeout(transactions);

        coordinatorEnvironmentBean.setCommitOnePhase(config.commitOnePhase);
        coordinatorEnvironmentBean.setReadonlyOptimisation(config.readonlyOptimisation);
        coordinatorEnvironmentBean.setStartDisabled(config.startDisabled);
        coordinatorEnvironmentBean.setDefaultTimeout(config.defaultTimeout);
        coordinatorEnvironmentBean.setBeforeCompletionWhenRollbackOnly(config.beforeCompletionWhenRollbackOnly);
        coordinatorEnvironmentBean.setMaintainHeuristics(config.maintainHeuristics);
        coordinatorEnvironmentBean.setAsyncCommit(config.asyncCommit);
        coordinatorEnvironmentBean.setAsyncPrepare(config.asyncPrepare);
        coordinatorEnvironmentBean.setAsyncRollback(config.asyncRollback);
        coordinatorEnvironmentBean.setAsyncBeforeSynchronization(config.asyncBeforeSynchronization);
        coordinatorEnvironmentBean.setAsyncAfterSynchronization(config.asyncAfterSynchronization);
        coordinatorEnvironmentBean.setDynamic1PC(config.dynamic1PC);

        objectStoreEnvironmentBean.setObjectStoreDir(config.objectStoreDir);
        objectStoreEnvironmentBean.setTransactionSync(config.transactionSync);

        coreEnvironmentBean.setSocketProcessIdPort(config.socketProcessIdPort);

        jtaEnvironmentBean.setXaRecoveryNodes(config.xaRecoveryNodes);
        jtaEnvironmentBean.setXaResourceOrphanFilterClassNames(config.xaResourceOrphanFilterClassNames);

        recoveryEnvironmentBean.setRecoveryModuleClassNames(config.recoveryModuleClassNames);
        recoveryEnvironmentBean.setExpiryScannerClassNames(config.expiryScannerClassNames);
        recoveryEnvironmentBean.setRecoveryPort(config.recoveryPort);
        recoveryEnvironmentBean.setRecoveryAddress(config.recoveryAddress);
        recoveryEnvironmentBean.setTransactionStatusManagerPort(config.transactionStatusManagerPort);
        recoveryEnvironmentBean.setTransactionStatusManagerAddress(config.transactionStatusManagerAddress);
        recoveryEnvironmentBean.setRecoveryListener(config.recoveryListener);
    }
}
