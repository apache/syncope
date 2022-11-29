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
package org.apache.syncope.core.logic.job;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.core.logic.api.Command;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.java.job.AbstractSchedTaskJobDelegate;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class MacroRunJobDelegate extends AbstractSchedTaskJobDelegate<MacroTask> {

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Autowired
    protected Validator validator;

    protected final Map<String, Command<?>> perContextCommands = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < task.getCommands().size(); i++) {
            Implementation command = task.getCommands().get(i);

            Command<CommandArgs> runnable;
            try {
                runnable = (Command<CommandArgs>) ImplementationManager.build(
                        command,
                        () -> perContextCommands.get(command.getKey()),
                        instance -> perContextCommands.put(command.getKey(), instance));
            } catch (Exception e) {
                throw new JobExecutionException("Could not build " + command.getKey(), e);
            }

            String args = POJOHelper.serialize(task.getCommandArgs().get(i));

            output.append("Command[").append(i).append("]: ").
                    append(command.getKey()).append(" ").append(args).append("\n");
            if (dryRun) {
                output.append(command).append(' ').append(args);
            } else {
                try {
                    if (task.getCommandArgs().get(i) != null) {
                        Set<ConstraintViolation<Object>> violations = validator.validate(task.getCommandArgs().get(i));
                        if (!violations.isEmpty()) {
                            LOG.error("Errors while validating {}: {}", task.getCommandArgs().get(i), violations);
                            throw new IllegalArgumentException(task.getCommandArgs().get(i).getClass().getName());
                        }
                    }

                    output.append(runnable.run(task.getCommandArgs().get(i)));
                } catch (Exception e) {
                    if (task.isContinueOnError()) {
                        output.append("Continuing on error: <").append(e.getMessage()).append('>');
                        LOG.error("While running {} with args {}, continuing on error", command.getKey(), args, e);
                    } else {
                        throw new RuntimeException("While running " + command.getKey(), e);
                    }
                }
            }
            output.append("\n\n");
        }

        output.append("COMPLETED");
        return output.toString();
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return task.isSaveExecs();
    }
}
