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

public class AnyObjectRead extends AbstractAnyObjectCommand {

    private static final Logger LOG = LoggerFactory.getLogger(AnyObjectRead.class);

    private static final String READ_HELP_MESSAGE = "any --read {ANY_OBJECT-KEY} {ANY_OBJECT-KEY} [...]";

    private final Input input;

    public AnyObjectRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            for (final String parameter : input.getParameters()) {
                try {
                    anyResultManager.printAny(anySyncopeOperations.read(parameter));
                } catch (final SyncopeClientException ex) {
                    LOG.error("Error reading group", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        anyResultManager.notFoundError("Any object", parameter);
                    } else {
                        anyResultManager.genericError(ex.getMessage());
                    }
                } catch (final NumberFormatException ex) {
                    anyResultManager.numberFormatException("any object", parameter);
                }
            }
        } else {
            anyResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}
