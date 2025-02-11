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
package org.apache.syncope.core.persistence.api.entity;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;

public class AnyUtilsFactory {

    protected final AnyUtils userAnyUtils;

    protected final AnyUtils groupAnyUtils;

    protected final AnyUtils anyObjectAnyUtils;

    public AnyUtilsFactory(
            final AnyUtils userAnyUtils,
            final AnyUtils groupAnyUtils,
            final AnyUtils anyObjectAnyUtils) {

        this.userAnyUtils = userAnyUtils;
        this.groupAnyUtils = groupAnyUtils;
        this.anyObjectAnyUtils = anyObjectAnyUtils;
    }

    public AnyUtils getInstance(final AnyTypeKind anyTypeKind) {
        switch (anyTypeKind) {
            case ANY_OBJECT:
                return anyObjectAnyUtils;

            case GROUP:
                return groupAnyUtils;

            case USER:
            default:
                return userAnyUtils;
        }
    }

    public AnyUtils getInstance(final Any any) {
        AnyTypeKind anyTypeKind = null;
        if (any instanceof User) {
            anyTypeKind = AnyTypeKind.USER;
        } else if (any instanceof Group) {
            anyTypeKind = AnyTypeKind.GROUP;
        } else if (any instanceof AnyObject) {
            anyTypeKind = AnyTypeKind.ANY_OBJECT;
        }

        if (anyTypeKind == null) {
            throw new IllegalArgumentException("Any type not supported: " + any.getClass().getName());
        }

        return getInstance(anyTypeKind);
    }
}
