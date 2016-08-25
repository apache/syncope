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
package org.apache.syncope.client.cli.commands.realm;

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.RealmTO;

public class RealmResultManager extends CommonsResultManager {

    public void printRealms(final List<RealmTO> realmTOs) {
        System.out.println("");
        for (final RealmTO realmTO : realmTOs) {
            printRealm(realmTO);
        }
    }

    private void printRealm(final RealmTO realmTO) {
        System.out.println(" > REALM KEY: " + realmTO.getKey());
        System.out.println("    name: " + realmTO.getName());
        System.out.println("    full path: " + realmTO.getFullPath());
        System.out.println("    actions: " + realmTO.getActionsClassNames());
        System.out.println("    templates: " + realmTO.getTemplates());
        System.out.println("    parent key: " + realmTO.getParent());
        System.out.println("    account policy key: " + realmTO.getAccountPolicy());
        System.out.println("    password policy key: " + realmTO.getPasswordPolicy());
        System.out.println("");
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("realms details", details);
    }
}
