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

import java.util.Date;
import javax.persistence.Column;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.Exec;

@MappedSuperclass
public abstract class AbstractExec extends AbstractGeneratedKeyEntity implements Exec {

    private static final long serialVersionUID = -812344822970166317L;

    @NotNull
    protected String status;

    /**
     * Any information to be accompanied to this execution's result.
     */
    @Lob
    protected String message;

    /**
     * Start instant of this execution.
     */
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "startDate")
    protected Date start;

    /**
     * End instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "endDate")
    protected Date end;

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message == null ? null : message.replace('\0', '\n');
    }

    @Override
    public Date getStart() {
        return start == null
                ? null
                : new Date(start.getTime());
    }

    @Override
    public void setStart(final Date start) {
        this.start = start == null
                ? null
                : new Date(start.getTime());
    }

    @Override
    public Date getEnd() {
        return end == null
                ? null
                : new Date(end.getTime());
    }

    @Override
    public void setEnd(final Date end) {
        this.end = end == null
                ? null
                : new Date(end.getTime());
    }
}
