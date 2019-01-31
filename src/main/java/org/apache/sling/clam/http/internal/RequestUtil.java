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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.jetbrains.annotations.NotNull;

import static org.apache.sling.clam.internal.ClamUtil.propertyTypesFromNames;

public class RequestUtil {

    private RequestUtil() {
    }

    static String path(@NotNull final SlingHttpServletRequest request) throws Exception {
        final String path = request.getParameter("path");
        if (path == null) {
            throw new Exception("Mandatory parameter path is missing");
        } else {
            return path;
        }
    }

    static Pattern pattern(@NotNull final SlingHttpServletRequest request, @NotNull final Pattern pattern) throws Exception {
        final String parameter = request.getParameter("pattern");
        if (parameter == null) {
            return pattern;
        } else {
            try {
                return Pattern.compile(parameter);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for pattern: " + parameter);
            }
        }
    }

    static Set<Integer> propertyTypes(@NotNull final SlingHttpServletRequest request, @NotNull final Set<Integer> propertyTypes) throws Exception {
        final String[] parameter = request.getParameterValues("propertyType");
        if (parameter == null || parameter.length == 0) {
            return propertyTypes;
        }
        try {
            return propertyTypesFromNames(parameter);
        } catch (Exception e) {
            throw new Exception("Invalid parameter value for propertyType: " + Arrays.toString(parameter));
        }
    }

    static long maxLength(@NotNull final SlingHttpServletRequest request, final long maxLength) throws Exception {
        final String parameter = request.getParameter("maxLength");
        if (parameter == null) {
            return maxLength;
        } else {
            try {
                return Long.parseLong(parameter);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for maxLength: " + parameter);
            }
        }
    }

    static int maxDepth(@NotNull final SlingHttpServletRequest request, final int maxDepth) throws Exception {
        final String parameter = request.getParameter("maxDepth");
        if (parameter == null) {
            return maxDepth;
        } else {
            try {
                return Integer.parseInt(parameter);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for maxDepth: " + parameter);
            }
        }
    }

    static boolean isAuthorized(@NotNull final SlingHttpServletRequest request, @NotNull final Collection<String> authorizedGroups) throws Exception {
        final Authorizable authorizable = request.getResourceResolver().adaptTo(Authorizable.class);
        if (authorizable == null) {
            return false;
        }
        final Iterator<Group> groups = authorizable.memberOf();
        while (groups.hasNext()) {
            final String id = groups.next().getID();
            if (authorizedGroups.contains(id)) {
                return true;
            }
        }
        return false;
    }

}
