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

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserGetUsername extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserGetUsername.class);

    private static final String GET_HELP_MESSAGE = "user --get-username {USER-ID}";

    private final Input input;

    public UserGetUsername(final Input input) {
        this.input = input;
    }

    public void get() {
        if (input.getParameters().length == 1) {
            try {
                final String username = userSyncopeOperations.getUsernameFromId(input.firstParameter());
                userResultManager.genericMessage(input.firstParameter() + " username is " + username);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error getting user", ex);
                userResultManager.genericError(ex.getMessage());
            }
        } else {
            userResultManager.commandOptionError(GET_HELP_MESSAGE);
        }
    }
}
