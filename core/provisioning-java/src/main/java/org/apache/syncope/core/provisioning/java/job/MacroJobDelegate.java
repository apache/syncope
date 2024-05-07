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

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.macro.Command;
import org.apache.syncope.core.provisioning.api.macro.MacroActions;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.util.ReflectionUtils;

public class MacroJobDelegate extends AbstractSchedTaskJobDelegate<MacroTask> {

    public static final String MACRO_TASK_FORM_JOBDETAIL_KEY = "macroTaskForm";

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Autowired
    protected Validator validator;

    @Resource(name = "batchExecutor")
    protected VirtualThreadPoolTaskExecutor executor;

    protected final Map<String, MacroActions> perContextActions = new ConcurrentHashMap<>();

    protected final Map<String, Command<?>> perContextCommands = new ConcurrentHashMap<>();

    protected boolean validate(final FormPropertyDef fpd, final String value, final Optional<MacroActions> actions) {
        if (!fpd.isWritable()) {
            return false;
        }

        return switch (fpd.getType()) {
            case Enum ->
                fpd.getEnumValues().containsKey(value);
            case Dropdown ->
                actions.map(a -> a.getDropdownValues(fpd.getKey()).containsKey(value)).orElse(false);
            default ->
                value != null;
        };
    }

    protected Optional<JexlContext> check(
            final SyncopeForm macroTaskForm,
            final Optional<MacroActions> actions,
            final StringBuilder output) throws JobExecutionException {

        if (macroTaskForm == null) {
            return Optional.empty();
        }

        // check if there is any required property with no value provided
        Set<String> missingFormProperties = task.getFormPropertyDefs().stream().
                filter(FormPropertyDef::isRequired).
                map(fpd -> Pair.of(
                fpd.getKey(),
                macroTaskForm.getProperty(fpd.getKey()).map(p -> p.getValue() != null))).
                filter(pair -> pair.getRight().isEmpty()).
                map(Pair::getLeft).
                collect(Collectors.toSet());
        if (!missingFormProperties.isEmpty()) {
            throw new JobExecutionException("Required form properties missing: " + missingFormProperties);
        }

        // if validator is defined, validate the provided form
        try {
            actions.ifPresent(a -> a.validate(macroTaskForm));
        } catch (ValidationException e) {
            throw new JobExecutionException("Invalid form submitted for task " + task.getKey(), e);
        }

        // build the JEXL context where variables are mapped to property values, built according to the defined type
        Map<String, Object> vars = macroTaskForm.getProperties().stream().
                map(p -> task.getFormPropertyDefs().stream().
                filter(fpd -> fpd.getKey().equals(p.getId()) && validate(fpd, p.getValue(), actions)).findFirst().
                map(fpd -> Pair.of(fpd, p.getValue()))).
                filter(Optional::isPresent).map(Optional::get).
                map(pair -> {
                    Object value;
                    switch (pair.getLeft().getType()) {
                        case Boolean:
                            value = BooleanUtils.toBoolean(pair.getRight());
                            break;

                        case Date:
                            value = StringUtils.isBlank(pair.getLeft().getDatePattern())
                                    ? FormatUtils.parseDate(pair.getRight())
                                    : FormatUtils.parseDate(pair.getRight(), pair.getLeft().getDatePattern());
                            break;

                        case Long:
                            value = NumberUtils.toLong(pair.getRight());
                            break;

                        case Enum:
                        case Dropdown:
                        case String:
                        case Password:
                        default:
                            value = pair.getRight();
                    }

                    return Pair.of(pair.getLeft().getKey(), value);
                }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        output.append("Form parameter values: ").append(vars).append("\n\n");

        return vars.isEmpty() ? Optional.empty() : Optional.of(new MapContext(vars));
    }

    protected String run(
            final List<Pair<Command<CommandArgs>, CommandArgs>> commands,
            final Optional<MacroActions> actions,
            final StringBuilder output,
            final boolean dryRun)
            throws JobExecutionException {

        Future<AtomicReference<Pair<String, Exception>>> future = executor.submit(
                new DelegatingSecurityContextCallable<>(() -> {

                    AtomicReference<Pair<String, Exception>> error = new AtomicReference<>();

                    for (int i = 0; i < commands.size() && error.get() == null; i++) {
                        Pair<Command<CommandArgs>, CommandArgs> command = commands.get(i);

                        try {
                            String args = POJOHelper.serialize(command.getRight());
                            output.append("Command[").append(command.getLeft().getClass().getName()).append("]: ").
                                    append(args).append("\n");

                            if (!dryRun) {
                                actions.ifPresent(a -> a.beforeCommand(command.getLeft(), command.getRight()));

                                String cmdOut = command.getLeft().run(command.getRight());

                                actions.ifPresent(a -> a.afterCommand(command.getLeft(), command.getRight(), cmdOut));

                                output.append(cmdOut);
                            }
                        } catch (Exception e) {
                            if (task.isContinueOnError()) {
                                output.append("Continuing on error: <").append(e.getMessage()).append('>');

                                LOG.error("While running {} with args {}, continuing on error",
                                        command.getLeft().getClass().getName(), command.getRight(), e);
                            } else {
                                error.set(Pair.of(command.getLeft().getClass().getName(), e));
                            }
                        }
                        output.append("\n\n");
                    }

                    return error;
                }));

        try {
            AtomicReference<Pair<String, Exception>> error = future.get();
            if (error.get() != null) {
                throw new JobExecutionException("While running " + error.get().getLeft(), error.get().getRight());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new JobExecutionException("While waiting for macro commands completion", e);
        }

        output.append("COMPLETED");

        return actions.filter(a -> !dryRun).map(a -> a.afterAll(output)).orElse(output).toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        Optional<MacroActions> actions;
        if (task.getMacroActions() == null) {
            actions = Optional.empty();
        } else {
            try {
                actions = Optional.of(ImplementationManager.build(
                        task.getMacroActions(),
                        () -> perContextActions.get(task.getMacroActions().getKey()),
                        instance -> perContextActions.put(task.getMacroActions().getKey(), instance)));
            } catch (Exception e) {
                throw new JobExecutionException("Could not build " + task.getMacroActions().getKey(), e);
            }
        }

        StringBuilder output = new StringBuilder();

        SyncopeForm macroTaskForm = (SyncopeForm) context.getData().get(MACRO_TASK_FORM_JOBDETAIL_KEY);
        Optional<JexlContext> jexlContext = check(macroTaskForm, actions, output);

        if (!dryRun) {
            actions.ifPresent(MacroActions::beforeAll);
        }

        List<Pair<Command<CommandArgs>, CommandArgs>> commands = new ArrayList<>();
        for (MacroTaskCommand command : task.getCommands()) {
            Command<CommandArgs> runnable;
            try {
                runnable = (Command<CommandArgs>) ImplementationManager.build(
                        command.getCommand(),
                        () -> perContextCommands.get(command.getCommand().getKey()),
                        instance -> perContextCommands.put(command.getCommand().getKey(), instance));
            } catch (Exception e) {
                throw new JobExecutionException("Could not build " + command.getCommand().getKey(), e);
            }

            CommandArgs args;
            if (command.getArgs() == null) {
                try {
                    args = ImplementationManager.emptyArgs(command.getCommand());
                } catch (Exception e) {
                    throw new JobExecutionException("While getting empty args from " + command.getKey(), e);
                }
            } else {
                args = command.getArgs();

                jexlContext.ifPresent(ctx -> ReflectionUtils.doWithFields(
                        args.getClass(),
                        field -> {
                            if (String.class.equals(field.getType())) {
                                field.setAccessible(true);
                                Object value = field.get(args);
                                if (value instanceof String) {
                                    field.set(args, JexlUtils.evaluateTemplate((String) value, ctx));
                                }
                            }
                        },
                        field -> !field.isSynthetic()));

                Set<ConstraintViolation<Object>> violations = validator.validate(args);
                if (!violations.isEmpty()) {
                    LOG.error("While validating {}: {}", args, violations);

                    throw new JobExecutionException(
                            "While running " + command.getKey(),
                            new IllegalArgumentException(args.getClass().getName()));
                }
            }

            commands.add(Pair.of(runnable, args));
        }

        return run(commands, actions, output, dryRun);
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return task.isSaveExecs();
    }
}
