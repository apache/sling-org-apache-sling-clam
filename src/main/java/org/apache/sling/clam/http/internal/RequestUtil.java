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

/**
 * Util for HTTP Requests.
 */
public final class RequestUtil {

    private RequestUtil() {
    }

    static String path(@NotNull final SlingHttpServletRequest request) throws Exception {
        final String value = request.getParameter("path");
        if (value == null) {
            throw new Exception("Mandatory parameter path is missing");
        } else {
            return value;
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    static Pattern pattern(@NotNull final SlingHttpServletRequest request, @NotNull final Pattern defaultPattern) throws Exception {
        final String value = request.getParameter("pattern");
        if (value == null) {
            return defaultPattern;
        } else {
            try {
                return Pattern.compile(value);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for pattern: " + value);
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    static Set<Integer> propertyTypes(@NotNull final SlingHttpServletRequest request, @NotNull final Set<Integer> defaultPropertyTypes) throws Exception {
        final String[] values = request.getParameterValues("propertyTypes");
        if (values == null || values.length == 0) {
            return defaultPropertyTypes;
        }
        try {
            return propertyTypesFromNames(values);
        } catch (Exception e) {
            throw new Exception("Invalid parameter value for propertyTypes: " + Arrays.toString(values));
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    static long maxLength(@NotNull final SlingHttpServletRequest request, final long defaultMaxLength) throws Exception {
        final String value = request.getParameter("maxLength");
        if (value == null) {
            return defaultMaxLength;
        } else {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for maxLength: " + value);
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    static int maxDepth(@NotNull final SlingHttpServletRequest request, final int defaultMaxDepth) throws Exception {
        final String value = request.getParameter("maxDepth");
        if (value == null) {
            return defaultMaxDepth;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new Exception("Invalid parameter value for maxDepth: " + value);
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
