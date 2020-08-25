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

@ObjectClassDefinition(
    name = "Apache Sling Clam Events Servlet",
    description = "Servlet to stream Clam events via HTTP"
)
@SuppressWarnings("java:S100")
@interface ClamEventsServletConfiguration {

    @AttributeDefinition(
        name = "OSGi HTTP whiteboard servlet pattern",
        description = "Patterns under which this servlet is available"
    )
    String[] osgi_http_whiteboard_servlet_pattern() default {"/system/clam-events"};

    @AttributeDefinition(
        name = "Sling authentication requirements",
        description = "Additional Sling authentication requirements, defaults to Sling Clam Events Servlet pattern"
    )
    String[] sling_auth_requirements() default {"/system/clam-events"};

}
