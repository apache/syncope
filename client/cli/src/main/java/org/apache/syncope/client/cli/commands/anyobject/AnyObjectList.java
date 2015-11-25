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
package org.apache.syncope.client.cli.commands.anyobject;

import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyObjectList extends AbstractAnyObjectCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AnyObjectList.class);

    private static final String LIST_HELP_MESSAGE = "any --list {ANY-TYPE}";

    private final Input input;

    public AnyObjectList(final Input input) {
        this.input = input;
    }

    public void list() {
        if (input.parameterNumber() == 1) {
            try {
                anyResultManager.printAnys(anySyncopeOperations.list(input.firstParameter()));
            } catch (final SyncopeClientException ex) {
                LOG.error("Error listing any object", ex);
                anyResultManager.genericError(ex.getMessage());
            }
        } else {
            anyResultManager.unnecessaryParameters(input.listParameters(), LIST_HELP_MESSAGE);
        }
    }
}
