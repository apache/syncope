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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BulkConfigurationOption extends ConfigurationOption {

    private static final long serialVersionUID = 8218541842239260269L;

    private final int maxOperations;

    private final int maxPayloadSize;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public BulkConfigurationOption(
            @JsonProperty("supported") final boolean supported,
            @JsonProperty("maxOperations") final int maxOperations,
            @JsonProperty("maxPayloadSize") final int maxPayloadSize) {

        super(supported);
        this.maxOperations = maxOperations;
        this.maxPayloadSize = maxPayloadSize;
    }

    public int getMaxOperations() {
        return maxOperations;
    }

    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

}
