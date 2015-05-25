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
package org.apache.syncope.core.persistence.jpa.entity.user;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerAttr;

@Entity
@Table(name = JPAUDerAttr.TABLE)
public class JPAUDerAttr extends AbstractDerAttr<User> implements UDerAttr {

    private static final long serialVersionUID = 4723044452807292060L;

    public static final String TABLE = "UDerAttr";

    @ManyToOne
    private JPAUser owner;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, JPAUser.class);
        this.owner = (JPAUser) owner;
    }

}
