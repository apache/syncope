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
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.GoogleMfaAuthTokenDAO;
import org.apache.syncope.core.persistence.api.entity.auth.GoogleMfaAuthToken;
import org.apache.syncope.core.provisioning.api.data.GoogleMfaAuthTokenDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoogleMfaAuthTokenLogic extends AbstractTransactionalLogic<GoogleMfaAuthTokenTO> {
    @Autowired
    private GoogleMfaAuthTokenDataBinder binder;

    @Autowired
    private GoogleMfaAuthTokenDAO googleMfaAuthTokenDAO;

    @Override
    protected GoogleMfaAuthTokenTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String owner = null;
        Integer token = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; owner == null && token == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    owner = (String) args[i];
                } else if (args[i] instanceof Integer) {
                    token = (Integer) args[i];
                } else if (args[i] instanceof GoogleMfaAuthTokenTO) {
                    owner = ((GoogleMfaAuthTokenTO) args[i]).getOwner();
                    token = ((GoogleMfaAuthTokenTO) args[i]).getToken();
                }
            }
        }

        if (owner != null && token != null) {
            try {
                return binder.getGoogleMfaAuthTokenTO(googleMfaAuthTokenDAO.find(owner, token));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }
        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Date expirationDate) {
        googleMfaAuthTokenDAO.delete(expirationDate);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner, final Integer otp) {
        googleMfaAuthTokenDAO.delete(owner, otp);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final String owner) {
        googleMfaAuthTokenDAO.delete(owner);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void delete(final Integer otp) {
        googleMfaAuthTokenDAO.delete(otp);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_DELETE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public void deleteAll() {
        googleMfaAuthTokenDAO.deleteAll();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_SAVE_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public GoogleMfaAuthTokenTO save(final GoogleMfaAuthTokenTO tokenTO) {
        return binder.getGoogleMfaAuthTokenTO(googleMfaAuthTokenDAO.save(binder.create(tokenTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthTokenTO read(final String owner, final Integer otp) {
        final GoogleMfaAuthToken tokenTO = googleMfaAuthTokenDAO.find(owner, otp);
        if (tokenTO == null) {
            throw new NotFoundException("Google MFA Token for " + owner + " and " + otp + " not found");
        }

        return binder.getGoogleMfaAuthTokenTO(tokenTO);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public GoogleMfaAuthTokenTO read(final String key) {
        final GoogleMfaAuthToken token = googleMfaAuthTokenDAO.find(key);
        if (token == null) {
            throw new NotFoundException("Google MFA Token for " + key + " not found");
        }
        return binder.getGoogleMfaAuthTokenTO(token);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long countTokensFor(final String owner) {
        return googleMfaAuthTokenDAO.count(owner);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_COUNT_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public long countAll() {
        return googleMfaAuthTokenDAO.count();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GOOGLE_MFA_READ_TOKEN + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<GoogleMfaAuthTokenTO> findTokensFor(final String owner) {
        final List<GoogleMfaAuthToken> tokens = googleMfaAuthTokenDAO.findForOwner(owner);
        return tokens.stream().map(binder::getGoogleMfaAuthTokenTO).collect(Collectors.toList());
    }
}
