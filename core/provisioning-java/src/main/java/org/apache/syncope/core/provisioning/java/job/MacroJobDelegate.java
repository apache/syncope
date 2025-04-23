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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.form.FormProperty;
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
import org.springframework.aop.support.AopUtils;
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
    protected VirtualThreadPoolTaskExecutor taskExecutor;

    protected final Map<String, MacroActions> perContextActions = new ConcurrentHashMap<>();

    protected final Map<String, Command<?>> perContextCommands = new ConcurrentHashMap<>();

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
                fpd.getName(),
                macroTaskForm.getProperty(fpd.getName()).map(p -> p.getValue() != null))).
                filter(pair -> pair.getRight().isEmpty()).
                map(Pair::getLeft).
                collect(Collectors.toSet());
        if (!missingFormProperties.isEmpty()) {
            throw new JobExecutionException("Required form properties missing: " + missingFormProperties);
        }

        // build the JEXL context where variables are mapped to property values, built according to the defined type
        Map<String, Object> vars = new HashMap<>();
        for (FormPropertyDef fpd : task.getFormPropertyDefs()) {
            String value = macroTaskForm.getProperty(fpd.getName()).map(FormProperty::getValue).orElse(null);
            if (value == null) {
                continue;
            }

            switch (fpd.getType()) {
                case String -> {
                    if (Optional.ofNullable(fpd.getStringRegEx()).
                            map(pattern -> !pattern.matcher(value).matches()).
                            orElse(false)) {

                        throw new JobExecutionException("RegEx not matching for " + fpd.getName() + ": " + value);
                    }

                    vars.put(fpd.getName(), value);
                }

                case Password ->
                    vars.put(fpd.getName(), value);

                case Boolean ->
                    vars.put(fpd.getName(), BooleanUtils.toBoolean(value));

                case Date -> {
                    try {
                        vars.put(fpd.getName(), StringUtils.isBlank(fpd.getDatePattern())
                                ? FormatUtils.parseDate(value)
                                : FormatUtils.parseDate(value, fpd.getDatePattern()));
                    } catch (DateTimeParseException e) {
                        throw new JobExecutionException("Unparseable date " + fpd.getName() + ": " + value, e);
                    }
                }

                case Long ->
                    vars.put(fpd.getName(), NumberUtils.toLong(value));

                case Enum -> {
                    if (!fpd.getEnumValues().containsKey(value)) {
                        throw new JobExecutionException("Not allowed for " + fpd.getName() + ": " + value);
                    }

                    vars.put(fpd.getName(), value);
                }

                case Dropdown -> {
                    if (!fpd.isDropdownFreeForm()) {
                        List<String> values = fpd.isDropdownSingleSelection()
                                ? List.of(value)
                                : List.of(value.split(";"));

                        if (!actions.map(a -> a.getDropdownValues(fpd.getName()).keySet()).
                                orElseGet(Set::of).containsAll(values)) {

                            throw new JobExecutionException("Not allowed for " + fpd.getName() + ": " + values);
                        }
                    }

                    vars.put(fpd.getName(), value);
                }

                default -> {
                }
            }
        }

        // if validator is defined, validate the provided form
        try {
            actions.ifPresent(a -> a.validate(macroTaskForm, vars));
        } catch (ValidationException e) {
            throw new JobExecutionException("Invalid form submitted for task " + task.getKey(), e);
        }

        output.append("Form parameter values: ").append(vars).append("\n\n");

        return vars.isEmpty() ? Optional.empty() : Optional.of(new MapContext(vars));
    }

    protected String run(
            final List<Pair<Command<CommandArgs>, CommandArgs>> commands,
            final Optional<MacroActions> actions,
            final StringBuilder output,
            final boolean dryRun)
            throws JobExecutionException {

        Future<Mutable<Pair<String, Throwable>>> future = taskExecutor.submit(
                new DelegatingSecurityContextCallable<>(() -> {

                    Mutable<Pair<String, Throwable>> error = new MutableObject<>();

                    for (int i = 0; i < commands.size() && error.getValue() == null; i++) {
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
                        } catch (Throwable t) {
                            if (task.isContinueOnError()) {
                                output.append("Continuing on error: <").append(t.getMessage()).append('>');

                                LOG.error("While running {} with args {}, continuing on error",
                                        command.getLeft().getClass().getName(), command.getRight(), t);
                            } else {
                                error.setValue(Pair.of(AopUtils.getTargetClass(command.getLeft()).getName(), t));
                            }
                        }
                        output.append("\n\n");
                    }

                    return error;
                }));

        try {
            Mutable<Pair<String, Throwable>> error = future.get();
            if (error.getValue() != null) {
                throw new JobExecutionException("While running "
                        + error.getValue().getLeft(), error.getValue().getRight());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new JobExecutionException("While waiting for macro commands completion", e);
        }

        output.append("COMPLETED");

        return actions.filter(a -> !dryRun).map(a -> a.afterAll(output)).orElse(output).toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
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

        if (!context.isDryRun()) {
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
                                if (value instanceof final String s) {
                                    field.set(args, JexlUtils.evaluateTemplate(s, ctx));
                                }
                            }
                        },
                        field -> !field.isSynthetic()));

                Set<ConstraintViolation<Object>> violations = validator.validate(args);
                if (!violations.isEmpty()) {
                    LOG.error("While validating {}: {}", args, violations);

                    throw new JobExecutionException(
                            "While running " + command.getKey(),
                            new IllegalArgumentException(violations.stream().
                                    map(v -> v.getPropertyPath() + ": " + v.getMessage()).
                                    collect(Collectors.joining(","))));
                }
            }

            commands.add(Pair.of(runnable, args));
        }

        return run(commands, actions, output, context.isDryRun());
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return task.isSaveExecs();
    }
}
