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

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.to.AbstractProvision;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.Provisionable;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@MappedSuperclass
public abstract class AbstractProvisionable<E extends AbstractProvision>
        extends AbstractProvidedKeyEntity
        implements Provisionable<E> {
    
    @Lob
    private String provisions;

    @Transient
    private final List<E> provisionList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel provisioningTraceLevel = TraceLevel.FAILURES;

    @Override
    public Optional<E> getProvisionByAnyType(final String anyType) {
        return getProvisions().stream().
                filter(provision -> provision.getAnyType().equals(anyType)).findFirst();
    }

    @Override
    public Optional<E> getProvisionByObjectClass(final String objectClass) {
        return getProvisions().stream().
                filter(provision -> provision.getObjectClass().equals(objectClass)).findFirst();
    }

    @Override
    public List<E> getProvisions() {
        return provisionList;
    }
    
    @Override
    public TraceLevel getProvisioningTraceLevel() {
        return provisioningTraceLevel;
    }

    @Override
    public void setProvisioningTraceLevel(final TraceLevel provisioningTraceLevel) {
        this.provisioningTraceLevel = provisioningTraceLevel;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getProvisions().clear();
        }
        if (provisions != null) {
            getProvisions().addAll(
                    POJOHelper.deserialize(provisions, new TypeReference<List<E>>() {
                    }));
        }
    }

    public void list2json() {
        provisions = POJOHelper.serialize(getProvisions());
    }

}
