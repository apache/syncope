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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;

@Entity
@DiscriminatorValue("PushTask")
public class JPAPushTask extends AbstractProvisioningTask implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm sourceRealm;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "PushTaskAction",
            joinColumns =
            @JoinColumn(name = "task_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "task_id", "implementation_id" }))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pushTask")
    private List<JPAPushTaskAnyFilter> filters = new ArrayList<>();

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
    public boolean add(final PushTaskAnyFilter filter) {
        checkType(filter, JPAPushTaskAnyFilter.class);
        return this.filters.add((JPAPushTaskAnyFilter) filter);
    }

    @Override
    public Optional<? extends PushTaskAnyFilter> getFilter(final AnyType anyType) {
        return filters.stream().filter(filter -> anyType != null && anyType.equals(filter.getAnyType())).findFirst();
    }

    @Override
    public List<? extends PushTaskAnyFilter> getFilters() {
        return filters;
    }
}
