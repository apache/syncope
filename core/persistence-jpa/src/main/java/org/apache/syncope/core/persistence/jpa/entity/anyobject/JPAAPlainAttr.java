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
package org.apache.syncope.core.persistence.jpa.entity.anyobject;

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
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttr;

@Entity
@Table(name = JPAAPlainAttr.TABLE)
public class JPAAPlainAttr extends AbstractPlainAttr<AnyObject> implements APlainAttr {

    private static final long serialVersionUID = 8066058729580952116L;

    public static final String TABLE = "APlainAttr";

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAnyObject owner;

    /**
     * The membership of this attribute; might be {@code NULL} if this attribute is not related to a membership.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAMembership membership;

    @OneToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private List<JPAAPlainAttrValue> values = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "attribute")
    @Valid
    private JPAAPlainAttrUniqueValue uniqueValue;

    @Override
    public AnyObject getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final AnyObject owner) {
        checkType(owner, JPAAnyObject.class);
        this.owner = (JPAAnyObject) owner;
    }

    @Override
    public AMembership getMembership() {
        return membership;
    }

    @Override
    public void setMembership(final AMembership membership) {
        checkType(membership, JPAAMembership.class);
        this.membership = (JPAAMembership) membership;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        checkType(attrValue, JPAAPlainAttrValue.class);
        return values.add((JPAAPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends APlainAttrValue> getValues() {
        return values;
    }

    @Override
    public APlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        checkType(uniqueValue, JPAAPlainAttrUniqueValue.class);
        this.uniqueValue = (JPAAPlainAttrUniqueValue) uniqueValue;
    }
}
