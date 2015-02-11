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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPAMPlainAttr.TABLE)
public class JPAMPlainAttr extends AbstractPlainAttr implements MPlainAttr {

    private static final long serialVersionUID = 3755864809152866489L;

    public static final String TABLE = "MPlainAttr";

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAMembership owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPAMPlainAttrTemplate template;

    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPAMPlainAttrValue> values;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "attribute")
    @Valid
    private JPAMPlainAttrUniqueValue uniqueValue;

    public JPAMPlainAttr() {
        super();
        values = new ArrayList<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public Membership getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Attributable<?, ?, ?> owner) {
        checkType(owner, JPAMembership.class);
        this.owner = (JPAMembership) owner;
    }

    @Override
    public MPlainAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final MPlainAttrTemplate template) {
        checkType(template, JPAMPlainAttrTemplate.class);
        this.template = (JPAMPlainAttrTemplate) template;
    }

    @Override
    public MPlainSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        LOG.warn("This is role attribute, set template to select schema");
    }

    @Override
    protected boolean addValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAMPlainAttrValue.class);
        return values.add((JPAMPlainAttrValue) attrValue);
    }

    @Override
    public boolean removeValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAMPlainAttrValue.class);
        return values.remove((JPAMPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends MPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public MPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(owner, JPAMPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAMPlainAttrUniqueValue) uniqueValue;
    }

}
