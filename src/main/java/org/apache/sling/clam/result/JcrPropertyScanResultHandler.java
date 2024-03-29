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
package org.apache.sling.clam.result;

import org.apache.sling.commons.clam.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Service to handle JCR property scan results.
 */
@ConsumerType
public interface JcrPropertyScanResultHandler {

    /**
     * Handles scan results of single-value properties.
     *
     * @param scanResult   The scan result from Clam service
     * @param path         The path of the scanned single-value property
     * @param propertyType The type of the scanned property
     * @param userId       The id of the user who added or changed the property
     */
    public abstract void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int propertyType, @Nullable final String userId);

    /**
     * Handles scan results of multi-value properties.
     *
     * @param scanResult   The scan result from Clam service
     * @param path         The path of the scanned multi-value property
     * @param index        The index of the scanned property value
     * @param propertyType The type of the scanned property
     * @param userId       The id of the user who added or changed the property
     */
    public abstract void handleJcrPropertyScanResult(@NotNull final ScanResult scanResult, @NotNull final String path, final int index, final int propertyType, @Nullable final String userId);

}
