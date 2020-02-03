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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@DiscriminatorValue("PullTask")
public class JPAPullTask extends AbstractProvisioningTask implements PullTask {

    private static final long serialVersionUID = -4141057723006682563L;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PullMode pullMode;

    @OneToOne
    private JPAImplementation reconFilterBuilder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm destinationRealm;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "PullTaskAction",
            joinColumns =
            @JoinColumn(name = "task_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "task_id", "implementation_id" }))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pullTask")
    private List<JPAAnyTemplatePullTask> templates = new ArrayList<>();

    @NotNull
    private Boolean remediation = false;

    @Override
    public PullMode getPullMode() {
        return pullMode;
    }

    @Override
    public void setPullMode(final PullMode pullMode) {
        this.pullMode = pullMode;
    }

    @Override
    public Implementation getReconFilterBuilder() {
        return reconFilterBuilder;
    }

    @Override
    public void setReconFilterBuilder(final Implementation reconFilterBuilder) {
        checkType(reconFilterBuilder, JPAImplementation.class);
        checkImplementationType(reconFilterBuilder, IdMImplementationType.RECON_FILTER_BUILDER);
        this.reconFilterBuilder = (JPAImplementation) reconFilterBuilder;
    }

    @Override
    public Realm getDestinationRealm() {
        return destinationRealm;
    }

    @Override
    public void setDestinationRealm(final Realm destinationRealm) {
        checkType(destinationRealm, JPARealm.class);
        this.destinationRealm = (JPARealm) destinationRealm;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, IdMImplementationType.PULL_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    @Override
    public boolean add(final AnyTemplatePullTask template) {
        checkType(template, JPAAnyTemplatePullTask.class);
        return this.templates.add((JPAAnyTemplatePullTask) template);
    }

    @Override
    public Optional<? extends AnyTemplatePullTask> getTemplate(final AnyType anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplatePullTask> getTemplates() {
        return templates;
    }

    @Override
    public void setRemediation(final boolean remediation) {
        this.remediation = remediation;
    }

    @Override
    public boolean isRemediation() {
        return remediation;
    }
}
