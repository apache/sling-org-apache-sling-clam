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

import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ScanResult;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.clam.internal.ClamUtil.properties;
import static org.apache.sling.clam.internal.ClamUtil.resultEventTopic;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Event Publishing Scan Result Handler",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@Designate(
    ocd = EventPublishingScanResultHandlerConfiguration.class
)
@SuppressWarnings("java:S3077")
public final class EventPublishingScanResultHandler implements JcrPropertyScanResultHandler {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile EventAdmin eventAdmin;

    private EventPublishingScanResultHandlerConfiguration configuration;

    private final Logger logger = LoggerFactory.getLogger(EventPublishingScanResultHandler.class);

    public EventPublishingScanResultHandler() { //
    }

    @Activate
    private void activate(final EventPublishingScanResultHandlerConfiguration configuration) {
        logger.debug("activating");
        this.configuration = configuration;
    }

    @Modified
    private void modified(final EventPublishingScanResultHandlerConfiguration configuration) {
        logger.debug("modifying");
        this.configuration = configuration;
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivating");
        this.configuration = null;
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int propertyType, @Nullable final String userId) {
        if (checkPublish(scanResult)) {
            final Event event = new Event(resultEventTopic(propertyType), properties(path, userId, scanResult));
            eventAdmin.postEvent(event);
        }
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int index, final int propertyType, @Nullable final String userId) {
        if (checkPublish(scanResult)) {
            final Event event = new Event(resultEventTopic(propertyType), properties(path, index, userId, scanResult));
            eventAdmin.postEvent(event);
        }
    }

    private boolean checkPublish(final ScanResult scanResult) {
        return !scanResult.isOk() || configuration.result_status_ok_publish();
    }

}
