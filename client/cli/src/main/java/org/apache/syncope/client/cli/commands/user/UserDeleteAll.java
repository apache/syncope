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
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDeleteAll extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserDeleteAll.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DELETE_ALL_HELP_MESSAGE = "user --delete-all {REALM}";

    private final Input input;

    public UserDeleteAll(final Input input) {
        this.input = input;
    }

    public void delete() {
        if (input.parameterNumber() == 1) {
            try {
                final Scanner scanIn = new Scanner(System.in);
                System.out.println(
                        "\nRunning this operation you will delete all the realm users managed by Syncope, "
                        + "are you sure? [yes/no]");
                final String answer = scanIn.nextLine();
                if ("yes".equalsIgnoreCase(answer)) {
                    System.out.println("\nUsername:");
                    final String username = scanIn.nextLine();
                    System.out.println("\nPassword:");
                    final String password = scanIn.nextLine();
                    if (userSyncopeOperations.auth(username, password)) {
                        System.out.println("Deleting process started");
                        final String realm = input.firstParameter();
                        if (!realmSyncopeOperations.exists(realm)) {
                            userResultManager.notFoundError("Realm", realm);
                            return;
                        }
                        List<BatchResponseItem> results = userSyncopeOperations.deleteAll(realm);

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
                    } else {
                        userResultManager.genericError("Authentication error");
                    }
                } else if ("no".equalsIgnoreCase(answer)) {
                    userResultManager.genericError("Delete all operation skipped");
                } else {
                    userResultManager.genericError("Invalid parameter, please use [yes/no]");
                }
            } catch (Exception e) {
                LOG.error("Error deleting user", e);
                userResultManager.genericError(e.getMessage());
            }
        } else {
            userResultManager.commandOptionError(DELETE_ALL_HELP_MESSAGE);
        }
    }
}
