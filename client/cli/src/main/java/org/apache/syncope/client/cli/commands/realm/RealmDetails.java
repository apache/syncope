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

import java.util.Map;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmDetails extends AbstractRealmCommand {

    private static final Logger LOG = LoggerFactory.getLogger(RealmDetails.class);

    private static final String DETAILS_HELP_MESSAGE = "realm --details";

    private final Input input;

    public RealmDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedMap<>();
                details.put("Total number", String.valueOf(realmSyncopeOperations.list().size()));
                realmResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about realm", ex);
                realmResultManager.genericError(ex.getMessage());
            }
        } else {
            realmResultManager.unnecessaryParameters(input.listParameters(), DETAILS_HELP_MESSAGE);
        }
    }
}
