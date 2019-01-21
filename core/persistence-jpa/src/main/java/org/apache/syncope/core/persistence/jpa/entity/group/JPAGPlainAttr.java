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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPAGPlainAttr.TABLE)
public class JPAGPlainAttr extends AbstractPlainAttr<Group> implements GPlainAttr {

    private static final long serialVersionUID = 2848159565890995780L;

    public static final String TABLE = "GPlainAttr";

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAGroup owner;

    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPAGPlainAttrValue> values = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private JPAGPlainAttrUniqueValue uniqueValue;

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Group owner) {
        checkType(owner, JPAGroup.class);
        this.owner = (JPAGroup) owner;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAGPlainAttrValue.class);
        return values.add((JPAGPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends GPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public GPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPAGPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAGPlainAttrUniqueValue) uniqueValue;
    }
}
