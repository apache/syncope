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
package org.syncope.core.persistence.beans;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@MappedSuperclass
public abstract class AbstractExec extends AbstractBaseBean {

    private static final long serialVersionUID = -812344822970166317L;

    @Column(nullable = false)
    protected String status;

    /**
     * Any information to be accompained to this execution's result.
     */
    @Lob
    protected String message;

    /**
     * Start instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date startDate;

    /**
     * End instant of this execution.
     */
    @Temporal(TemporalType.TIMESTAMP)
    protected Date endDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getEndDate() {
        return endDate == null
                ? null : new Date(endDate.getTime());
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate == null
                ? null : new Date(endDate.getTime());
    }

    public Date getStartDate() {
        return startDate == null
                ? null : new Date(startDate.getTime());
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate == null
                ? null : new Date(startDate.getTime());
    }
}
