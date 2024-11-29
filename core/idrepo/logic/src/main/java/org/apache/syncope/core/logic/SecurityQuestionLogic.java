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

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SecurityQuestionLogic extends AbstractTransactionalLogic<SecurityQuestionTO> {

    protected final SecurityQuestionDAO securityQuestionDAO;

    protected final UserDAO userDAO;

    protected final SecurityQuestionDataBinder binder;

    public SecurityQuestionLogic(
            final SecurityQuestionDAO securityQuestionDAO,
            final UserDAO userDAO,
            final SecurityQuestionDataBinder binder) {

        this.securityQuestionDAO = securityQuestionDAO;
        this.userDAO = userDAO;
        this.binder = binder;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<SecurityQuestionTO> list() {
        return securityQuestionDAO.findAll().stream().map(binder::getSecurityQuestionTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SECURITY_QUESTION_READ + "')")
    @Transactional(readOnly = true)
    public SecurityQuestionTO read(final String key) {
        SecurityQuestion securityQuestion = securityQuestionDAO.findById(key).
                orElseThrow(() -> new NotFoundException("SecurityQuestion " + key));

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SECURITY_QUESTION_CREATE + "')")
    public SecurityQuestionTO create(final SecurityQuestionTO securityQuestionTO) {
        return binder.getSecurityQuestionTO(securityQuestionDAO.save(binder.create(securityQuestionTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SECURITY_QUESTION_UPDATE + "')")
    public SecurityQuestionTO update(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestion securityQuestion = securityQuestionDAO.findById(securityQuestionTO.getKey()).
                orElseThrow(() -> new NotFoundException("SecurityQuestion " + securityQuestionTO.getKey()));

        binder.update(securityQuestion, securityQuestionTO);
        securityQuestion = securityQuestionDAO.save(securityQuestion);

        return binder.getSecurityQuestionTO(securityQuestion);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.SECURITY_QUESTION_DELETE + "')")
    public SecurityQuestionTO delete(final String key) {
        SecurityQuestion securityQuestion = securityQuestionDAO.findById(key).
                orElseThrow(() -> new NotFoundException("SecurityQuestion " + key));

        SecurityQuestionTO deleted = binder.getSecurityQuestionTO(securityQuestion);
        securityQuestionDAO.deleteById(key);
        return deleted;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SecurityQuestionTO readByUser(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }
        User user = userDAO.findByUsername(username).
                orElseThrow(() -> new NotFoundException("User " + username));

        if (user.getSecurityQuestion() == null) {
            LOG.error("Could not find security question for user '{}'", username);

            throw new NotFoundException("Security question for user " + username);
        }

        return binder.getSecurityQuestionTO(user.getSecurityQuestion());
    }

    @Override
    protected SecurityQuestionTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof SecurityQuestionTO securityQuestionTO) {
                    key = securityQuestionTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getSecurityQuestionTO(securityQuestionDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
