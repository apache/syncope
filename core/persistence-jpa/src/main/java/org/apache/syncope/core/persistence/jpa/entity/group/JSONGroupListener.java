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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.JSONEntityListener;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class JSONGroupListener extends JSONEntityListener<Group, GPlainAttr> {

    protected static final TypeReference<List<JSONGPlainAttr>> TYPEREF =
            new TypeReference<List<JSONGPlainAttr>>() {
    };

    @Override
    protected List<? extends GPlainAttr> getAttrs(final String plainAttrsJSON) {
        return POJOHelper.deserialize(plainAttrsJSON, TYPEREF);
    }

    @PostLoad
    public void read(final JPAGroup group) {
        super.json2list(group, false);
    }

    @PrePersist
    @PreUpdate
    public void save(final JPAGroup group) {
        group.list2json();
    }

    @PostPersist
    @PostUpdate
    public void readAfterSave(final JPAGroup group) {
        super.json2list(group, true);
    }
}
