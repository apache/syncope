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

import java.util.Set;
import org.apache.syncope.client.cli.view.Messages;
import org.apache.syncope.common.lib.types.ConnConfProperty;

public abstract class CommonsResultManager {

    public void numberFormatException(final String what, final String key) {
        Messages.printIdNotNumberDeletedMessage(what, key);
    }

    public void deletedMessage(final String what, final String key) {
        Messages.printDeletedMessage(what, key);
    }

    public void notFoundError(final String what, final String parameter) {
        Messages.printNofFoundMessage(what, parameter);
    }

    public void notBooleanDeletedError(final String what, final String key) {
        Messages.printNotBooleanDeletedMessage(what, key);
    }

    public void typeNotValidError(final String what, final String parameter, final String[] options) {
        Messages.printTypeNotValidMessage(what, parameter, options);
    }

    public void commandOptionError(final String message) {
        Messages.printCommandOptionMessage(message);
    }

    public void defaultError(final String option, final String helpMessage) {
        Messages.printDefaultMessage(option, helpMessage);
    }

    public void generic(final String... messages) {
        Messages.printMessage(messages);
    }
    
    protected void printConfiguration(final Set<ConnConfProperty> configurationPropertys) {
        for (final ConnConfProperty configuration : configurationPropertys) {
            System.out.println("       name: " + configuration.getSchema().getName());
            System.out.println("       values: " + configuration.getValues());
            System.out.println("       type: " + configuration.getSchema().getType());
            System.out.println("       display name: " + configuration.getSchema().getDisplayName());
            System.out.println("       help message: " + configuration.getSchema().getHelpMessage());
            System.out.println("       order: " + configuration.getSchema().getOrder());
            System.out.println("       default values: " + configuration.getSchema().getDefaultValues());
            System.out.println("       confidential: " + configuration.getSchema().isConfidential());
            System.out.println("       required: " + configuration.getSchema().isRequired());
            System.out.println("       overridable: " + configuration.isOverridable());
            System.out.println("");
        }
    }
}
