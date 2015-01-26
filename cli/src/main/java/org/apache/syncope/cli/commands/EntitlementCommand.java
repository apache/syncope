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
package org.apache.syncope.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.syncope.cli.SyncopeServices;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.wrap.EntitlementTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(
        commandNames = "entitlement",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope entitlement service")
public class EntitlementCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EntitlementCommand.class);

    private static final Class SYNCOPE_ENTITLEMENT_CLASS = EntitlementService.class;

    private final String helpMessage = "Usage: entitlement [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -lo, --list-own \n";

    @Parameter(names = {"-lo", "--list-own"})
    public boolean listOwn = false;

    @Override
    public void execute() {
        final EntitlementService entitlementService = (EntitlementService) SyncopeServices.
                get(SYNCOPE_ENTITLEMENT_CLASS);
        LOG.debug("Entitlement service successfully created");

        if (help) {
            LOG.debug("- entitlement help command");
            System.out.println(helpMessage);
        } else if (list) {
            System.out.println("All entitlement:");
            for (final EntitlementTO entitlementTO : entitlementService.getAllEntitlements()) {
                System.out.println("  *** " + entitlementTO.getElement());
            }
        } else if (listOwn) {
            System.out.println("All own entitlement:");
            for (final EntitlementTO entitlementTO : entitlementService.getOwnEntitlements()) {
                System.out.println("  *** " + entitlementTO.getElement());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}
