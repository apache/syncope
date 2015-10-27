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

import java.util.LinkedList;
import java.util.Scanner;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;

public class UserList extends AbstractUserCommand {

    public void list() {
        try {
            final Scanner scanIn = new Scanner(System.in);
            System.out.println(
                    "This operation could be print a lot of information "
                    + "on your screen. Do you want to continue? [yes/no]");
            final String answer = scanIn.nextLine();
            if ("yes".equalsIgnoreCase(answer)) {
                final PagedResult<UserTO> uResult = userSyncopeOperations.list();
                userResultManager.toView(new LinkedList<>(uResult.getResult()));
            } else if ("no".equalsIgnoreCase(answer)) {
                userResultManager.generic("List operation skipped");
            } else {
                userResultManager.generic("Invalid parameter, please use [yes/no]");
            }
        } catch (final SyncopeClientException ex) {
            userResultManager.generic(ex.getMessage());
        }
    }
}
