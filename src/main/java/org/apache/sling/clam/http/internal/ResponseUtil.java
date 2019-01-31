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

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResponseUtil {

    private ResponseUtil() {
    }

    static void handleError(@NotNull final SlingHttpServletResponse response, final int status, @Nullable final String message) throws ServletException, IOException {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.setStatus(status);
            if (message != null) {
                final JsonObjectBuilder error = Json.createObjectBuilder();
                error.add("message", message);
                Json.createGenerator(response.getWriter()).write(error.build()).flush();
            }
        } catch (final JsonException e) {
            throw new ServletException("Building response failed.");
        }
    }

}
