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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerAttr;

@Entity
@Table(name = JPAMDerAttr.TABLE)
public class JPAMDerAttr extends AbstractDerAttr implements MDerAttr {

    private static final long serialVersionUID = -443509121923448129L;

    public static final String TABLE = "MDerAttr";

    @ManyToOne
    private JPAMembership owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPAMDerAttrTemplate template;

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
    public MDerAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final MDerAttrTemplate template) {
        checkType(template, JPAMDerAttrTemplate.class);
        this.template = (JPAMDerAttrTemplate) template;
    }

    @Override
    public MDerSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final DerSchema schema) {
        LOG.warn("This is membership attribute, set template to select schema");
    }

}
