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
package org.apache.syncope.core.persistence.jpa.entity.membership;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAttributable;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPAMembership.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "user_id", "group_id" }))
public class JPAMembership extends AbstractAttributable<MPlainAttr, MDerAttr, MVirAttr> implements Membership {

    private static final long serialVersionUID = 5030106264797289469L;

    public static final String TABLE = "Membership";

    @Id
    private Long id;

    @ManyToOne
    private JPAUser user;

    @ManyToOne
    private JPAGroup group;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMPlainAttr> plainAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAMVirAttr> virAttrs;

    public JPAMembership() {
        super();

        plainAttrs = new ArrayList<>();
        derAttrs = new ArrayList<>();
        virAttrs = new ArrayList<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(final Group group) {
        checkType(group, JPAGroup.class);
        this.group = (JPAGroup) group;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        checkType(user, JPAUser.class);
        this.user = (JPAUser) user;
    }

    @Override
    public boolean addPlainAttr(final MPlainAttr plainAttr) {
        checkType(plainAttr, JPAMPlainAttr.class);

        if (getGroup() != null && plainAttr.getSchema() != null) {
            MPlainAttrTemplate found = CollectionUtils.find(getGroup().getAttrTemplates(MPlainAttrTemplate.class),
                    new Predicate<MPlainAttrTemplate>() {

                        @Override
                        public boolean evaluate(final MPlainAttrTemplate template) {
                            return plainAttr.getSchema().equals(template.getSchema());
                        }

                    });
            if (found != null) {
                plainAttr.setTemplate(found);
                return plainAttrs.add((JPAMPlainAttr) plainAttr);
            }
        }

        LOG.warn("Attribute not added because either group was not yet set, "
                + "schema was not specified or no template for that schema is available");
        return false;
    }

    @Override
    public boolean removePlainAttr(final MPlainAttr attr) {
        checkType(attr, JPAMPlainAttr.class);
        return plainAttrs.remove((JPAMPlainAttr) attr);
    }

    @Override
    public List<? extends MPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean addDerAttr(final MDerAttr derAttr) {
        checkType(derAttr, JPAMDerAttr.class);

        if (getGroup() != null && derAttr.getSchema() != null) {
            MDerAttrTemplate found = CollectionUtils.find(getGroup().getAttrTemplates(MDerAttrTemplate.class),
                    new Predicate<MDerAttrTemplate>() {

                        @Override
                        public boolean evaluate(final MDerAttrTemplate template) {
                            return derAttr.getSchema().equals(template.getSchema());
                        }

                    });
            if (found != null) {
                derAttr.setTemplate(found);
                return derAttrs.add((JPAMDerAttr) derAttr);
            }
        }

        LOG.warn("Attribute not added because either group was not yet set, "
                + "schema was not specified or no template for that schema is available");
        return false;
    }

    @Override
    public boolean removeDerAttr(final MDerAttr derAttr) {
        checkType(derAttr, JPAMDerAttr.class);
        return derAttrs.remove((JPAMDerAttr) derAttr);
    }

    @Override
    public List<? extends MDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean addVirAttr(final MVirAttr virAttr) {
        checkType(virAttr, JPAMVirAttr.class);

        if (getGroup() != null && virAttr.getSchema() != null) {
            MVirAttrTemplate found = null;
            for (MVirAttrTemplate template : getGroup().getAttrTemplates(MVirAttrTemplate.class)) {
                if (virAttr.getSchema().equals(template.getSchema())) {
                    found = template;
                }
            }
            if (found != null) {
                virAttr.setTemplate(found);
                return virAttrs.add((JPAMVirAttr) virAttr);
            }
        }

        LOG.warn("Attribute not added because either "
                + "schema was not specified or no template for that schema is available");
        return false;
    }

    @Override
    public boolean removeVirAttr(final MVirAttr virAttr) {
        checkType(virAttr, JPAMVirAttr.class);
        return virAttrs.remove((JPAMVirAttr) virAttr);
    }

    @Override
    public List<? extends MVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @Override
    public String toString() {
        return "Membership[" + "id=" + id + ", " + user + ", " + group + ']';
    }
}
