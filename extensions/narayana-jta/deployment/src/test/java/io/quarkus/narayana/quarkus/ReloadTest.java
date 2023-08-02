package io.quarkus.narayana.quarkus;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

public class ReloadTest {
    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> {
                jar
                        .add(new StringAsset("greeting.message=hello\n"),
                                "application.properties");
            });

    @Test
    public void testHotReplacement() {
        test.modifyResourceFile("application.properties", s -> s.replace("hello", "goodbye"));
    }
}
