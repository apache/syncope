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
package org.apache.syncope.client.console.commons;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public enum AttrLayoutType {

    ADMIN_USER("admin.user.layout", Mode.ADMIN, AnyTypeKind.USER),
    SELF_USER("self.user.layout", Mode.SELF, AnyTypeKind.USER),
    ADMIN_GROUP("admin.group.layout", Mode.ADMIN, AnyTypeKind.GROUP),
    SELF_GROUP("self.group.layout", Mode.SELF, AnyTypeKind.GROUP);

    private final String confKey;

    private final Mode mode;

    private final AnyTypeKind anyTypeKind;

    AttrLayoutType(final String confKey, final Mode mode, final AnyTypeKind anyTypeKind) {
        this.confKey = confKey;
        this.mode = mode;
        this.anyTypeKind = anyTypeKind;
    }

    public String getConfKey() {
        return confKey;
    }

    public Mode getMode() {
        return mode;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public static List<String> confKeys() {
        List<String> confKeys = new ArrayList<>();
        for (AttrLayoutType value : values()) {
            confKeys.add(value.getConfKey());
        }

        return confKeys;
    }

    public static AttrLayoutType valueOf(final Mode mode, final AnyTypeKind anyTypeKind) {
        AttrLayoutType result = null;
        if (mode == Mode.ADMIN) {
            switch (anyTypeKind) {
                case USER:
                    result = ADMIN_USER;
                    break;

                case GROUP:
                    result = ADMIN_GROUP;
                    break;

                default:
            }
        } else if (mode == Mode.SELF) {
            switch (anyTypeKind) {
                case USER:
                    result = SELF_USER;
                    break;

                case GROUP:
                    result = SELF_GROUP;
                    break;

                default:
            }
        }

        return result;
    }
}
