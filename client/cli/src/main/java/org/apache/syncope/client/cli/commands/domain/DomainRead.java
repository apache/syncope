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

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DomainTO;

public class DomainRead extends AbstractDomainCommand {

    private static final String READ_HELP_MESSAGE = "domain --read {DOMAIN-KEY} {DOMAIN-KEY} [...]";

    private final Input input;

    public DomainRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    final DomainTO domainTO = domainService.read(parameter);
                    domainResultManager.generic(domainTO.getKey());
                } catch (final SyncopeClientException ex) {
                    if (ex.getMessage().startsWith("NotFound")) {
                        domainResultManager.notFoundError("Domain", parameter);
                    } else {
                        domainResultManager.generic(ex.getMessage());
                    }
                }
            }
        } else {
            domainResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
