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

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.to.LiveSyncProvision;
import org.apache.syncope.core.persistence.api.entity.LiveSync;
import org.apache.syncope.core.persistence.jpa.validation.entity.LiveSyncCheck;

@Entity
@Table(name = JPALiveSync.TABLE)
@Cacheable
@LiveSyncCheck
public class JPALiveSync extends AbstractProvisionable<LiveSyncProvision> implements LiveSync {

    private static final long serialVersionUID = 5366750071299713173L;
    public static final String TABLE = "LiveSync";

    @NotNull
    private String kafkaUrl;

    @Override
    public String getKafkaUrl() {
        return kafkaUrl;
    }

    @Override
    public void setKafkaUrl(final String kafkaUrl) {
        this.kafkaUrl = kafkaUrl;
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
       super.list2json();
    }

}
