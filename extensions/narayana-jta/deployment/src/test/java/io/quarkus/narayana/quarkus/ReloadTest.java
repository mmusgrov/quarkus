package io.quarkus.narayana.quarkus;

import jakarta.enterprise.context.control.ActivateRequestContext;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.QuarkusUnitTest;

public class ReloadTest {
    //    @RegisterExtension
    static final QuarkusUnitTest configWorks = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));
    //    @RegisterExtension
    static final QuarkusDevModeTest config1 = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withApplicationRoot((jar) -> jar
                    .addAsResource("reload.properties", "application.properties"));
    //    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withApplicationRoot((jar) -> jar
                    .addAsResource("reload.properties", "application.properties"));

    //    @RegisterExtension // produces a NPE at QuarkusTransactionImpl.java:222
    final static QuarkusDevModeTest config3 = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("reload.properties", "application.properties"));

    @RegisterExtension // produces a NPE at QuarkusTransactionImpl.java:222
    static final QuarkusDevModeTest config4 = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addAsResource("reload.properties", "application.properties"));

    @Test
    @ActivateRequestContext
    public void testHotReplacement() {
        //        QuarkusTransaction.begin();
        //        QuarkusTransaction.commit();
        config4.modifyResourceFile("application.properties", s -> s.replace("hello", "goodbye"));
        /*
         * config.modifyResourceFile("application.properties", s -> s.replace("hello", "goodbye"));
         * config1.modifyResourceFile("application.properties", s -> s.replace("hello", "goodbye"));
         * config2.modifyResourceFile("application.properties", s -> s.replace("hello", "goodbye"));
         * System.out.printf("done%n");
         */
    }
}
