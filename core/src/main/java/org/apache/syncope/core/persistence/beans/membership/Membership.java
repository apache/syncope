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
    private List<MAttr> attrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<MVirAttr> virAttrs;

    public Membership() {
        super();

        attrs = new ArrayList<MAttr>();
        derAttrs = new ArrayList<MDerAttr>();
        virAttrs = new ArrayList<MVirAttr>();
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
    public <T extends AbstractAttr> boolean addAttr(final T attr) {
        if (!(attr instanceof MAttr)) {
            throw new ClassCastException("attribute is expected to be typed MAttr: " + attr.getClass().getName());
        }
        return attrs.add((MAttr) attr);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttr(final T attr) {
        if (!(attr instanceof MAttr)) {
            throw new ClassCastException("attribute is expected to be typed MAttr: " + attr.getClass().getName());
        }
        return attrs.remove((MAttr) attr);
    }

    @Override
    public List<? extends AbstractAttr> getAttrs() {
        return attrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttrs(final List<? extends AbstractAttr> attrs) {
        this.attrs.clear();
        if (attrs != null && !attrs.isEmpty()) {
            for (AbstractAttr attr : attrs) {
                addAttr(attr);
            }
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerAttr(final T derAttr) {
        if (!(derAttr instanceof MDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed MDerAttr: " + derAttr.getClass().getName());
        }

        if (getSyncopeRole() != null && derAttr.getSchema() != null) {
            MDerAttrTemplate found = null;
            for (MDerAttrTemplate template : getSyncopeRole().findInheritedTemplates(MDerAttrTemplate.class)) {
                if (derAttr.getSchema().equals(template.getSchema())) {
                    found = template;
                }
            }
            if (found != null) {
                ((MDerAttr) derAttr).setTemplate(found);
                return derAttrs.add((MDerAttr) derAttr);
            }
        }

        LOG.warn("Attribute not added because either role was not yet set, "
                + "schema was not specified or no template for that schema is available");
        return false;
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerAttr(final T derAttr) {
        if (!(derAttr instanceof MDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed MDerAttr: " + derAttr.getClass().getName());
        }

        return derAttrs.remove((MDerAttr) derAttr);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDerAttrs(final List<? extends AbstractDerAttr> derAttrs) {
        this.derAttrs.clear();
        if (derAttrs != null && !derAttrs.isEmpty()) {
            for (AbstractDerAttr attr : derAttrs) {
                addDerAttr(attr);
            }
        }
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirAttr(final T virAttr) {
        if (!(virAttr instanceof MVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed MVirAttr: " + virAttr.getClass().getName());
        }

        if (getSyncopeRole() != null && virAttr.getSchema() != null) {
            MVirAttrTemplate found = null;
            for (MVirAttrTemplate template : getSyncopeRole().findInheritedTemplates(MVirAttrTemplate.class)) {
                if (virAttr.getSchema().equals(template.getSchema())) {
                    found = template;
                }
            }
            if (found != null) {
                ((MVirAttr) virAttr).setTemplate(found);
                return virAttrs.add((MVirAttr) virAttr);
            }
        }

        LOG.warn("Attribute not added because either "
                + "schema was not specified or no template for that schema is available");
        return false;
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirAttr(final T virAttr) {
        if (!(virAttr instanceof MVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed MVirAttr: " + virAttr.getClass().getName());
        }

        return virAttrs.remove((MVirAttr) virAttr);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setVirAttrs(final List<? extends AbstractVirAttr> virAttrs) {
        this.virAttrs.clear();
        if (virAttrs != null && !virAttrs.isEmpty()) {
            for (AbstractVirAttr attr : virAttrs) {
                addVirAttr(attr);
            }
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
        // Memberships cannot be directly associated to resources.
    }

    @Override
    public String toString() {
        return "Membership[" + "id=" + id + ", " + syncopeUser + ", " + syncopeRole + ']';
    }
}
