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
package org.apache.syncope.client.enduser.layout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;

public class UserFormLayouts implements Serializable {

    private static final long serialVersionUID = 9106933641699158419L;

    private final Map<String, UserFormLayoutInfo> layouts;

    @JsonCreator
    public UserFormLayouts(@JsonProperty("layouts") final Map<String, UserFormLayoutInfo> layouts) {
        this.layouts = layouts == null ? new HashMap<>() : new HashMap<>(layouts);
        this.layouts.putIfAbsent(SyncopeConstants.ROOT_REALM, new UserFormLayoutInfo());
    }

    public Map<String, UserFormLayoutInfo> getLayouts() {
        return layouts;
    }

    public UserFormLayoutInfo getLayout(final String realm) {
        for (String current = StringUtils.isBlank(realm) ? SyncopeConstants.ROOT_REALM : realm;
                StringUtils.isNotBlank(current);
                current = StringUtils.substringBeforeLast(current, "/")) {

            UserFormLayoutInfo layout = layouts.get(current);
            if (layout != null) {
                return layout;
            }
        }
        return layouts.get(SyncopeConstants.ROOT_REALM);
    }
}
