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

import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

@MappedSuperclass
public abstract class AbstractAny<P extends PlainAttr<?>, D extends DerAttr<?>, V extends VirAttr<?>>
        extends AbstractAnnotatedEntity<Long>
        implements Any<P, D, V> {

    private static final long serialVersionUID = -2666540708092702810L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
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
    public P getPlainAttr(final String plainSchemaName) {
        return CollectionUtils.find(getPlainAttrs(), new Predicate<P>() {

            @Override
            public boolean evaluate(final P plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainSchemaName.equals(plainAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public D getDerAttr(final String derSchemaName) {
        return CollectionUtils.find(getDerAttrs(), new Predicate<D>() {

            @Override
            public boolean evaluate(final D derAttr) {
                return derAttr != null && derAttr.getSchema() != null
                        && derSchemaName.equals(derAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public V getVirAttr(final String virSchemaName) {
        return CollectionUtils.find(getVirAttrs(), new Predicate<V>() {

            @Override
            public boolean evaluate(final V virAttr) {
                return virAttr != null && virAttr.getSchema() != null
                        && virSchemaName.equals(virAttr.getSchema().getKey());
            }
        });
    }

    protected abstract List<JPAExternalResource> internalGetResources();

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return internalGetResources().add((JPAExternalResource) resource);
    }

    @Override
    public boolean remove(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return internalGetResources().remove((JPAExternalResource) resource);
    }

    @Override
    public List<String> getResourceNames() {
        return CollectionUtils.collect(getResources(), new Transformer<ExternalResource, String>() {

            @Override
            public String transform(final ExternalResource input) {
                return input.getKey();
            }
        }, new ArrayList<String>());
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return internalGetResources();
    }
}
