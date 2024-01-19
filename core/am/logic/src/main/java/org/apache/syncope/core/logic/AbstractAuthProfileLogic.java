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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;

public abstract class AbstractAuthProfileLogic extends AbstractTransactionalLogic<AuthProfileTO> {

    protected final AuthProfileDAO authProfileDAO;

    protected final AuthProfileDataBinder binder;

    protected final EntityFactory entityFactory;

    public AbstractAuthProfileLogic(
            final AuthProfileDataBinder binder,
            final AuthProfileDAO authProfileDAO,
            final EntityFactory entityFactory) {

        this.authProfileDAO = authProfileDAO;
        this.binder = binder;
        this.entityFactory = entityFactory;
    }

    protected AuthProfile authProfile(final String owner) {
        AuthProfile profile = authProfileDAO.findByOwner(owner).orElse(null);
        if (profile == null) {
            profile = entityFactory.newEntity(AuthProfile.class);
            profile.setOwner(owner);
        }
        return profile;
    }

    @Override
    protected AuthProfileTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof AuthProfileTO authProfileTO) {
                    key = authProfileTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAuthProfileTO(authProfileDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
