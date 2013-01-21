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
package org.apache.syncope.core.persistence.beans.membership;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;

import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {"syncopeUser_id", "syncopeRole_id"}))
public class Membership extends AbstractAttributable {

    private static final long serialVersionUID = 5030106264797289469L;

    @Id
    private Long id;

    @ManyToOne
    private SyncopeUser syncopeUser;

    @ManyToOne
    private SyncopeRole syncopeRole;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MAttr> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MDerAttr> derivedAttributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MVirAttr> virtualAttributes;

    public Membership() {
        super();

        attributes = new ArrayList<MAttr>();
        derivedAttributes = new ArrayList<MDerAttr>();
        virtualAttributes = new ArrayList<MVirAttr>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    protected Set<ExternalResource> internalGetResources() {
        return Collections.emptySet();
    }

    public SyncopeRole getSyncopeRole() {
        return syncopeRole;
    }

    public void setSyncopeRole(final SyncopeRole syncopeRole) {
        this.syncopeRole = syncopeRole;
    }

    public SyncopeUser getSyncopeUser() {
        return syncopeUser;
    }

    public void setSyncopeUser(final SyncopeUser syncopeUser) {
        this.syncopeUser = syncopeUser;
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(final T attribute) {
        if (!(attribute instanceof MAttr)) {
            throw new ClassCastException("attribute is expected to be typed MAttr: " + attribute.getClass().getName());
        }
        return attributes.add((MAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(final T attribute) {
        if (!(attribute instanceof MAttr)) {
            throw new ClassCastException("attribute is expected to be typed MAttr: " + attribute.getClass().getName());
        }
        return attributes.remove((MAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(final List<? extends AbstractAttr> attributes) {
        this.attributes.clear();
        if (attributes != null && !attributes.isEmpty()) {
            this.attributes.addAll((List<MAttr>) attributes);
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(final T derAttr) {
        if (!(derAttr instanceof MDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed MDerAttr: " + derAttr.getClass().getName());
        }
        return derivedAttributes.add((MDerAttr) derAttr);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(final T derAttr) {
        if (!(derAttr instanceof MDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed MDerAttr: " + derAttr.getClass().getName());
        }

        return derivedAttributes.remove((MDerAttr) derAttr);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(final List<? extends AbstractDerAttr> derivedAttributes) {
        this.derivedAttributes.clear();
        if (derivedAttributes != null && !derivedAttributes.isEmpty()) {
            this.derivedAttributes.addAll((List<MDerAttr>) derivedAttributes);
        }
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirtualAttribute(final T virAttr) {
        if (!(virAttr instanceof MVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed MVirAttr: " + virAttr.getClass().getName());
        }

        return virtualAttributes.add((MVirAttr) virAttr);
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirtualAttribute(final T virAttr) {
        if (!(virAttr instanceof MVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed MVirAttr: " + virAttr.getClass().getName());
        }

        return virtualAttributes.remove((MVirAttr) virAttr);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirtualAttributes() {
        return virtualAttributes;
    }

    @Override
    public void setVirtualAttributes(final List<? extends AbstractVirAttr> virtualAttributes) {
        this.virtualAttributes.clear();
        if (virtualAttributes != null && !virtualAttributes.isEmpty()) {
            this.virtualAttributes.addAll((List<MVirAttr>) virtualAttributes);
        }
    }

    @Override
    public boolean addResource(final ExternalResource resource) {
        return false;
    }

    @Override
    public boolean removeResource(final ExternalResource resource) {
        return false;
    }

    @Override
    public Set<ExternalResource> getResources() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getResourceNames() {
        return Collections.emptySet();
    }

    @Override
    public void setResources(final Set<ExternalResource> resources) {
    }

    @Override
    public String toString() {
        return "Membership[" + "id=" + id + ", " + syncopeUser + ", " + syncopeRole + ']';
    }
}
