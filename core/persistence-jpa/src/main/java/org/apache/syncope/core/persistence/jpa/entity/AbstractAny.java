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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.validation.entity.AnyCheck;

@AnyCheck
@MappedSuperclass
public abstract class AbstractAny<P extends PlainAttr<?>> extends AbstractAnnotatedEntity implements Any<P> {

    private static final long serialVersionUID = -2666540708092702810L;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPARealm realm;

    private String workflowId;

    @Column(nullable = true)
    private String status;

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public List<String> getResourceKeys() {
        return CollectionUtils.collect(
                getResources(), EntityUtils.<ExternalResource>keyTransformer(), new ArrayList<String>());
    }
}
