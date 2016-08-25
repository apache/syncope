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
package org.apache.syncope.client.cli.commands.entitlement;

import java.util.Collection;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.to.RoleTO;

public class EntitlementResultManager extends CommonsResultManager {

    public void toView(final Collection<String> entitlements) {
        System.out.println("");
        for (final String entitlement : entitlements) {
            System.out.println("- " + entitlement);
        }
        System.out.println("");
    }

    public void rolesToView(final Collection<RoleTO> roles) {
        System.out.println("");
        for (final RoleTO role : roles) {
            printRole(role);
        }
    }

    private void printRole(final RoleTO roleTO) {
        System.out.println(" > ROLE KEY: " + roleTO.getKey());
        System.out.println("    REALMS: ");
        printRealms(roleTO.getRealms());
        System.out.println("");
    }

    private void printRealms(final Collection<String> realms) {
        for (final String realm : realms) {
            System.out.println("       - " + realm);
        }
    }
}
