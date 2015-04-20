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

import java.util.HashSet;
import java.util.Set;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;

@MappedSuperclass
public abstract class AbstractSubject<P extends PlainAttr, D extends DerAttr, V extends VirAttr>
        extends AbstractAttributable<P, D, V> implements Subject<P, D, V> {

    private static final long serialVersionUID = -6876467491398928855L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    protected JPARealm realm;

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
    }

    protected abstract Set<? extends ExternalResource> internalGetResources();

    @Override
    @SuppressWarnings("unchecked")
    public boolean addResource(final ExternalResource resource) {
        return ((Set<ExternalResource>) internalGetResources()).add(resource);
    }

    @Override
    public boolean removeResource(final ExternalResource resource) {
        return internalGetResources().remove(resource);
    }

    @Override
    public Set<? extends ExternalResource> getResources() {
        return internalGetResources();
    }

    @Override
    public Set<String> getResourceNames() {
        return CollectionUtils.collect(getResources(), new Transformer<ExternalResource, String>() {

            @Override
            public String transform(final ExternalResource input) {
                return input.getKey();
            }
        }, new HashSet<String>());
    }

}
