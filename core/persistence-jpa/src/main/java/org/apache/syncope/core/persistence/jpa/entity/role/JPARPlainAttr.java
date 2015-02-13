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
package org.apache.syncope.core.persistence.jpa.entity.role;

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
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.role.RPlainSchema;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPARPlainAttr.TABLE)
public class JPARPlainAttr extends AbstractPlainAttr implements RPlainAttr {

    private static final long serialVersionUID = 2848159565890995780L;

    public static final String TABLE = "RPlainAttr";

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPARole owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPARPlainAttrTemplate template;

    @OneToMany(cascade = CascadeType.MERGE, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPARPlainAttrValue> values;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "attribute")
    @Valid
    private JPARPlainAttrUniqueValue uniqueValue;

    public JPARPlainAttr() {
        super();
        values = new ArrayList<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public Role getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Attributable<?, ?, ?> owner) {
        checkType(owner, JPARole.class);
        this.owner = (JPARole) owner;
    }

    @Override
    public RPlainAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final RPlainAttrTemplate template) {
        checkType(template, JPARPlainAttrTemplate.class);
        this.template = (JPARPlainAttrTemplate) template;
    }

    @Override
    public RPlainSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        LOG.warn("This is role attribute, set template to select schema");
    }

    @Override
    protected boolean addValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPARPlainAttrValue.class);
        return values.add((JPARPlainAttrValue) attrValue);
    }

    @Override
    public boolean removeValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPARPlainAttrValue.class);
        return values.remove((JPARPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends RPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public RPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(owner, JPARPlainAttrUniqueValue.class);
        this.uniqueValue = (JPARPlainAttrUniqueValue) uniqueValue;
    }
}
