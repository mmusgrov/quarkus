package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.Session;
import io.dekorate.SessionWriter;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import io.dekorate.kubernetes.generator.DefaultKubernetesApplicationGenerator;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.utils.Maps;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;

class KubernetesProcessor {

    private static final String PROPERTY_PREFIX = "dekorate";

    @Inject
    BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer;

    @Inject
    BuildProducer<FeatureBuildItem> featureProducer;

    @BuildStep(onlyIf = IsNormal.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig kubernetesConfig,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem)
            throws UnsupportedEncodingException {

        if (kubernetesPortBuildItems.isEmpty()) {
            return;
        }

        // The resources that dekorate's execution will result in, will later-on be written
        // by quarkus in the 'wiring-classes' directory
        // The location is needed in order to properly support s2i build triggering

        // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
        final Path root;
        try {
            root = Files.createTempDirectory("quarkus-kubernetes");
        } catch (IOException e) {
            throw new RuntimeException("Unable to setup environment for generating Kubernetes resources", e);
        }

        Config config = ConfigProvider.getConfig();
        Map<String, Object> configAsMap = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(k -> k.startsWith(PROPERTY_PREFIX))
                .collect(Collectors.toMap(k -> k, k -> config.getValue(k, String.class)));

        final SessionWriter sessionWriter = new SimpleFileWriter(root, false);
        Project project = FileProjectFactory.create(new File("."));
        sessionWriter.setProject(project);
        final Session session = Session.getSession();
        session.setWriter(sessionWriter);
        session.feed(Maps.fromProperties(configAsMap));

        final Map<String, Integer> ports = verifyPorts(kubernetesPortBuildItems);
        enableKubernetes(applicationInfo,
                kubernetesConfig,
                ports,
                kubernetesHealthLivenessPathBuildItem.map(i -> i.getPath()),
                kubernetesHealthReadinessPathBuildItem.map(i -> i.getPath()));

        // write the generated resources to the filesystem
        final Map<String, String> generatedResourcesMap = session.close();
        for (Map.Entry<String, String> resourceEntry : generatedResourcesMap.entrySet()) {
            String relativePath = resourceEntry.getKey().replace(root.toAbsolutePath() + "/", "kubernetes/");
            if (relativePath.startsWith("kubernetes/.")) { // ignore some of Dekorate's internal files
                continue;
            }
            generatedResourceProducer.produce(
                    new GeneratedFileSystemResourceBuildItem(
                            // we need to make sure we are only passing the relative path to the build item
                            relativePath,
                            resourceEntry.getValue().getBytes("UTF-8")));
        }

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES));
    }

    private Map<String, Integer> verifyPorts(List<KubernetesPortBuildItem> kubernetesPortBuildItems) {
        final Map<String, Integer> result = new HashMap<>();
        final Set<Integer> usedPorts = new HashSet<>();
        for (KubernetesPortBuildItem entry : kubernetesPortBuildItems) {
            final String name = entry.getName();
            if (result.containsKey(name)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must have unique names - " + name + "has been used multiple times");
            }
            final Integer port = entry.getPort();
            if (usedPorts.contains(port)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must be unique - " + port + "has been used multiple times");
            }
            result.put(name, port);
            usedPorts.add(port);
        }
        return result;
    }

    private void enableKubernetes(ApplicationInfoBuildItem applicationInfo, KubernetesConfig kubernetesConfig,
            Map<String, Integer> portsMap, Optional<String> httpLivenessProbePath,
            Optional<String> httpReadinessProbePath) {
        final Map<String, Object> kubernetesProperties = new HashMap<>();
        kubernetesProperties.put("group", kubernetesConfig.group);
        kubernetesProperties.put("name", applicationInfo.getName());
        kubernetesProperties.put("version", applicationInfo.getVersion());

        final List<Map<String, Object>> ports = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : portsMap.entrySet()) {
            final Map<String, Object> portProperties = new HashMap<>();
            portProperties.put("name", entry.getKey());
            portProperties.put("containerPort", entry.getValue());
            ports.add(portProperties);
        }

        kubernetesProperties.put("ports", toArray(ports));
        if (httpLivenessProbePath.isPresent()) {
            Map<String, Object> livenessProbeProperties = new HashMap<>();
            livenessProbeProperties.put("httpActionPath", httpLivenessProbePath.get());
            kubernetesProperties.put("livenessProbe", livenessProbeProperties);
        }
        if (httpReadinessProbePath.isPresent()) {
            Map<String, Object> readinessProbeProperties = new HashMap<>();
            readinessProbeProperties.put("httpActionPath", httpReadinessProbePath.get());
            kubernetesProperties.put("readinessProbe", readinessProbeProperties);
        }

        final DefaultKubernetesApplicationGenerator generator = new DefaultKubernetesApplicationGenerator();
        final Map<String, Object> generatorInput = new HashMap<>();
        generatorInput.put(KubernetesApplication.class.getName(), kubernetesProperties);
        generator.add(generatorInput);
    }

    private <T> T[] toArray(List<T> list) {
        Class clazz = list.get(0).getClass();
        T[] array = (T[]) java.lang.reflect.Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }
}
