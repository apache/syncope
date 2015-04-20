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
package org.apache.syncope.client.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(
        commandNames = "policy",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope policy service")
public class PolicyCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyCommand.class);

    private final String helpMessage = "Usage: policy [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -ll, --list-policy \n"
            + "       Syntax: -ll={POLICY-TYPE} \n"
            + "    -r, --read \n"
            + "       Syntax: -r={POLICY-ID} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={POLICY-ID}";

    @Parameter(names = { "-ll", "--list-policy" })
    public String policyType;

    @Parameter(names = { "-r", "--read" })
    public Long policyIdToRead = -1L;

    @Parameter(names = { "-d", "--delete" })
    public Long policyIdToDelete = -1L;

    @Override
    public void execute() {
        final PolicyService policyService = SyncopeServices.get(PolicyService.class);
        LOG.debug("Policy service successfully created");

        if (help) {
            LOG.debug("- policy help command");
            System.out.println(helpMessage);
        } else if (list) {

        } else if (StringUtils.isNotBlank(policyType)) {
            LOG.debug("- policy list command for type {}", policyType);
            try {
                for (final AbstractPolicyTO policyTO : policyService.list(PolicyType.valueOf(policyType))) {
                    System.out.println(policyTO);
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            } catch (final IllegalArgumentException ex) {
                System.out.println(" - Error: " + policyType + " isn't a valid policy type, try with:");
                for (final PolicyType type : PolicyType.values()) {
                    System.out.println("  *** " + type.name());
                }
            }
        } else if (policyIdToRead > -1L) {
            LOG.debug("- policy read {} command", policyIdToRead);
            try {
                System.out.println(policyService.read(policyIdToRead));
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (policyIdToDelete > -1L) {
            try {
                LOG.debug("- policy delete {} command", policyIdToDelete);
                policyService.delete(policyIdToDelete);
                System.out.println(" - Report " + policyIdToDelete + " deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}
