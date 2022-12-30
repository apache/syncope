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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAPushTask.TABLE)
public class JPAPushTask extends AbstractProvisioningTask<PushTask> implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    public static final String TABLE = "PushTask";

    protected static final TypeReference<HashMap<String, String>> FILTER_TYPEREF =
            new TypeReference<HashMap<String, String>>() {
    };

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm sourceRealm;

    @Lob
    private String filters;

    @Transient
    private Map<String, String> filterMap = new HashMap<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "PushTaskAction",
            joinColumns =
            @JoinColumn(name = "task_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "task_id", "implementation_id" }))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToMany(targetEntity = JPAPushTaskExec.class,
            cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    private List<TaskExec<SchedTask>> executions = new ArrayList<>();

    @Override
    public JPARealm getSourceRealm() {
        return sourceRealm;
    }

    @Override
    public void setSourceRealm(final Realm sourceRealm) {
        checkType(sourceRealm, JPARealm.class);
        this.sourceRealm = (JPARealm) sourceRealm;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, IdMImplementationType.PUSH_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    @Override
    public Optional<String> getFilter(final String anyType) {
        return Optional.ofNullable(filterMap.get(anyType));
    }

    @Override
    public Map<String, String> getFilters() {
        return filterMap;
    }

    @Override
    protected Class<? extends TaskExec<SchedTask>> executionClass() {
        return JPAPushTaskExec.class;
    }

    @Override
    protected List<TaskExec<SchedTask>> executions() {
        return executions;
    }

    protected void json2map(final boolean clearFirst) {
        if (clearFirst) {
            getFilters().clear();
        }
        if (filters != null) {
            getFilters().putAll(POJOHelper.deserialize(filters, FILTER_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2map(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2map(true);
    }

    @PrePersist
    @PreUpdate
    public void map2json() {
        filters = POJOHelper.serialize(getFilters());
    }
}
