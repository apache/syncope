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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.validation.entity.RemediationCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPARemediation.TABLE)
@RemediationCheck
public class JPARemediation extends AbstractGeneratedKeyEntity implements Remediation {

    private static final long serialVersionUID = -1612530286294448682L;

    public static final String TABLE = "Remediation";

    @NotNull
    @Enumerated(EnumType.STRING)
    private AnyTypeKind anyTypeKind;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ResourceOperation operation;

    @NotNull
    @Lob
    private String payload;

    @NotNull
    @Lob
    private String error;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    private Date instant;

    @ManyToOne
    private JPAPullTask pullTask;

    @NotNull
    private String remoteName;

    @Override
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Override
    public ResourceOperation getOperation() {
        return operation;
    }

    @Override
    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Override
    public <T extends AnyTO> T getPayloadAsTO(final Class<T> reference) {
        return POJOHelper.deserialize(this.payload, reference);
    }

    @Override
    public <P extends AnyPatch> P getPayloadAsPatch(final Class<P> reference) {
        return POJOHelper.deserialize(this.payload, reference);
    }

    @Override
    public String getPayloadAsKey() {
        return this.payload;
    }

    @Override
    public void setPayload(final AnyTO anyTO) {
        this.payload = POJOHelper.serialize(anyTO);
    }

    @Override
    public void setPayload(final AnyPatch anyPatch) {
        this.payload = POJOHelper.serialize(anyPatch);
    }

    @Override
    public void setPayload(final String key) {
        this.payload = key;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(final String error) {
        this.error = error;
    }

    @Override
    public Date getInstant() {
        return instant == null
                ? null
                : new Date(instant.getTime());
    }

    @Override
    public void setInstant(final Date instant) {
        this.instant = instant == null
                ? null
                : new Date(instant.getTime());
    }

    @Override
    public PullTask getPullTask() {
        return pullTask;
    }

    @Override
    public void setPullTask(final PullTask pullTask) {
        checkType(pullTask, JPAPullTask.class);
        this.pullTask = (JPAPullTask) pullTask;
    }

    @Override
    public String getRemoteName() {
        return remoteName;
    }

    @Override
    public void setRemoteName(final String remoteName) {
        this.remoteName = remoteName;
    }

}
