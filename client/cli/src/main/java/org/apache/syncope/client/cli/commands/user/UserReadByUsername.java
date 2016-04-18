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

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserReadByUsername extends AbstractUserCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UserReadByUsername.class);

    private static final String READ_HELP_MESSAGE = "user --read-by-username {USERNAME} {USERNAME} [...]";

    private final Input input;

    public UserReadByUsername(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.getParameters().length >= 1) {
            final List<UserTO> userTOs = new ArrayList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    userTOs.add(userSyncopeOperations.read(parameter));
                } catch (final SyncopeClientException | WebServiceException ex) {
                    LOG.error("Error reading user", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        userResultManager.notFoundError("User", parameter);
                    } else {
                        userResultManager.genericError(ex.getMessage());
                    }
                    break;
                } catch (final NumberFormatException ex) {
                    LOG.error("Error reading user", ex);
                    userResultManager.numberFormatException("user", parameter);
                }
            }
            userResultManager.printUsers(userTOs);
        } else {
            userResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
