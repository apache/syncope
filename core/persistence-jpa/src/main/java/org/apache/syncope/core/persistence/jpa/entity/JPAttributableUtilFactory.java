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

import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.springframework.stereotype.Component;

@Component
public class JPAttributableUtilFactory implements AttributableUtilFactory {

    @Override
    public AttributableUtil getInstance(final AttributableType type) {
        return new JPAAttributableUtil(type);
    }

    @Override
    public AttributableUtil getInstance(final String attributableType) {
        return new JPAAttributableUtil(AttributableType.valueOf(attributableType));
    }

    @Override
    public AttributableUtil getInstance(final ObjectClass objectClass) {
        AttributableType type = null;
        if (ObjectClass.ACCOUNT.equals(objectClass)) {
            type = AttributableType.USER;
        }
        if (ObjectClass.GROUP.equals(objectClass)) {
            type = AttributableType.GROUP;
        }

        if (type == null) {
            throw new IllegalArgumentException("ObjectClass not supported: " + objectClass);
        }

        return new JPAAttributableUtil(type);
    }

    @Override
    public AttributableUtil getInstance(final Attributable<?, ?, ?> attributable) {
        AttributableType type = null;
        if (attributable instanceof User) {
            type = AttributableType.USER;
        }
        if (attributable instanceof Group) {
            type = AttributableType.GROUP;
        }
        if (attributable instanceof Membership) {
            type = AttributableType.MEMBERSHIP;
        }
        if (attributable instanceof Conf) {
            type = AttributableType.CONFIGURATION;
        }

        if (type == null) {
            throw new IllegalArgumentException("Attributable type not supported: " + attributable.getClass().getName());
        }

        return new JPAAttributableUtil(type);
    }

}
