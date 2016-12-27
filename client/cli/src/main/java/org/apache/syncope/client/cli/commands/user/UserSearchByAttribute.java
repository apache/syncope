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

import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSearchByAttribute extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserSearchByAttribute.class);

    private static final String SEARCH_HELP_MESSAGE = "user --search-by-attribute {REALM} {ATTR-NAME}={ATTR-VALUE}";

    private final Input input;

    public UserSearchByAttribute(final Input input) {
        this.input = input;
    }

    public void search() {
        if (input.parameterNumber() == 2) {
            String realm = input.firstParameter();
            Pair<String, String> pairParameter = Input.toPairParameter(input.secondParameter());
            try {
                if (!realmSyncopeOperations.exists(realm)) {
                    userResultManager.genericMessage(
                            "Operation performed on root realm because " + realm + " does not exists");
                }
                List<UserTO> userTOs =
                        userSyncopeOperations.searchByAttribute(
                                realm, pairParameter.getKey(), pairParameter.getValue());
                if (userTOs.isEmpty()) {
                    userResultManager.genericMessage("No users found with attribute "
                            + pairParameter.getKey() + " and value " + pairParameter.getValue());
                } else {
                    userResultManager.printUsers(userTOs);
                }
            } catch (WebServiceException | SyncopeClientException ex) {
                LOG.error("Error searching user", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    userResultManager.notFoundError("User with " + pairParameter.getKey(), pairParameter.getValue());
                } else {
                    userResultManager.genericError(ex.getMessage());
                }
            } catch (IllegalArgumentException ex) {
                LOG.error("Error searching user", ex);
                userResultManager.genericError(ex.getMessage());
                userResultManager.genericError(SEARCH_HELP_MESSAGE);
            }
        } else {
            userResultManager.commandOptionError(SEARCH_HELP_MESSAGE);
        }
    }
}
