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
package org.apache.syncope.core.persistence.jpa.entity.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;

@Entity
@Table(name = JPAConf.TABLE)
@Cacheable
public class JPAConf extends AbstractProvidedKeyEntity implements Conf {

    private static final long serialVersionUID = 7671699609879382195L;

    public static final String TABLE = "SyncopeConf";

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    @Valid
    private List<JPACPlainAttr> plainAttrs = new ArrayList<>();

    @Override
    public boolean add(final CPlainAttr attr) {
        checkType(attr, JPACPlainAttr.class);
        return plainAttrs.add((JPACPlainAttr) attr);
    }

    @Override
    public boolean remove(final CPlainAttr attr) {
        checkType(attr, JPACPlainAttr.class);
        return plainAttrs.remove((JPACPlainAttr) attr);
    }

    @Override
    public CPlainAttr getPlainAttr(final String plainSchemaName) {
        return IterableUtils.find(plainAttrs, new Predicate<CPlainAttr>() {

            @Override
            public boolean evaluate(final CPlainAttr plainAttr) {
                return plainAttr != null && plainAttr.getSchema() != null
                        && plainSchemaName.equals(plainAttr.getSchema().getKey());
            }
        });
    }

    @Override
    public List<? extends CPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        return false;
    }

    @Override
    public List<String> getResourceKeys() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return Collections.emptyList();
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        return false;
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return Collections.emptyList();
    }

    @Override
    public String getWorkflowId() {
        return null;
    }

    @Override
    public void setWorkflowId(final String workflowId) {
        // nothing to do
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public void setStatus(final String status) {
        // nothing to do
    }

    @Override
    public Realm getRealm() {
        return null;
    }

    @Override
    public void setRealm(final Realm realm) {
        // nothing to do
    }

    @Override
    public AnyType getType() {
        return null;
    }

    @Override
    public void setType(final AnyType type) {
        // nothing to do
    }

    @Override
    public Date getCreationDate() {
        return null;
    }

    @Override
    public void setCreationDate(final Date creationDate) {
        // nothing to do
    }

    @Override
    public String getCreator() {
        return null;
    }

    @Override
    public void setCreator(final String creator) {
        // nothing to do
    }

    @Override

    public Date getLastChangeDate() {
        return null;
    }

    @Override
    public void setLastChangeDate(final Date lastChangeDate) {
        // nothing to do
    }

    @Override
    public String getLastModifier() {
        return null;
    }

    @Override
    public void setLastModifier(final String lastModifier) {
    }
}
