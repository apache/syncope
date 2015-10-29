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

public class UserCount extends AbstractUserCommand {

    private static final String COUNT_HELP_MESSAGE = "user --count";

    private final Input input;

    public UserCount(final Input input) {
        this.input = input;
    }

    public void count() {
        if (input.parameterNumber() == 0) {
            try {
                userResultManager.genericMessage("Total users: " + userSyncopeOperations.count());
            } catch (final SyncopeClientException ex) {
                userResultManager.genericError(ex.getMessage());
            }
        } else {
            userResultManager.unnecessaryParameters(input.listParameters(), COUNT_HELP_MESSAGE);
        }
    }
}
