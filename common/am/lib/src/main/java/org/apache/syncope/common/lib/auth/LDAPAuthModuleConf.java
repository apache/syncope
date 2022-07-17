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
package org.apache.syncope.common.lib.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;

public class LDAPAuthModuleConf extends AbstractLDAPConf implements AuthModuleConf {

    private static final long serialVersionUID = -471527731042579422L;

    /**
     * The attribute value that should be used
     * for the authenticated username, upon a successful authentication
     * attempt.
     */
    private String userIdAttribute;

    /**
     * List of attribute names to fetch as user attributes.
     */
    private final List<String> principalAttributeList = new ArrayList<>();

    public String getUserIdAttribute() {
        return userIdAttribute;
    }

    public void setUserIdAttribute(final String userIdAttribute) {
        this.userIdAttribute = userIdAttribute;
    }

    public List<String> getPrincipalAttributeList() {
        return principalAttributeList;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
