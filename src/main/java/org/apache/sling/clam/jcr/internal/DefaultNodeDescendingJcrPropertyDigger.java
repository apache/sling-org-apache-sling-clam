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
package org.apache.sling.clam.jcr.internal;

import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.apache.sling.clam.jcr.NodeDescendingJcrPropertyDigger;
import org.apache.sling.event.jobs.JobManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.clam.internal.ClamUtil.checkLength;
import static org.apache.sling.clam.internal.ClamUtil.properties;
import static org.apache.sling.clam.internal.ClamUtil.scanJobTopic;

@Component(
    service = NodeDescendingJcrPropertyDigger.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Default Node Descending JCR Property Digger",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
@SuppressWarnings("java:S3077")
public class DefaultNodeDescendingJcrPropertyDigger implements NodeDescendingJcrPropertyDigger {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile JobManager jobManager;

    private final Logger logger = LoggerFactory.getLogger(DefaultNodeDescendingJcrPropertyDigger.class);

    public DefaultNodeDescendingJcrPropertyDigger() { //
    }

    public void dig(@NotNull final Node node, @NotNull final Pattern pattern, @NotNull final Set<Integer> propertyTypes, final long maxLength, final int maxDepth) throws Exception {
        final int absoluteMaxDepth = maxDepth < 0 ? -1 : node.getDepth() + maxDepth;
        _dig(node, pattern, propertyTypes, maxLength, absoluteMaxDepth);
    }

    private void _dig(@NotNull final Node node, @NotNull final Pattern pattern, @NotNull final Set<Integer> propertyTypes, final long maxLength, final int maxDepth) throws Exception {
        final PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            final int propertyType = property.getType();
            final String path = property.getPath();
            if (propertyTypes.contains(propertyType) && pattern.matcher(path).matches()) {
                if (property.isMultiple()) { // multiple property values
                    final long[] lengths = property.getLengths();
                    for (int index = 0; index < lengths.length; index++) {
                        final long length = lengths[index];
                        if (checkLength(length, maxLength)) {
                            jobManager.addJob(scanJobTopic(propertyType), properties(path, index, null));
                        } else {
                            logger.warn("Length of property '{}' [{}] greater than given max length ({}).", path, index, maxLength);
                        }
                    }
                } else { // single property value
                    if (checkLength(property.getLength(), maxLength)) {
                        jobManager.addJob(scanJobTopic(propertyType), properties(path, null));
                    } else {
                        logger.warn("Length of property '{}' greater than given max length ({}).", path, maxLength);
                    }
                }
            }
        }
        if (maxDepth == -1 || node.getDepth() < maxDepth) {
            final NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                _dig(nodes.nextNode(), pattern, propertyTypes, maxLength, maxDepth);
            }
        }
    }

}
