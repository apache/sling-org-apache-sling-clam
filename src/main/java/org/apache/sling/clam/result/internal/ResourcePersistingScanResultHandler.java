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
package org.apache.sling.clam.result.internal;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.jcr.PropertyType;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ScanResult;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.nodetype.NodeType.MIX_CREATED;
import static javax.jcr.nodetype.NodeType.NT_UNSTRUCTURED;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.sling.clam.internal.ClamUtil.properties;

/**
 * Service to persist scan results as <code>Resource</code>.
 */
@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Resource Persisting Scan Result Handler",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = ResourcePersistingScanResultHandlerConfiguration.class,
    factory = true
)
@SuppressWarnings("java:S3077")
public final class ResourcePersistingScanResultHandler implements JcrPropertyScanResultHandler {

    private static final String PATH_DATE_PATTERN = "yyyy/MM/dd/HH/mm/ss";

    private static final String NT_SLING_ORDERED_FOLDER = "sling:OrderedFolder";

    private static final String SLING_RESOURCE_TYPE_PROPERTY = "sling:resourceType";

    private static final String JCR_RESULT_RESOURCE_TYPE = "sling/clam/jcr/result";

    private static final String SUBSERVICE = "result-writer";

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ResourceResolverFactory resourceResolverFactory;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUBSERVICE + ")"
    )
    @SuppressWarnings("unused")
    private volatile ServiceUserMapped serviceUserMapped;

    private ResourcePersistingScanResultHandlerConfiguration configuration;

    private final Logger logger = LoggerFactory.getLogger(ResourcePersistingScanResultHandler.class);

    public ResourcePersistingScanResultHandler() { //
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate(final ResourcePersistingScanResultHandlerConfiguration configuration) {
        logger.debug("activating");
        this.configuration = configuration;
    }

    @Modified
    @SuppressWarnings("unused")
    private void modified(final ResourcePersistingScanResultHandlerConfiguration configuration) {
        logger.debug("modifying");
        this.configuration = configuration;
    }

    @Deactivate
    @SuppressWarnings("unused")
    private void deactivate() {
        logger.debug("deactivating");
        this.configuration = null;
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int propertyType, @Nullable final String userId) {
        if (checkPersist(scanResult)) {
            persistResult(scanResult, path, null, propertyType, userId);
        }
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int index, final int propertyType, @Nullable final String userId) {
        if (checkPersist(scanResult)) {
            persistResult(scanResult, path, index, propertyType, userId);
        }
    }

    private boolean checkPersist(final ScanResult scanResult) {
        return !scanResult.isOk() || configuration.result_status_ok_persist();
    }

    private void persistResult(@NotNull final ScanResult scanResult, @NotNull final String path, final Integer index, final int propertyType, @Nullable final String userId) {
        try (ResourceResolver resourceResolver = serviceResourceResolver()) {
            final Map<String, Object> properties = properties(path, index, userId, scanResult);
            properties.put(JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
            properties.put(JCR_MIXINTYPES, MIX_CREATED);
            properties.put(SLING_RESOURCE_TYPE_PROPERTY, JCR_RESULT_RESOURCE_TYPE);
            final Resource parent = getOrCreateParent(resourceResolver);
            final String name = String.format("%s-%s", PropertyType.nameFromValue(propertyType), UUID.randomUUID());
            final Resource result = resourceResolver.create(parent, name, properties);
            resourceResolver.commit();
            if (index == null) {
                logger.debug("Scan result for {} persisted at {}.", path, result.getPath());
            } else {
                logger.debug("Scan result for {} [{}] persisted at {}.", path, index, result.getPath());
            }
        } catch (LoginException | PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    private Resource getOrCreateParent(final ResourceResolver resourceResolver) throws PersistenceException {
        final SimpleDateFormat format = new SimpleDateFormat(PATH_DATE_PATTERN);
        final String path = String.format("%s/%s", configuration.result_root_path(), format.format(new Date()));
        return ResourceUtil.getOrCreateResource(resourceResolver, path, NT_SLING_ORDERED_FOLDER, NT_SLING_ORDERED_FOLDER, true);
    }

    private ResourceResolver serviceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE));
    }

}
