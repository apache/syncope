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
package org.apache.syncope.core.persistence.jpa.entity.task;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplate;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;

@Entity
@Table(name = JPAAnyTemplate.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "syncTask_id", "anyType_name" }))
public class JPAAnyTemplate extends AbstractEntity<Long> implements AnyTemplate {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String TABLE = "AnyTemplate";

    @Id
    private Long id;

    @ManyToOne
    private JPASyncTask syncTask;

    @ManyToOne
    private JPAAnyType anyType;

    @Lob
    private String template;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public SyncTask getSyncTask() {
        return syncTask;
    }

    @Override
    public void setSyncTask(final SyncTask syncTask) {
        checkType(syncTask, JPASyncTask.class);
        this.syncTask = (JPASyncTask) syncTask;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public AnyTO get() {
        return template == null
                ? anyType == null
                        ? null
                        : new JPAAnyUtilsFactory().getInstance(anyType.getKind()).newAnyTO()
                : anyType == null
                        ? null
                        : POJOHelper.deserialize(template, anyType.getKind().getToClass());
    }

    @Override
    public void set(final AnyTO template) {
        if (template == null) {
            this.template = null;
        } else {
            this.template = POJOHelper.serialize(template);
        }
    }

}
