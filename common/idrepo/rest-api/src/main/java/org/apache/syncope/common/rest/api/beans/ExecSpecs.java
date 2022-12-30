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
package org.apache.syncope.common.rest.api.beans;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Optional;

public class ExecSpecs implements Serializable {

    private static final long serialVersionUID = 3846547401120638351L;

    public static class Builder {

        private final ExecSpecs instance = new ExecSpecs();

        public Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public Builder startAt(final OffsetDateTime startAt) {
            instance.setStartAt(startAt);
            return this;
        }

        public Builder dryRun(final boolean dryRun) {
            instance.setDryRun(dryRun);
            return this;
        }

        public ExecSpecs build() {
            return instance;
        }
    }

    private String key;

    private OffsetDateTime startAt;

    private Boolean dryRun;

    public String getKey() {
        return key;
    }

    @NotNull
    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    @QueryParam("startAt")
    public void setStartAt(final OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public Boolean getDryRun() {
        return Optional.ofNullable(dryRun).orElse(Boolean.FALSE);
    }

    @QueryParam("dryRun")
    @DefaultValue("false")
    public void setDryRun(final Boolean dryRun) {
        this.dryRun = dryRun;
    }
}
