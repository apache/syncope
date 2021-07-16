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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A {@link org.apache.syncope.core.provisioning.api.pushpull.PullActions} implementation which allows the ability to
 * import passwords from an LDAP backend that are hashed.
 */
public class LDAPPasswordPullActions implements PullActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPPasswordPullActions.class);

    @Autowired
    protected UserDAO userDAO;

    @Override
    public Set<String> moreAttrsToGet(final ProvisioningProfile<?, ?> profile, final Provision provision) {
        if (AnyTypeKind.USER == provision.getAnyType().getKind()) {
            return Set.of(OperationalAttributes.PASSWORD_NAME);
        }
        return PullActions.super.moreAttrsToGet(profile, provision);
    }

    private static Optional<Pair<String, CipherAlgorithm>> parseEncodedPassword(final String password) {
        if (password != null && password.startsWith("{")) {
            String digest = Optional.ofNullable(
                    password.substring(1, password.indexOf('}'))).map(String::toUpperCase).
                    orElse(null);
            int closingBracketIndex = password.indexOf('}');
            try {
                return Optional.of(
                        Pair.of(password.substring(closingBracketIndex + 1), CipherAlgorithm.valueOf(digest)));
            } catch (IllegalArgumentException e) {
                LOG.error("Cipher algorithm not allowed: {}", digest, e);
            }
        }
        return Optional.empty();
    }

    @Transactional
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final ProvisioningReport result) throws JobExecutionException {

        if (entity instanceof UserTO) {
            User user = userDAO.find(entity.getKey());
            if (user != null) {
                GuardedString passwordAttr = AttributeUtil.getPasswordValue(delta.getObject().getAttributes());
                if (passwordAttr != null) {
                    parseEncodedPassword(SecurityUtil.decrypt(passwordAttr)).ifPresent(encoded -> {
                        byte[] encodedPasswordBytes = Base64.getDecoder().decode(encoded.getLeft().getBytes());
                        String encodedHexStr = DatatypeConverter.printHexBinary(encodedPasswordBytes).toUpperCase();

                        user.setEncodedPassword(encodedHexStr, encoded.getRight());
                    });
                }
            }
        }
    }
}
