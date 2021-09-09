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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;

public class JPAAnyUtilsFactory implements AnyUtilsFactory {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final EntityFactory entityFactory;

    protected final Map<AnyTypeKind, AnyUtils> instances = new HashMap<>(3);

    protected AnyUtils linkedAccountInstance;

    public JPAAnyUtilsFactory(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final EntityFactory entityFactory) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public AnyUtils getInstance(final AnyTypeKind anyTypeKind) {
        AnyUtils instance;
        synchronized (instances) {
            instance = instances.get(anyTypeKind);
            if (instance == null) {
                instance = new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, anyTypeKind, false);
                ApplicationContextProvider.getBeanFactory().autowireBean(instance);
                instances.put(anyTypeKind, instance);
            }
        }

        return instance;
    }

    @Override
    public AnyUtils getInstance(final Any<?> any) {
        AnyTypeKind type = null;
        if (any instanceof User) {
            type = AnyTypeKind.USER;
        } else if (any instanceof Group) {
            type = AnyTypeKind.GROUP;
        } else if (any instanceof AnyObject) {
            type = AnyTypeKind.ANY_OBJECT;
        }

        if (type == null) {
            throw new IllegalArgumentException("Any type not supported: " + any.getClass().getName());
        }

        return getInstance(type);
    }

    @Override
    public AnyUtils getLinkedAccountInstance() {
        synchronized (this) {
            if (linkedAccountInstance == null) {
                linkedAccountInstance = new JPAAnyUtils(
                        userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.USER, true);
            }
        }
        return linkedAccountInstance;
    }
}
