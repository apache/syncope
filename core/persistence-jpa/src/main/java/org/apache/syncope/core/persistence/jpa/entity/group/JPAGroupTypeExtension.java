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
package org.apache.syncope.core.persistence.jpa.entity.group;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.AbstractTypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;

@Entity
@Table(name = JPAGroupTypeExtension.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "group_id", "anyType_id" }))
public class JPAGroupTypeExtension extends AbstractTypeExtension implements GroupTypeExtension {

    private static final long serialVersionUID = -8367626793791263551L;

    public static final String TABLE = "GroupTypeExtension";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "GroupTypeExtension_Class",
            joinColumns =
            @JoinColumn(name = "typeExtension_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "typeExtension_id", "anyTypeClass_id" }))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @ManyToOne
    private JPAGroup group;

    @Override
    protected List<JPAAnyTypeClass> auxClasses() {
        return auxClasses;
    }

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(final Group group) {
        checkType(group, JPAGroup.class);
        this.group = (JPAGroup) group;
    }
}
