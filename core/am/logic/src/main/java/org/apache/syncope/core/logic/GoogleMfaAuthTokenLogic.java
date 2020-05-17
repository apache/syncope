/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.logic;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.auth.GoogleMfaAuthTokenDAO;
import org.apache.syncope.core.provisioning.api.data.GoogleMfaAuthTokenBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class GoogleMfaAuthTokenLogic extends AbstractTransactionalLogic<GoogleMfaAuthTokenTO> {
    @Autowired
    private GoogleMfaAuthTokenBinder binder;

    @Autowired
    private GoogleMfaAuthTokenDAO googleMfaAuthTokenDAO;

    @Override
    protected GoogleMfaAuthTokenTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String user = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; user == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    user = (String) args[i];
                } else if (args[i] instanceof GoogleMfaAuthTokenTO) {
                    user = ((GoogleMfaAuthTokenTO) args[i]).getUser();
                }
            }
        }

        if (user != null) {
            try {

            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean delete(final LocalDateTime expirationDate) {
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean delete(final String user, final Integer otp) {
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean delete(final String user) {
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean delete(final Integer otp) {
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public boolean deleteAll() {
        return false;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_SAVE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthTokenTO save(final GoogleMfaAuthTokenTO tokenTO) {
        return null;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthTokenTO read(final String user, final Integer otp) {
        return null;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long count(final String user) {
        return 0;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long count() {
        return 0;
    }
}
