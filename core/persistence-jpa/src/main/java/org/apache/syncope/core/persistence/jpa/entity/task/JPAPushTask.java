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
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;

@Entity
@DiscriminatorValue("PushTask")
public class JPAPushTask extends AbstractProvisioningTask implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm sourceRealm;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "PushTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "pushTask_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

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
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
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
