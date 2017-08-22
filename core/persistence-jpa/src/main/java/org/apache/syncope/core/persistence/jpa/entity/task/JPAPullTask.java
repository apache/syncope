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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;

@Entity
@DiscriminatorValue("PullTask")
public class JPAPullTask extends AbstractProvisioningTask implements PullTask {

    private static final long serialVersionUID = -4141057723006682563L;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PullMode pullMode;

    private String reconciliationFilterBuilderClassName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm destinationRealm;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "PullTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "pullTask_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pullTask")
    private List<JPAAnyTemplatePullTask> templates = new ArrayList<>();

    @Override
    public PullMode getPullMode() {
        return pullMode;
    }

    @Override
    public void setPullMode(final PullMode pullMode) {
        this.pullMode = pullMode;
    }

    @Override
    public String getReconciliationFilterBuilderClassName() {
        return reconciliationFilterBuilderClassName;
    }

    @Override
    public void setReconciliationFilterBuilderClassName(final String reconciliationFilterBuilderClassName) {
        this.reconciliationFilterBuilderClassName = reconciliationFilterBuilderClassName;
    }

    @Override
    public Realm getDestinatioRealm() {
        return destinationRealm;
    }

    @Override
    public void setDestinationRealm(final Realm destinationRealm) {
        checkType(destinationRealm, JPARealm.class);
        this.destinationRealm = (JPARealm) destinationRealm;
    }

    @Override
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
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

}
