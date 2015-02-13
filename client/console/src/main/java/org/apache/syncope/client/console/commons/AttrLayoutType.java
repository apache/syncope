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
import org.apache.syncope.common.lib.types.AttributableType;

public enum AttrLayoutType {

    ADMIN_USER("admin.user.layout", Mode.ADMIN, AttributableType.USER),
    SELF_USER("self.user.layout", Mode.SELF, AttributableType.USER),
    ADMIN_ROLE("admin.role.layout", Mode.ADMIN, AttributableType.ROLE),
    SELF_ROLE("self.role.layout", Mode.SELF, AttributableType.ROLE),
    ADMIN_MEMBERSHIP("admin.membership.layout", Mode.ADMIN, AttributableType.MEMBERSHIP),
    SELF_MEMBERSHIP("self.membership.layout", Mode.SELF, AttributableType.MEMBERSHIP);

    private final String confKey;

    private final Mode mode;

    private final AttributableType attrType;

    AttrLayoutType(final String confKey, final Mode mode, final AttributableType attrType) {
        this.confKey = confKey;
        this.mode = mode;
        this.attrType = attrType;
    }

    public String getConfKey() {
        return confKey;
    }

    public Mode getMode() {
        return mode;
    }

    public AttributableType getAttrType() {
        return attrType;
    }

    public static List<String> confKeys() {
        List<String> confKeys = new ArrayList<String>();
        for (AttrLayoutType value : values()) {
            confKeys.add(value.getConfKey());
        }

        return confKeys;
    }

    public static AttrLayoutType valueOf(final Mode mode, final AttributableType attrType) {
        AttrLayoutType result = null;
        if (mode == Mode.ADMIN) {
            switch (attrType) {
                case USER:
                    result = ADMIN_USER;
                    break;

                case MEMBERSHIP:
                    result = ADMIN_MEMBERSHIP;
                    break;

                case ROLE:
                    result = ADMIN_ROLE;
                    break;

                default:
            }
        } else if (mode == Mode.SELF) {
            switch (attrType) {
                case USER:
                    result = SELF_USER;
                    break;

                case MEMBERSHIP:
                    result = SELF_MEMBERSHIP;
                    break;

                case ROLE:
                    result = SELF_ROLE;
                    break;

                default:
            }
        }

        return result;
    }
}
