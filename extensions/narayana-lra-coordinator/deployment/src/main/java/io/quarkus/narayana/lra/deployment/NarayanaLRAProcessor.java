package io.quarkus.narayana.stm.deployment;

import javax.inject.Inject;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.narayana.lra.NarayanaLRAProducers;

class NarayanaLRAProcessor {
    @Inject
    BuildProducer<AdditionalBeanBuildItem> additionalBeans;

    @BuildStep
    void registerFeature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.NARAYANA_LRA_COORDINATOR));

        additionalBeans.produce(new AdditionalBeanBuildItem(NarayanaLRAProducers.class));
    }

    @BuildStep
    BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
        return new BeanDefiningAnnotationBuildItem(
                DotName.createSimple("io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantRegistry"));
    }
}
