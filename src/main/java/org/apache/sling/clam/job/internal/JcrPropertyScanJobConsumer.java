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
package org.apache.sling.clam.job.internal;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.clam.internal.ClamUtil;
import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ClamService;
import org.apache.sling.commons.clam.ScanResult;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to consume jobs and scan data with Clam service.
 */
@Component(
    property = {
        JobExecutor.PROPERTY_TOPICS + "=org/apache/sling/clam/scan/jcr/property/*",
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam JCR Property Scan Job Consumer",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@SuppressWarnings("java:S3077")
public final class JcrPropertyScanJobConsumer implements JobConsumer {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ResourceResolverFactory resourceResolverFactory;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile ClamService clamService;

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<JcrPropertyScanResultHandler> scanResultHandlers;

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    @SuppressWarnings("unused")
    private volatile ServiceUserMapped serviceUserMapped;

    private final Logger logger = LoggerFactory.getLogger(JcrPropertyScanJobConsumer.class);

    public JcrPropertyScanJobConsumer() { //
    }

    @Activate
    @SuppressWarnings("unused")
    private void activate() {
        logger.debug("activating");
    }

    @Modified
    @SuppressWarnings("unused")
    private void modified() {
        logger.debug("modifying");
    }

    @Deactivate
    @SuppressWarnings("unused")
    private void deactivate() {
        logger.debug("deactivating");
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public JobResult process(final Job job) {
        logger.debug("processing property scan job: {}", job);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(null)) {
            final String path = job.getProperty(ClamUtil.PROPERTY_PATH, String.class);
            final String userId = job.getProperty(ClamUtil.USER_ID, String.class);
            final Session session = resourceResolver.adaptTo(Session.class);
            assert session != null;
            final Property property = session.getProperty(path);
            final int propertyType = property.getType();
            if (property.isMultiple()) { // multiple property values
                final int index = job.getProperty(ClamUtil.VALUE_INDEX, Integer.class);
                final Value[] values = property.getValues();
                final Value value = values[index];
                try (InputStream inputStream = getInputStream(value)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("scanning property of type {} at {} [{}]", PropertyType.nameFromValue(propertyType), path, index);
                    }
                    final ScanResult scanResult = clamService.scan(inputStream);
                    invokeScanResultHandlers(scanResult, path, index, propertyType, userId);
                }
            } else { // single property value
                try (InputStream inputStream = getInputStream(property.getValue())) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("scanning property of type {} at {}", PropertyType.nameFromValue(propertyType), path);
                    }
                    final ScanResult scanResult = clamService.scan(inputStream);
                    invokeScanResultHandlers(scanResult, path, null, propertyType, userId);
                }
            }
            return JobResult.OK;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return JobResult.FAILED;
        }
    }

    @SuppressWarnings("java:S112")
    private InputStream getInputStream(final Value value) throws Exception {
        final int propertyType = value.getType();
        if (propertyType == PropertyType.BINARY) {
            return value.getBinary().getStream();
        } else {
            return IOUtils.toInputStream(value.getString(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void invokeScanResultHandlers(final ScanResult scanResult, final String path, final Integer index, final int propertyType, final String userId) {
        if (scanResultHandlers != null) {
            for (final JcrPropertyScanResultHandler scanResultHandler : scanResultHandlers) {
                try {
                    if (index == null) { // single-value property
                        logger.debug("invoking scan result handler {} for single-value property: {}, {}, {}", scanResultHandler, path, propertyType, userId);
                        scanResultHandler.handleJcrPropertyScanResult(scanResult, path, propertyType, userId);
                    } else { // multi-value property
                        logger.debug("invoking scan result handler {} for multi-value property: {}, {}, {}, {}", scanResultHandler, path, index, propertyType, userId);
                        scanResultHandler.handleJcrPropertyScanResult(scanResult, path, index, propertyType, userId);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
