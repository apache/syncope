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
package org.apache.syncope.server.persistence.jpa.entity.role;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.server.persistence.api.entity.Attributable;
import org.apache.syncope.server.persistence.api.entity.VirSchema;
import org.apache.syncope.server.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.server.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RVirSchema;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.jpa.entity.AbstractVirAttr;

@Entity
@Table(name = JPARVirAttr.TABLE)
public class JPARVirAttr extends AbstractVirAttr implements RVirAttr {

    private static final long serialVersionUID = -1747430556914428649L;

    public static final String TABLE = "RVirAttr";

    @ManyToOne
    private JPARole owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPARVirAttrTemplate template;

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
    public RVirAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final RVirAttrTemplate template) {
        checkType(template, JPARVirAttrTemplate.class);
        this.template = (JPARVirAttrTemplate) template;
    }

    @Override
    public RVirSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final VirSchema schema) {
        LOG.warn("This is role attribute, set template to select schema");
    }

}
