package io.quarkus.narayana.jta.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class NarayanaQuarkusConfiguration {
    // non final properties
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.commitOnePhase", defaultValue = "true")
    public boolean commitOnePhase;
    /**
     * config
     */
    @ConfigItem(name = "ObjectStoreEnvironmentBean.objectStoreDir", defaultValue = "PutObjectStoreDirHere")
    public String objectStoreDir;
    /**
     * config
     */
    @ConfigItem(name = "ObjectStoreEnvironmentBean.transactionSync", defaultValue = "true")
    public boolean transactionSync;

    //    @ConfigItem(name = "CoreEnvironmentBean.nodeIdentifier", defaultValue = "1")
    //    public String nodeIdentifier;
    /**
     * config
     */
    @ConfigItem(name = "JTAEnvironmentBean.xaRecoveryNodes", defaultValue = "1")
    public List<String> xaRecoveryNodes;
    /**
     * config
     */
    @ConfigItem(name = "JTAEnvironmentBean.xaResourceOrphanFilterClassNames", defaultValue = "com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter,"
            + "com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter,"
            + "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinationManagerXAResourceOrphanFilter")
    public List<String> xaResourceOrphanFilterClassNames;
    /**
     * config
     */
    @ConfigItem(name = "CoreEnvironmentBean.socketProcessIdPort", defaultValue = "0")
    public int socketProcessIdPort;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryModuleClassNames", defaultValue = "com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule,"
            + "com.arjuna.ats.internal.txoj.recovery.TORecoveryModule,"
            + "com.arjuna.ats.internal.jta.recovery.arjunacore.SubordinateAtomicActionRecoveryModule,"
            + "com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule")
    public List<String> recoveryModuleClassNames;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.expiryScannerClassNames", defaultValue = "com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner")
    public List<String> expiryScannerClassNames;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryPort", defaultValue = "4712")
    public int recoveryPort;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryAddress")
    public String recoveryAddress;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.transactionStatusManagerPort", defaultValue = "0")
    public int transactionStatusManagerPort;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.transactionStatusManagerAddress")
    public String transactionStatusManagerAddress;
    /**
     * config
     */
    @ConfigItem(name = "RecoveryEnvironmentBean.recoveryListener", defaultValue = "true")
    public boolean recoveryListener;

    // non final properties
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.readonlyOptimisation", defaultValue = "true")
    public boolean readonlyOptimisation;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.startDisabled", defaultValue = "false")
    public boolean startDisabled;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.defaultTimeout", defaultValue = "60")
    public int defaultTimeout;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.beforeCompletionWhenRollbackOnly", defaultValue = "false")
    public boolean beforeCompletionWhenRollbackOnly;

    // final properties
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.maintainHeuristics", defaultValue = "true")
    public boolean maintainHeuristics;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.asyncCommit", defaultValue = "false")
    public boolean asyncCommit;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.asyncPrepare", defaultValue = "false")
    public boolean asyncPrepare;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.asyncRollback", defaultValue = "false")
    public boolean asyncRollback;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.asyncBeforeSynchronization", defaultValue = "false")
    public boolean asyncBeforeSynchronization;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.asyncAfterSynchronization", defaultValue = "false")
    public boolean asyncAfterSynchronization;
    /**
     * config
     */
    @ConfigItem(name = "CoordinatorEnvironmentBean.dynamic1PC", defaultValue = "true")
    public boolean dynamic1PC;

    //    @ConfigItem(name = "CoordinatorEnvironmentBean.transactionStatusManagerEnable", defaultValue = "false")
    //    public boolean transactionStatusManagerEnable;
}
