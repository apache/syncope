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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.apache.syncope.core.persistence.api.entity.Realm;

public class JSONRealmListener extends JSONEntityListener<Realm> {

    @PostLoad
    public void read(final JPARealm realm) {
        super.json2list(realm, false);
    }

    @PrePersist
    @PreUpdate
    public void save(final JPARealm realm) {
        realm.list2json();
    }

    @PostPersist
    @PostUpdate
    public void readAfterSave(final JPARealm realm) {
        super.json2list(realm, true);
    }
}
