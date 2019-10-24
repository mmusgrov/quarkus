package io.quarkus.narayana.stm.deployment;

import javax.inject.Inject;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.narayana.lra.NarayanaLRAProducers;

class NarayanaLRAProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit;

    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(
                DotName.createSimple("io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry"));
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_LRA_PARTICIPANT));

        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        builder.addBeanClass(Compensate.class);
        builder.addBeanClass(Complete.class);
        builder.addBeanClass(Status.class);
        builder.addBeanClass(Forget.class);
        builder.addBeanClass(LRA.class);
        builder.addBeanClass(AfterLRA.class);
        builder.addBeanClass(NarayanaLRAProducers.class);
        additionalBeans.produce(builder.build());
    }
}
