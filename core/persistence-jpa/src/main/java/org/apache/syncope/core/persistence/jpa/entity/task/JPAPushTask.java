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
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.task.AnyFilter;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.job.PushJob;

@Entity
@DiscriminatorValue("PushTask")
public class JPAPushTask extends AbstractProvisioningTask implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "PushTask_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "pushTask_id", referencedColumnName = "id"))
    private List<String> actionsClassNames = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pushTask")
    private List<JPAAnyFilter> filters = new ArrayList<>();

    /**
     * Default constructor.
     */
    public JPAPushTask() {
        super(TaskType.PUSH, PushJob.class.getName());
    }

    @Override
    public List<String> getActionsClassNames() {
        return actionsClassNames;
    }

    @Override
    public boolean add(final AnyFilter filter) {
        checkType(filter, JPAAnyFilter.class);
        return this.filters.add((JPAAnyFilter) filter);
    }

    @Override
    public boolean remove(final AnyFilter filter) {
        checkType(filter, JPAAnyFilter.class);
        return this.filters.remove((JPAAnyFilter) filter);
    }

    @Override
    public AnyFilter getFilter(final AnyType anyType) {
        return CollectionUtils.find(filters, new Predicate<AnyFilter>() {

            @Override
            public boolean evaluate(final AnyFilter filter) {
                return anyType != null && anyType.equals(filter.getAnyType());
            }
        });
    }

    @Override
    public List<? extends AnyFilter> getFilters() {
        return filters;
    }
}
