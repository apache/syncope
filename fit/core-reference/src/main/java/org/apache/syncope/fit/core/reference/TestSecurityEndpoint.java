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
package org.apache.syncope.fit.core.reference;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.transaction.annotation.Transactional;

@Endpoint(id = "testSecurity")
public class TestSecurityEndpoint {

    public enum Element {
        PASSWORD,
        TOKEN,
        FIRST_LINKED_ACCOUNT_PASSWORD;

    }

    private final UserDAO userDAO;

    public TestSecurityEndpoint(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @ReadOperation
    @Transactional(readOnly = true)
    public Map<String, Object> get(final @Selector Element element, final @Selector String key) {
        User user = userDAO.findById(key).orElseThrow(() -> new NotFoundException("User " + key));

        return switch (element) {
            case PASSWORD ->
                user.getPassword() == null
                ? Map.of()
                : Map.of("password", user.getPassword());

            case TOKEN ->
                user.getToken() == null
                ? Map.of()
                : Map.of(
                "token", user.getToken(),
                "tokenExpireTime", user.getTokenExpireTime());

            case FIRST_LINKED_ACCOUNT_PASSWORD -> {
                if (user.getLinkedAccounts().isEmpty()) {
                    yield Map.of();
                }

                Map<String, Object> result = new HashMap<>();
                result.put("password", user.getLinkedAccounts().getFirst().getPassword());
                yield result;
            }

            default ->
                Map.of();
        };
    }
}
