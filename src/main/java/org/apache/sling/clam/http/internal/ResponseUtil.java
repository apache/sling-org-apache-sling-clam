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
import java.nio.charset.StandardCharsets;

import javax.jcr.PropertyType;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.clam.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Util for HTTP Responses.
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    static void handleError(@NotNull final SlingHttpServletResponse response, final int status, @Nullable final String message) throws ServletException, IOException {
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.setStatus(status);
            if (message != null) {
                final JsonObjectBuilder error = Json.createObjectBuilder();
                error.add("message", message);
                try (JsonGenerator generator = Json.createGenerator(response.getWriter())) {
                    generator.write(error.build()).flush();
                }
            }
        } catch (final JsonException e) {
            throw new ServletException("Building response failed.");
        }
    }

    static String json(@NotNull final ScanResult scanResult, @NotNull final String path, @Nullable final Integer index, final int propertyType, @Nullable final String userId) {
        final JsonObjectBuilder event = Json.createObjectBuilder();
        event.add("timestamp", scanResult.getTimestamp());
        event.add("status", scanResult.getStatus().name());
        event.add("message", scanResult.getMessage());
        event.add("started", scanResult.getStarted());
        event.add("size", scanResult.getSize());
        event.add("path", path);
        if (index != null) {
            event.add("index", index);
        }
        event.add("propertyType", PropertyType.nameFromValue(propertyType));
        if (userId != null) {
            event.add("userId", userId);
        }
        return event.build().toString();
    }

}
