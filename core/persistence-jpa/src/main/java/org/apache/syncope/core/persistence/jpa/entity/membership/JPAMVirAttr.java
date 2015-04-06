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
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.jpa.entity.AbstractVirAttr;

@Entity
@Table(name = JPAMVirAttr.TABLE)
public class JPAMVirAttr extends AbstractVirAttr implements MVirAttr {

    private static final long serialVersionUID = 7774760571251641332L;

    public static final String TABLE = "MVirAttr";

    @ManyToOne
    private JPAMembership owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPAMVirAttrTemplate template;

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
    public MVirAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final MVirAttrTemplate template) {
        checkType(template, JPAMVirAttrTemplate.class);
        this.template = (JPAMVirAttrTemplate) template;
    }

    @Override
    public MVirSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final VirSchema schema) {
        LOG.warn("This is membership attribute, set template to select schema");
    }
}
