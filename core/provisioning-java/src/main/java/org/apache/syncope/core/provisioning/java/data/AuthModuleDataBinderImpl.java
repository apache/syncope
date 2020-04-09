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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;

@Component
public class AuthModuleDataBinderImpl implements AuthModuleDataBinder {

    @Autowired
    private EntityFactory entityFactory;

    private AuthModule getAuthModule(final AuthModule authModule, final AuthModuleTO authModuleTO) {
        AuthModule result = authModule;

        if (result == null) {
            result = entityFactory.newEntity(AuthModule.class);
        }

        AuthModule authenticationModule = AuthModule.class.cast(result);
        AuthModuleTO authenticationModuleTO = AuthModuleTO.class.cast(authModuleTO);

        authenticationModule.setName(authenticationModuleTO.getName());
        authenticationModule.setConf(authenticationModuleTO.getConf());
        authenticationModule.setDescription(authenticationModuleTO.getDescription());
        // remove all profile items not contained in the TO
        authenticationModule.getProfileItems().
                removeIf(item -> !authenticationModuleTO.getProfileItems().stream().
                anyMatch(otherItem -> item.getKey().equals(otherItem.getKey())));

        return result;
    }

    @Override
    public AuthModule create(final AuthModuleTO authModuleTO) {
        return getAuthModule(null, authModuleTO);
    }

    @Override
    public AuthModule update(final AuthModule authModule, final AuthModuleTO authModuleTO) {
        return getAuthModule(authModule, authModuleTO);
    }

    @Override
    public AuthModuleTO getAuthModuleTO(final AuthModule authModule) {
        AuthModuleTO authModuleTO = new AuthModuleTO();

        authModuleTO.setName(authModule.getName());
        authModuleTO.setKey(authModule.getKey());
        authModuleTO.setDescription(authModule.getDescription());
        authModuleTO.setConf(authModule.getConf());
        authModuleTO.getProfileItems().forEach(item -> {
            authModuleTO.add(item);
        });

        return authModuleTO;
    }
}
