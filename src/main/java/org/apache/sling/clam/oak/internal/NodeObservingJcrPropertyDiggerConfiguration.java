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

import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
    name = "Apache Sling Clam Node Observing JCR Property Digger",
    description = "Observes the node store and adds scan jobs for matching JCR properties"
)
@SuppressWarnings("java:S100")
@interface NodeObservingJcrPropertyDiggerConfiguration {

    @AttributeDefinition(
        name = "property type",
        description = "Type of properties",
        options = {
            @Option(label = "Binary", value = "Binary"),
            @Option(label = "String", value = "String"),
            @Option(label = "URI", value = "URI")
        }
    )
    String property_type() default "Binary";

    @AttributeDefinition(
        name = "property path pattern",
        description = "Pattern a property path has to match, e.g. '^/content/.*/jcr:content/jcr:data$'"
    )
    String property_path_pattern() default "^/.*$";

    @AttributeDefinition(
        name = "property length max",
        description = "Max length of property value, -1 for unlimited length. Scanning data greater 4GB may result in errors due to limitations in Clam."
    )
    long property_length_max() default -1L;

    @AttributeDefinition(
        name = "threadpool name",
        description = "Name of the ThreadPool to use for digging"
    )
    String threadpool_name() default ThreadPoolManager.DEFAULT_THREADPOOL_NAME;

    String webconsole_configurationFactory_nameHint() default "{property.type}:{property.path.pattern}";

}
