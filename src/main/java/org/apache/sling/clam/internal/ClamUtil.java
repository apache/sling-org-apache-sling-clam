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
package org.apache.sling.clam.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.sling.commons.clam.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClamUtil {

    public static final String PROPERTY_PATH = "path";

    public static final String VALUE_INDEX = "index";

    public static final String USER_ID = "userId";

    private static final String SCAN_JOB_TOPIC_ROOT = "org/apache/sling/clam/scan/jcr/property";

    private static final String RESULT_EVENT_TOPIC_ROOT = "org/apache/sling/clam/result/jcr/property";

    private ClamUtil() {
    }

    public static String scanJobTopic(final int propertyType) {
        return String.format("%s/%s", SCAN_JOB_TOPIC_ROOT, PropertyType.nameFromValue(propertyType));
    }

    public static String resultEventTopic(final int propertyType) {
        return String.format("%s/%s", RESULT_EVENT_TOPIC_ROOT, PropertyType.nameFromValue(propertyType));
    }

    public static Map<String, Object> properties(@NotNull final String path, @Nullable final String userId) {
        return properties(path, null, userId);
    }

    public static Map<String, Object> properties(@NotNull final String path, @Nullable final Integer index, @Nullable final String userId) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_PATH, path);
        if (index != null) {
            properties.put(VALUE_INDEX, index);
        }
        if (userId != null) {
            properties.put(USER_ID, userId);
        }
        return properties;
    }

    public static Map<String, Object> properties(@NotNull final String path, @Nullable final String userId, @NotNull final ScanResult scanResult) {
        return properties(path, null, userId, scanResult);
    }

    public static Map<String, Object> properties(@NotNull final String path, @Nullable final Integer index, @Nullable final String userId, @NotNull final ScanResult scanResult) {
        final Map<String, Object> properties = properties(path, index, userId);
        properties.put("timestamp", scanResult.getTimestamp());
        properties.put("message", scanResult.getMessage());
        properties.put("status", scanResult.getStatus().name());
        properties.put("started", scanResult.getStarted());
        properties.put("size", scanResult.getSize());
        return properties;
    }

    public static boolean checkLength(final long length, final long maxLength) {
        if (maxLength == -1) {
            return true;
        }
        return length <= maxLength;
    }

    public static Set<Integer> propertyTypesFromNames(@NotNull final String[] names) throws Exception {
        final Set<Integer> propertyTypes = new HashSet<>();
        for (final String name : names) {
            final int propertyType = PropertyType.valueFromName(name);
            propertyTypes.add(propertyType);
        }
        return propertyTypes;
    }

}
