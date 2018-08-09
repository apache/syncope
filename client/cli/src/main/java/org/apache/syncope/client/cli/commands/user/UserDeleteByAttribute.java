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
package org.apache.syncope.client.cli.commands.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDeleteByAttribute extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserDeleteByAttribute.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SEARCH_HELP_MESSAGE = "user --delete-by-attribute {REALM} {ATTR-NAME}={ATTR-VALUE}";

    private final Input input;

    public UserDeleteByAttribute(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.parameterNumber() == 2) {
            String realm = input.firstParameter();
            Pair<String, String> pairParameter = Input.toPairParameter(input.secondParameter());
            try {
                if (!realmSyncopeOperations.exists(realm)) {
                    userResultManager.notFoundError("Realm", realm);
                    return;
                }
                List<BatchResponseItem> results = userSyncopeOperations.deleteByAttribute(
                        realm, pairParameter.getKey(), pairParameter.getValue());

                Map<String, String> failedUsers = new HashMap<>();
                AtomicReference<Integer> deletedUsers = new AtomicReference<>(0);

                results.forEach(item -> {
                    if (item.getStatus() == Response.Status.OK.getStatusCode()) {
                        deletedUsers.getAndSet(deletedUsers.get() + 1);
                    } else {
                        try {
                            ProvisioningResult<UserTO> user = MAPPER.readValue(item.getContent(),
                                    new TypeReference<ProvisioningResult<UserTO>>() {
                            });
                            failedUsers.put(
                                    user.getEntity().getUsername(),
                                    item.getHeaders().get(RESTHeaders.ERROR_CODE).toString());
                        } catch (IOException ioe) {
                            LOG.error("Error reading {}", item.getContent(), ioe);
                        }
                    }
                });

                userResultManager.genericMessage("Deleted users: " + deletedUsers);
                if (!failedUsers.isEmpty()) {
                    userResultManager.printFailedUsers(failedUsers);
                }
            } catch (Exception e) {
                LOG.error("Error searching user", e);
                if (e.getMessage().startsWith("NotFound")) {
                    userResultManager.notFoundError("User with " + pairParameter.getKey(), pairParameter.getValue());
                } else {
                    userResultManager.genericError(e.getMessage());
                }
            }
        } else {
            userResultManager.commandOptionError(SEARCH_HELP_MESSAGE);
        }
    }
}
