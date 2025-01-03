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
package org.apache.syncope.core.provisioning.api.macro;

import jakarta.validation.ValidationException;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.form.SyncopeForm;

/**
 * Interface for actions to be performed during macro execution.
 */
public interface MacroActions {

    default Optional<String> getDefaultValue(String formProperty) {
        return Optional.empty();
    }

    default Map<String, String> getDropdownValues(String formProperty) {
        return Map.of();
    }

    default void validate(SyncopeForm form, Map<String, Object> vars) throws ValidationException {
        // does nothing by default
    }

    default void beforeAll() {
        // does nothing by default
    }

    default void beforeCommand(Command<CommandArgs> command, CommandArgs args) {
        // does nothing by default
    }

    default void afterCommand(Command<CommandArgs> command, CommandArgs args, String output) {
        // does nothing by default
    }

    default StringBuilder afterAll(StringBuilder output) {
        return output;
    }
}
