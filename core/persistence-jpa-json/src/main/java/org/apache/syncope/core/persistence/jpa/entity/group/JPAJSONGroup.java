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
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.JSONAttributable;
import org.apache.syncope.core.persistence.jpa.validation.entity.JPAJSONAttributableCheck;

@Entity
@Table(name = JPAGroup.TABLE)
@EntityListeners({ JPAJSONGroupListener.class })
@JPAJSONAttributableCheck
public class JPAJSONGroup extends JPAGroup implements JSONAttributable<Group>, Group {

    private static final long serialVersionUID = -8543654943709531885L;

    @Lob
    private String plainAttrs;

    @Transient
    private final List<JPAJSONGPlainAttr> plainAttrList = new ArrayList<>();

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public List<JPAJSONGPlainAttr> getPlainAttrList() {
        return plainAttrList;
    }

    @Override
    public boolean add(final JSONPlainAttr<Group> attr) {
        return add((GPlainAttr) attr);
    }

    @Override
    public boolean add(final GPlainAttr attr) {
        checkType(attr, JPAJSONGPlainAttr.class);
        return plainAttrList.add((JPAJSONGPlainAttr) attr);
    }

    @Override
    public boolean remove(final GPlainAttr attr) {
        return plainAttrList.removeIf(jsonAttr -> jsonAttr.getSchemaKey().equals(attr.getSchema().getKey()));
    }

    @Override
    public List<? extends GPlainAttr> getPlainAttrs() {
        return plainAttrList;
    }
}
