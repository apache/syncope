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
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;

public abstract class AbstractAuthProfileLogic extends AbstractTransactionalLogic<AuthProfileTO> {

    protected final AuthProfileDAO authProfileDAO;

    protected final AuthProfileDataBinder binder;

    public AbstractAuthProfileLogic(final AuthProfileDAO authProfileDAO, final AuthProfileDataBinder binder) {
        this.authProfileDAO = authProfileDAO;
        this.binder = binder;
    }

    @Override
    protected AuthProfileTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AuthProfileTO) {
                    key = ((AuthProfileTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAuthProfileTO(authProfileDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
