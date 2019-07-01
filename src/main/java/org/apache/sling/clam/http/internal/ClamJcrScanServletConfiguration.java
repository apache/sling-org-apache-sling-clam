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
package org.apache.sling.clam.http.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
    name = "Apache Sling Clam JCR Scan Servlet",
    description = "..."
)
@interface ClamJcrScanServletConfiguration {

    @AttributeDefinition(
        name = "scan authorized groups",
        description = "User groups authorized for scanning"
    )
    String[] scan_authorized_groups() default {};

    @AttributeDefinition(
        name = "default property types",
        description = "Type of properties",
        options = {
            @Option(label = "Binary", value = "Binary"),
            @Option(label = "String", value = "String")
        }
    )
    String[] digger_default_property_types() default {"Binary"};

    @AttributeDefinition(
        name = "default property path pattern",
        description = "Pattern a property path has to match, e.g. '^/content/.*/jcr:content/jcr:data$'"
    )
    String digger_default_property_path_pattern() default "^/.*$";

    @AttributeDefinition(
        name = "default property length max",
        description = "Max length of property value to scan, -1 for unlimited length. Scanning data greater 4GB may result in errors due to limitations in Clam."
    )
    long digger_default_property_length_max() default -1L;

    @AttributeDefinition(
        name = "default node depth max",
        description = "Max depth of nodes below given path to scan, -1 for unlimited depth."
    )
    int digger_default_node_depth_max() default -1;

}
