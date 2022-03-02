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

import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class ExecDeleteQuery implements Serializable {

    private static final long serialVersionUID = 3846547401120638351L;

    public static class Builder {

        private final ExecDeleteQuery instance = new ExecDeleteQuery();

        public Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public Builder startedBefore(final OffsetDateTime date) {
            instance.setStartedBefore(date);
            return this;
        }

        public Builder startedAfter(final OffsetDateTime date) {
            instance.setStartedAfter(date);
            return this;
        }

        public Builder endedBefore(final OffsetDateTime date) {
            instance.setEndedBefore(date);
            return this;
        }

        public Builder endedAfter(final OffsetDateTime date) {
            instance.setEndedAfter(date);
            return this;
        }

        public ExecDeleteQuery build() {
            return instance;
        }
    }

    private String key;

    private OffsetDateTime startedBefore;

    private OffsetDateTime startedAfter;

    private OffsetDateTime endedBefore;

    private OffsetDateTime endedAfter;

    public String getKey() {
        return key;
    }

    @NotNull
    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public OffsetDateTime getStartedBefore() {
        return startedBefore;
    }

    @QueryParam("startedBefore")
    public void setStartedBefore(final OffsetDateTime startedBefore) {
        this.startedBefore = startedBefore;
    }

    public OffsetDateTime getStartedAfter() {
        return startedAfter;
    }

    @QueryParam("startedAfter")
    public void setStartedAfter(final OffsetDateTime startedAfter) {
        this.startedAfter = startedAfter;
    }

    public OffsetDateTime getEndedBefore() {
        return endedBefore;
    }

    @QueryParam("endedBefore")
    public void setEndedBefore(final OffsetDateTime endedBefore) {
        this.endedBefore = endedBefore;
    }

    public OffsetDateTime getEndedAfter() {
        return endedAfter;
    }

    @QueryParam("endedAfter")
    public void setEndedAfter(final OffsetDateTime endedAfter) {
        this.endedAfter = endedAfter;
    }
}
