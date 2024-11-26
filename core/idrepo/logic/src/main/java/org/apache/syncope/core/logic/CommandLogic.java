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
package org.apache.syncope.core.logic;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.macro.Command;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class CommandLogic extends AbstractLogic<EntityTO> {

    protected final ImplementationDAO implementationDAO;

    protected final Validator validator;

    protected final Map<String, Command<?>> perContextCommands = new ConcurrentHashMap<>();

    public CommandLogic(final ImplementationDAO implementationDAO, final Validator validator) {
        this.implementationDAO = implementationDAO;
        this.validator = validator;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_LIST + "')")
    @Transactional(readOnly = true)
    public Page<CommandTO> search(final String keyword, final Pageable pageable) {
        List<Implementation> result = implementationDAO.findByTypeAndKeyword(IdRepoImplementationType.COMMAND, keyword);

        long count = result.size();

        List<CommandTO> commands = result.stream().
                skip(pageable.getPageSize() * pageable.getPageNumber()).
                limit(pageable.getPageSize()).
                map(command -> {
                    try {
                        return new CommandTO.Builder(command.getKey()).
                                args(ImplementationManager.emptyArgs(command)).build();
                    } catch (Exception e) {
                        LOG.error("Could not get arg class for {}", command, e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                toList();

        return new SyncopePage<>(commands, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.IMPLEMENTATION_READ + "')")
    @Transactional(readOnly = true)
    public CommandTO read(final String key) {
        Implementation impl = implementationDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Implementation " + key));

        try {
            return new CommandTO.Builder(impl.getKey()).
                    args(ImplementationManager.emptyArgs(impl)).build();
        } catch (Exception e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);
            sce.getElements().add("Could not build " + impl.getKey());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.COMMAND_RUN + "')")
    @SuppressWarnings("unchecked")
    public String run(final CommandTO command) {
        Implementation impl = implementationDAO.findById(command.getKey()).
                orElseThrow(() -> new NotFoundException("Implementation " + command.getKey()));

        Command<CommandArgs> runnable;
        try {
            runnable = (Command<CommandArgs>) ImplementationManager.build(
                    impl,
                    () -> perContextCommands.get(impl.getKey()),
                    instance -> perContextCommands.put(impl.getKey(), instance));
        } catch (Exception e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);
            sce.getElements().add("Could not build " + impl.getKey());
            throw sce;
        }

        if (command.getArgs() != null) {
            try {
                Set<ConstraintViolation<Object>> violations = validator.validate(command.getArgs());
                if (!violations.isEmpty()) {
                    throw new InvalidEntityException(command.getArgs().getClass().getName(), violations);
                }
            } catch (ValidationException e) {
                LOG.error("While validating {}", command.getArgs(), e);

                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidValues);
                sce.getElements().add(e.getMessage());
                throw sce;
            }
        }

        try {
            return runnable.run(command.getArgs() == null ? ImplementationManager.emptyArgs(impl) : command.getArgs());
        } catch (Exception e) {
            LOG.error("While running {} on {}", command.getKey(), command.getArgs(), e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RunError);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) {

        throw new UnsupportedOperationException();
    }
}
