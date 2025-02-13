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

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.jpa.entity.JSONEntityListener;

public class JSONAnyObjectListener extends JSONEntityListener<AnyObject> {

    @PostLoad
    public void read(final JPAAnyObject anyObject) {
        super.json2list(anyObject, false);
    }

    @PrePersist
    @PreUpdate
    public void save(final JPAAnyObject anyObject) {
        anyObject.list2json();
    }

    @PostPersist
    @PostUpdate
    public void readAfterSave(final JPAAnyObject anyObject) {
        super.json2list(anyObject, true);
    }
}
