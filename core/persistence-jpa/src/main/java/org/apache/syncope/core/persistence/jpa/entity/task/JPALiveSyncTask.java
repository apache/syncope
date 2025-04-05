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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplateLiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPALiveSyncTask.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "resource_id" }))
public class JPALiveSyncTask extends AbstractInboundTask<LiveSyncTask> implements LiveSyncTask {

    private static final long serialVersionUID = 7741318722366524409L;

    public static final String TABLE = "LiveSyncTask";

    @Min(1)
    @NotNull
    private Integer delaySecondsAcrossInvocations = 5;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAImplementation liveSyncDeltaMapper;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "LiveSyncTaskAction",
            joinColumns =
            @JoinColumn(name = "task_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "task_id", "implementation_id" }))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "liveSyncTask")
    private List<JPAAnyTemplateLiveSyncTask> templates = new ArrayList<>();

    @OneToMany(targetEntity = JPALiveSyncTaskExec.class,
            cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    private List<TaskExec<SchedTask>> executions = new ArrayList<>();

    @Override
    public int getDelaySecondsAcrossInvocations() {
        return Optional.ofNullable(delaySecondsAcrossInvocations).orElse(5);
    }

    @Override
    public void setDelaySecondsAcrossInvocations(final int delaySecondsAcrossInvocations) {
        this.delaySecondsAcrossInvocations = delaySecondsAcrossInvocations;
    }

    @Override
    public Implementation getLiveSyncDeltaMapper() {
        return liveSyncDeltaMapper;
    }

    @Override
    public void setLiveSyncDeltaMapper(final Implementation liveSyncDeltaMapper) {
        checkType(liveSyncDeltaMapper, JPAImplementation.class);
        checkImplementationType(liveSyncDeltaMapper, IdMImplementationType.LIVE_SYNC_DELTA_MAPPER);
        this.liveSyncDeltaMapper = (JPAImplementation) liveSyncDeltaMapper;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, IdMImplementationType.INBOUND_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    @Override
    public boolean add(final AnyTemplateLiveSyncTask template) {
        checkType(template, JPAAnyTemplateLiveSyncTask.class);
        return this.templates.add((JPAAnyTemplateLiveSyncTask) template);
    }

    @Override
    public Optional<? extends AnyTemplateLiveSyncTask> getTemplate(final String anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType().getKey())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplateLiveSyncTask> getTemplates() {
        return templates;
    }

    @Override
    protected Class<? extends TaskExec<SchedTask>> executionClass() {
        return JPALiveSyncTaskExec.class;
    }

    @Override
    protected List<TaskExec<SchedTask>> executions() {
        return executions;
    }
}
