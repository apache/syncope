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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.PGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.PGJPAAny;
import org.apache.syncope.core.persistence.jpa.validation.entity.PGJPAAnyCheck;

@Entity
@Table(name = JPAAnyObject.TABLE)
@EntityListeners({ PGJPAAnyObjectListener.class })
@PGJPAAnyCheck
public class PGJPAAnyObject extends JPAAnyObject implements PGJPAAny<AnyObject>, AnyObject {

    private static final long serialVersionUID = -8543654943709531885L;

    @Lob
    @Column(columnDefinition = "jsonb")
    private String plainAttrs;

    @Transient
    private final List<PGAPlainAttr> plainAttrList = new ArrayList<>();

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public List<PGAPlainAttr> getPlainAttrList() {
        return plainAttrList;
    }

    @Override
    public boolean add(final PGPlainAttr<AnyObject> attr) {
        return add((APlainAttr) attr);
    }

    @Override
    public boolean add(final APlainAttr attr) {
        checkType(attr, PGAPlainAttr.class);
        return plainAttrList.add((PGAPlainAttr) attr);
    }

    @Override
    public boolean remove(final APlainAttr attr) {
        return plainAttrList.removeIf(pgattr
                -> pgattr.getSchemaKey().equals(attr.getSchema().getKey())
                && attr.getOwner().getKey().equals(getKey())
                && attr.getMembership() == null
                ? true
                : pgattr.getMembership() != null && pgattr.getMembershipKey().equals(attr.getMembership().getKey()));
    }

    @Override
    protected List<? extends APlainAttr> internalGetPlainAttrs() {
        return plainAttrList;
    }

    @Override
    public boolean remove(final AMembership membership) {
        plainAttrList.removeIf(attr -> attr.getMembership().getKey().equals(membership.getKey()));
        return super.remove(membership);
    }
}
