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
package org.apache.sling.clam.it.support;

import java.util.HashMap;

import org.apache.sling.clam.result.JcrPropertyScanResultHandler;
import org.apache.sling.commons.clam.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecordingJcrPropertyScanResultHandler implements JcrPropertyScanResultHandler {

    private final HashMap<String, ScanResult> results = new HashMap<>();

    public RecordingJcrPropertyScanResultHandler() {
    }

    public HashMap<String, ScanResult> getResults() {
        return results;
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull ScanResult scanResult, @NotNull String path, int propertyType, @Nullable String userId) {
        results.put(path, scanResult);
    }

    @Override
    public void handleJcrPropertyScanResult(@NotNull ScanResult scanResult, @NotNull String path, int index, int propertyType, @Nullable String userId) {
        final String key = String.format("%s[%s]", path, index);
        results.put(key, scanResult);
    }

}
