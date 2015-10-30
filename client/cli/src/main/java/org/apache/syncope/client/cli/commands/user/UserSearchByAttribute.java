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
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.realm.RealmSyncopeOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;

public class UserSearchByAttribute extends AbstractUserCommand {

    private static final String SEARCH_HELP_MESSAGE = "user --search-by-attribute {REALM} {ATTR-NAME}={ATTR-VALUE}";

    private final Input input;

    public UserSearchByAttribute(final Input input) {
        this.input = input;
    }

    public void search() {
        if (input.parameterNumber() >= 2) {
            final String realm = input.firstParameter();
            final Input.PairParameter pairParameter = input.toPairParameter(input.secondParameter());
            final RealmSyncopeOperations realmSyncopeOperations = new RealmSyncopeOperations();
            try {
                List<UserTO> userTOs;
                if (!realmSyncopeOperations.exists(realm)) {
                    userResultManager.genericMessage("Operation performed on root realm because " + realm
                            + "does not exists");
                }
                userTOs = userSyncopeOperations.searchByAttribute(
                        realm, pairParameter.getKey(), pairParameter.getValue());
                if (userTOs == null || userTOs.isEmpty()) {
                    userResultManager.genericMessage("No users found with attribute "
                            + pairParameter.getKey() + " and value " + pairParameter.getValue());
                } else {
                    userResultManager.printUsers(userTOs);
                }
            } catch (final WebServiceException | SyncopeClientException ex) {
                if (ex.getMessage().startsWith("NotFound")) {
                    userResultManager.notFoundError("User with " + pairParameter.getKey(), pairParameter.getValue());
                } else {
                    userResultManager.genericError(ex.getMessage());
                }
            } catch (final IllegalArgumentException ex) {
                userResultManager.genericError(ex.getMessage());
                userResultManager.genericError(SEARCH_HELP_MESSAGE);
            }
            userResultManager.commandOptionError(SEARCH_HELP_MESSAGE);
        }
    }
}
