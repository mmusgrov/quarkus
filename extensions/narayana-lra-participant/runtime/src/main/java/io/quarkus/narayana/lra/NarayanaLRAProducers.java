package io.quarkus.narayana.lra;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry;

@Dependent
public class NarayanaLRAProducers {
    @Produces
    public LRAParticipantRegistry lraParticipantRegistry() {
        return null;
    }
}
