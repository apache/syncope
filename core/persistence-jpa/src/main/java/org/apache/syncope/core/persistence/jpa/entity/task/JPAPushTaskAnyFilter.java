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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAPushTaskAnyFilter.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "pushTask_id", "anyType_id" }))
public class JPAPushTaskAnyFilter extends AbstractGeneratedKeyEntity implements PushTaskAnyFilter {

    private static final long serialVersionUID = 3517381731849788407L;

    public static final String TABLE = "PushTaskAnyFilter";

    @ManyToOne
    private JPAPushTask pushTask;

    @ManyToOne
    private JPAAnyType anyType;

    @NotNull
    private String fiql;

    @Override
    public PushTask getPushTask() {
        return pushTask;
    }

    @Override
    public void setPushTask(final PushTask pushTask) {
        checkType(pushTask, JPAPushTask.class);
        this.pushTask = (JPAPushTask) pushTask;
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
    public String getFIQLCond() {
        return fiql;
    }

    @Override
    public void setFIQLCond(final String fiql) {
        this.fiql = fiql;
    }

}
