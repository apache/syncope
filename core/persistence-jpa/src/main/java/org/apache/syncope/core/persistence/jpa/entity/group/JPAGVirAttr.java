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
package org.apache.syncope.core.persistence.jpa.entity.group;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.AbstractVirAttr;

@Entity
@Table(name = JPAGVirAttr.TABLE)
public class JPAGVirAttr extends AbstractVirAttr implements GVirAttr {

    private static final long serialVersionUID = -1747430556914428649L;

    public static final String TABLE = "GVirAttr";

    @ManyToOne
    private JPAGroup owner;

    @Column(nullable = false)
    @OneToOne(cascade = CascadeType.MERGE)
    private JPAGVirAttrTemplate template;

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Attributable<?, ?, ?> owner) {
        checkType(owner, JPAGroup.class);
        this.owner = (JPAGroup) owner;
    }

    @Override
    public GVirAttrTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(final GVirAttrTemplate template) {
        checkType(template, JPAGVirAttrTemplate.class);
        this.template = (JPAGVirAttrTemplate) template;
    }

    @Override
    public GVirSchema getSchema() {
        return template == null ? null : template.getSchema();
    }

    @Override
    public void setSchema(final VirSchema schema) {
        LOG.warn("This is group attribute, set template to select schema");
    }

}
