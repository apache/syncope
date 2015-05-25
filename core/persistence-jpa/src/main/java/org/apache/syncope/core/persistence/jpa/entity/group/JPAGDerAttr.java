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

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerAttr;

@Entity
@Table(name = JPAGDerAttr.TABLE)
public class JPAGDerAttr extends AbstractDerAttr<Group> implements GDerAttr {

    private static final long serialVersionUID = 8007080005675899946L;

    public static final String TABLE = "GDerAttr";

    @ManyToOne
    private JPAGroup owner;

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Group owner) {
        checkType(owner, JPAGroup.class);
        this.owner = (JPAGroup) owner;
    }

}
