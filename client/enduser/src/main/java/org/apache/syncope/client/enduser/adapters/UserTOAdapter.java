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
package org.apache.syncope.client.enduser.adapters;

import org.apache.syncope.client.enduser.model.UserTORequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserTOAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(UserTOAdapter.class);

    public UserTO fromUserTORequest(final UserTORequest userTORequest, final String oldSelfPassword) {

        final UserTO userTO = new UserTO();
        // set key if in update mode
        final Long key = userTORequest.getKey();
        if (key != null) {
            userTO.setKey(key);
        }
        // set username...
        userTO.setUsername(userTORequest.getUsername());
        // ...and password
        String requestPassword = userTORequest.getPassword();
        if (requestPassword == null || requestPassword.isEmpty()) {
            userTO.setPassword(oldSelfPassword == null ? null : oldSelfPassword);
        } else {
            userTO.setPassword(requestPassword);
        }

        //set security question and answer
        userTO.setSecurityQuestion(userTORequest.getSecurityQuestion());
        userTO.setSecurityAnswer(userTORequest.getSecurityAnswer());
        //set realm
        userTO.setRealm(userTORequest.getRealm());
        // add attributes
        userTO.getPlainAttrs().addAll(userTORequest.getPlainAttrs().values());
        userTO.getDerAttrs().addAll(userTORequest.getDerAttrs().values());
        userTO.getVirAttrs().addAll(userTORequest.getVirAttrs().values());
        // add resources
        userTO.getResources().addAll(userTORequest.getResources());

        return userTO;
    }

    public UserTORequest toUserTORequest(final UserTO userTO) {

        final UserTORequest userTORequest = new UserTORequest().
                key(userTO.getKey()).
                username(userTO.getUsername()).
                securityQuestion(userTO.getSecurityQuestion()).
                securityAnswer(userTO.getSecurityAnswer()).
                realm(userTO.getRealm());

        userTORequest.getPlainAttrs().putAll(userTO.getPlainAttrMap());
        userTORequest.getDerAttrs().putAll(userTO.getDerAttrMap());
        userTORequest.getVirAttrs().putAll(userTO.getVirAttrMap());
        
        userTORequest.getResources().addAll(userTO.getResources());

        return userTORequest;
    }

}
