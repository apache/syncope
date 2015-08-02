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
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplate;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;

@Entity
@DiscriminatorValue("SyncTask")
public class JPASyncTask extends AbstractProvisioningTask implements SyncTask {

    private static final long serialVersionUID = -4141057723006682563L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm destinationRealm;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "SyncTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "syncTask_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "syncTask")
    private List<JPAAnyTemplate> templates = new ArrayList<>();

    @Basic
    @Min(0)
    @Max(1)
    private Integer fullReconciliation;

    /**
     * Default constructor.
     */
    public JPASyncTask() {
        super(TaskType.SYNCHRONIZATION, null);
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
    public boolean isFullReconciliation() {
        return isBooleanAsInteger(fullReconciliation);
    }

    @Override
    public void setFullReconciliation(final boolean fullReconciliation) {
        this.fullReconciliation = getBooleanAsInteger(fullReconciliation);
    }

    @Override
    public boolean add(final AnyTemplate template) {
        checkType(template, JPAAnyTemplate.class);
        return this.templates.add((JPAAnyTemplate) template);
    }

    @Override
    public boolean remove(final AnyTemplate template) {
        checkType(template, JPAAnyTemplate.class);
        return this.templates.remove((JPAAnyTemplate) template);
    }

    @Override
    public AnyTemplate getTemplate(final AnyType anyType) {
        return CollectionUtils.find(templates, new Predicate<AnyTemplate>() {

            @Override
            public boolean evaluate(final AnyTemplate template) {
                return anyType != null && anyType.equals(template.getAnyType());
            }
        });
    }

    @Override
    public List<? extends AnyTemplate> getTemplates() {
        return templates;
    }

}
