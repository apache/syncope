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
package org.apache.syncope.common.lib.to;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.PathParam;
import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.types.ResourceOperation;

public class RemediationTO implements EntityTO {

    private static final long serialVersionUID = 3983540425142284975L;

    private String key;

    private String anyType;

    private ResourceOperation operation;

    private AnyCR anyCRPayload;

    private AnyUR anyURPayload;

    private String keyPayload;

    private String error;

    private OffsetDateTime instant;

    private String pullTask;

    private String resource;

    private String remoteName;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getAnyType() {
        return anyType;
    }

    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public ResourceOperation getOperation() {
        return operation;
    }

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public AnyCR getAnyCRPayload() {
        return anyCRPayload;
    }

    public void setAnyCRPayload(final AnyCR anyCRPayload) {
        this.anyCRPayload = anyCRPayload;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public AnyUR getAnyURPayload() {
        return anyURPayload;
    }

    public void setAnyURPayload(final AnyUR anyURPayload) {
        this.anyURPayload = anyURPayload;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getKeyPayload() {
        return keyPayload;
    }

    public void setKeyPayload(final String keyPayload) {
        this.keyPayload = keyPayload;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public OffsetDateTime getInstant() {
        return instant;
    }

    public void setInstant(final OffsetDateTime instant) {
        this.instant = instant;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getPullTask() {
        return pullTask;
    }

    public void setPullTask(final String pullTask) {
        this.pullTask = pullTask;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(final String remoteName) {
        this.remoteName = remoteName;
    }
}
