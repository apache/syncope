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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
public interface AuthModuleConf extends BaseBean {

    interface Mapper {

        Map<String, Object> map(StaticAuthModuleConf conf);

        Map<String, Object> map(LDAPAuthModuleConf conf);

        Map<String, Object> map(JDBCAuthModuleConf conf);

        Map<String, Object> map(JaasAuthModuleConf conf);

        Map<String, Object> map(OIDCAuthModuleConf conf);

        Map<String, Object> map(SAML2IdPAuthModuleConf conf);

        Map<String, Object> map(SyncopeAuthModuleConf conf);

        Map<String, Object> map(GoogleMfaAuthModuleConf conf);

        Map<String, Object> map(DuoMfaAuthModuleConf conf);

        Map<String, Object> map(U2FAuthModuleConf conf);

        Map<String, Object> map(SimpleMfaAuthModuleConf conf);
    }

    Map<String, Object> map(Mapper mapper);
}
