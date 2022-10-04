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
package org.apache.syncope.core.provisioning.java.job;

import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.task.CommandTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class CommandRunJobDelegate extends AbstractSchedTaskJobDelegate<CommandTask> {

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

//        Optional<Command> command = Optional.empty();
//        try {
//            command = ImplementationManager.buildCommand(
//                    task.getCommand(),
//                    () -> PER_CONTEXT_COMMANDS.get(task.getCommand().getKey()),
//                    instance -> PER_CONTEXT_COMMANDS.put(task.getCommand().getKey(), instance));
//        } catch (Exception e) {
//            LOG.error("While building {}", task.getCommand(), e);
//        }
//        if (command.isEmpty()) {
//            throw new JobExecutionException("While building " + task.getCommand().getKey());
//        }
//
//        try {
//            return command.get().run();
//        } catch (Exception e) {
//            return "FAILURE\n\n" + ExceptionUtils2.getFullStackTrace(e);
//        }
        return "";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return task.isSaveExecs();
    }
}
