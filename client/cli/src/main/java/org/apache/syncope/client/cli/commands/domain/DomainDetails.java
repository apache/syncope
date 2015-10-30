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
package org.apache.syncope.client.cli.commands.domain;

import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainDetails extends AbstractDomainCommand {

    private static final Logger LOG = LoggerFactory.getLogger(DomainDetails.class);

    private static final String LIST_HELP_MESSAGE = "domain --details";

    private final Input input;

    public DomainDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                details.put("Total number", String.valueOf(domainSyncopeOperations.list().size()));
                domainResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about domain", ex);
                domainResultManager.genericError(ex.getMessage());
            }
        } else {
            domainResultManager.unnecessaryParameters(input.listParameters(), LIST_HELP_MESSAGE);
        }

    }
}
