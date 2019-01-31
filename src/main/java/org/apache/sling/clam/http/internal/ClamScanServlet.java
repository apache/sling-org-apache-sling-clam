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

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.clam.jcr.NodeDescendingJcrPropertyDigger;
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

import static org.apache.sling.clam.http.internal.RequestUtil.isAuthorized;
import static org.apache.sling.clam.http.internal.RequestUtil.maxLength;
import static org.apache.sling.clam.http.internal.RequestUtil.path;
import static org.apache.sling.clam.http.internal.RequestUtil.pattern;
import static org.apache.sling.clam.http.internal.RequestUtil.propertyTypes;
import static org.apache.sling.clam.http.internal.ResponseUtil.handleError;
import static org.apache.sling.clam.internal.ClamUtil.propertyTypesFromNames;

@Component(
    service = Servlet.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Clam Scan Servlet",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        "sling.servlet.paths=/bin/clam/scan",
        "sling.auth.requirements=/bin/clam/scan"
    }
)
@Designate(
    ocd = ClamScanServletConfiguration.class
)
public class ClamScanServlet extends SlingAllMethodsServlet {

    @Reference(
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile NodeDescendingJcrPropertyDigger digger;

    private ClamScanServletConfiguration configuration;

    private Pattern pattern;

    private Set<Integer> propertyTypes;

    private final Logger logger = LoggerFactory.getLogger(ClamScanServlet.class);

    public ClamScanServlet() {
    }

    @Activate
    private void activate(final ClamScanServletConfiguration configuration) throws Exception {
        logger.debug("activating");
        this.configuration = configuration;
        configure(configuration);
    }

    @Modified
    private void modified(final ClamScanServletConfiguration configuration) throws Exception {
        logger.debug("modifying");
        this.configuration = configuration;
        configure(configuration);
    }

    @Deactivate
    private void deactivate() {
        logger.debug("deactivating");
        configuration = null;
        pattern = null;
        propertyTypes = null;
    }

    private void configure(final ClamScanServletConfiguration configuration) throws Exception {
        pattern = Pattern.compile(configuration.digger_default_property_path_pattern());
        propertyTypes = propertyTypesFromNames(configuration.digger_default_property_types());
    }

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) throws ServletException, IOException {
        boolean isAuthorized = false;
        try {
            isAuthorized = isAuthorized(request, Arrays.asList(configuration.scan_authorized_groups()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (!isAuthorized) {
            handleError(response, 403, null);
            return;
        }

        final String path;
        final Pattern pattern;
        final Set<Integer> propertyTypes;
        final long maxLength;
        final int maxDepth;
        try {
            path = path(request);
            pattern = pattern(request, this.pattern);
            propertyTypes = propertyTypes(request, this.propertyTypes);
            maxLength = maxLength(request, configuration.digger_default_property_length_max());
            maxDepth = RequestUtil.maxDepth(request, configuration.digger_default_node_depth_max());
        } catch (Exception e) {
            response.sendError(400, e.getMessage());
            return;
        }

        final Resource resource = request.getResourceResolver().getResource(path);
        if (resource == null) {
            response.sendError(400, "No resource at given path found: " + path);
            return;
        }

        final Node node = resource.adaptTo(Node.class);
        if (node == null) {
            response.sendError(400, "Resource at given path is not a Node: " + path);
            return;
        }

        try {
            digger.dig(node, pattern, propertyTypes, maxLength, maxDepth);
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
    }

}
