/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.clam.oak.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;

import org.apache.jackrabbit.oak.plugins.observation.NodeObserver;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.clam.internal.ClamUtil.checkLength;
import static org.apache.sling.clam.internal.ClamUtil.properties;
import static org.apache.sling.clam.internal.ClamUtil.scanJobTopic;

/**
 * Service to observe JCR nodes for matching properties.
 */
@Component(
    service = Observer.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Node Observing JCR Property Digger",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = NodeObservingJcrPropertyDiggerConfiguration.class,
    factory = true
)
@SuppressWarnings("java:S3077")
public final class NodeObservingJcrPropertyDigger extends NodeObserver {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ResourceResolverFactory resourceResolverFactory;

    @Reference
    private volatile ThreadPoolManager threadPoolManager;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile JobManager jobManager;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ServiceUserMapped serviceUserMapped;

    private Pattern pattern;

    private int propertyType;

    private ThreadPool threadPool;

    private NodeObservingJcrPropertyDiggerConfiguration configuration;

    private final Logger logger = LoggerFactory.getLogger(NodeObservingJcrPropertyDigger.class);

    public NodeObservingJcrPropertyDigger() {
        super("/");
    }

    @Activate
    private void activate(final NodeObservingJcrPropertyDiggerConfiguration configuration) {
        logger.debug("activating");
        this.configuration = configuration;
        configure(configuration);
        threadPool = threadPoolManager.get(configuration.threadpool_name());
    }

    @Modified
    private void modified(final NodeObservingJcrPropertyDiggerConfiguration configuration) {
        logger.debug("modifying");
        this.configuration = configuration;
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivating");
        threadPoolManager.release(threadPool);
        configuration = null;
    }

    private void configure(final NodeObservingJcrPropertyDiggerConfiguration configuration) {
        pattern = Pattern.compile(configuration.property_path_pattern());
        propertyType = PropertyType.valueFromName(configuration.property_type());
    }

    @Override
    protected void added(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
        final Set<String> names = concat(added, changed);
        dig(path, names, pattern, propertyType, configuration.property_length_max(), commitInfo.getUserId());
    }

    @Override
    protected void deleted(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
    }

    @Override
    protected void changed(@NotNull final String path, @NotNull final Set<String> added, @NotNull final Set<String> deleted, @NotNull final Set<String> changed, @NotNull final Map<String, String> properties, @NotNull final CommitInfo commitInfo) {
        final Set<String> names = concat(added, changed);
        dig(path, names, pattern, propertyType, configuration.property_length_max(), commitInfo.getUserId());
    }

    private void dig(final String path, final Set<String> names, final Pattern pattern, final int propertyType, final long maxLength, final String userId) {
        final DigTask digTask = new DigTask(path, names, pattern, propertyType, maxLength, userId);
        threadPool.submit(digTask);
    }

    private Set<String> filter(final String path, final Set<String> names, final Pattern pattern) {
        final Set<String> paths = new HashSet<>();
        for (final String name : names) {
            final String p = String.format("%s/%s", path, name);
            if (pattern.matcher(p).matches()) {
                logger.debug("path '{}' matches pattern '{}'", p, pattern.pattern());
                paths.add(p);
            } else {
                logger.debug("path '{}' doesn't match pattern '{}'", p, pattern.pattern());
            }
        }
        return paths;
    }

    private Set<String> concat(final Set<String> a, final Set<String> b) {
        final Set<String> set = new HashSet<>(a);
        set.addAll(b);
        return set;
    }

    private class DigTask implements Runnable {

        private final String path;

        private final Set<String> names;

        private final Pattern pattern;

        private final int propertyType;

        private final long maxLength;

        private final String userId;

        DigTask(final String path, final Set<String> names, final Pattern pattern, final int propertyType, final long maxLength, final String userId) {
            this.path = path;
            this.names = names;
            this.pattern = pattern;
            this.propertyType = propertyType;
            this.maxLength = maxLength;
            this.userId = userId;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void run() {
            final Set<String> paths = filter(path, names, pattern);
            try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
                final Session session = resourceResolver.adaptTo(Session.class);
                assert session != null;
                for (final String path : paths) {
                    final Property property = session.getProperty(path);
                    final int propertyType = property.getType();
                    if (propertyType == this.propertyType) {
                        if (property.isMultiple()) { // multiple property values
                            final long[] lengths = property.getLengths();
                            for (int index = 0; index < lengths.length; index++) {
                                final long length = lengths[index];
                                if (checkLength(length, maxLength)) {
                                    jobManager.addJob(scanJobTopic(propertyType), properties(path, index, userId));
                                } else {
                                    logger.warn("Length of property '{}' [{}] greater than configured max length ({}).", path, index, maxLength);
                                }
                            }
                        } else { // single property value
                            if (checkLength(property.getLength(), maxLength)) {
                                jobManager.addJob(scanJobTopic(propertyType), properties(path, userId));
                            } else {
                                logger.warn("Length of property '{}' greater than configured max length ({}).", path, maxLength);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

}
