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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Exec;

@MappedSuperclass
public abstract class AbstractExec extends AbstractGeneratedKeyEntity implements Exec {

    private static final long serialVersionUID = -812344822970166317L;

    @NotNull
    protected String status;

    @NotNull
    protected String executor;

    /**
     * Any information to be accompanied to this execution's result.
     */
    @Lob
    protected String message;

    /**
     * Start instant of this execution.
     */
    @NotNull
    @Column(name = "startDate", nullable = false)
    protected OffsetDateTime start;

    /**
     * End instant of this execution.
     */
    @Column(name = "endDate")
    protected OffsetDateTime end;

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public String getExecutor() {
        return this.executor;
    }

    @Override
    public void setExecutor(final String executor) {
        this.executor = executor;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = Optional.ofNullable(message).map(s -> s.replace('\0', '\n')).orElse(null);
    }

    @Override
    public OffsetDateTime getStart() {
        return start;
    }

    @Override
    public void setStart(final OffsetDateTime start) {
        this.start = start;
    }

    @Override
    public OffsetDateTime getEnd() {
        return end;
    }

    @Override
    public void setEnd(final OffsetDateTime end) {
        this.end = end;
    }
}
