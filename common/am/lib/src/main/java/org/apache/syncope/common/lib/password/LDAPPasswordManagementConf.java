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
package org.apache.syncope.common.lib.password;

import java.util.Map;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;

public class LDAPPasswordManagementConf extends AbstractLDAPConf implements PasswordManagementConf {

    private static final long serialVersionUID = 3721321025567652223L;

    /**
     * Username attribute required by LDAP.
     */
    private String usernameAttribute = "uid";

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(final String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    @Override
    public Map<String, Object> map(final PasswordManagementTO passwordManagementTO, final Mapper mapper) {
        return mapper.map(passwordManagementTO, this);
    }
}
