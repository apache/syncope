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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.client.ClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.ClientApp;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtils;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClientAppLogic extends AbstractTransactionalLogic<ClientAppTO> {

    @Autowired
    private ClientAppUtilsFactory clientAppUtilsFactory;

    @Autowired
    private ClientAppDataBinder binder;

    @Autowired
    private SAML2SPDAO saml2spDAO;

    @Autowired
    private OIDCRPDAO oidcrpDAO;

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_LIST + "')")
    public <T extends ClientAppTO> List<T> list(final ClientAppType type) {
        Stream<T> stream;

        switch (type) {
            case OIDCRP:
                stream = oidcrpDAO.findAll().stream().map(binder::getClientAppTO);
                break;

            case SAML2SP:
            default:
                stream = saml2spDAO.findAll().stream().map(binder::getClientAppTO);
        }

        return stream.collect(Collectors.toList());
    }

    private void checkType(final ClientAppType type, final ClientAppUtils clientAppUtils) {
        if (clientAppUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + clientAppUtils.getType());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_READ + "')")
    @Transactional(readOnly = true)
    public <T extends ClientAppTO> T read(final ClientAppType type, final String key) {
        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.find(key);
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }

                checkType(type, clientAppUtilsFactory.getInstance(oidcrp));

                return binder.getClientAppTO(oidcrp);

            case SAML2SP:
            default:
                SAML2SP saml2sp = saml2spDAO.find(key);
                if (saml2sp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }

                checkType(type, clientAppUtilsFactory.getInstance(saml2sp));

                return binder.getClientAppTO(saml2sp);
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_CREATE + "')")
    public ClientAppTO create(final ClientAppType type, final ClientAppTO clientAppTO) {
        checkType(type, clientAppUtilsFactory.getInstance(clientAppTO));

        switch (type) {
            case OIDCRP:
                return binder.getClientAppTO(oidcrpDAO.save(binder.create(clientAppTO)));

            case SAML2SP:
            default:
                return binder.getClientAppTO(saml2spDAO.save(binder.create(clientAppTO)));
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_UPDATE + "')")
    public void update(final ClientAppType type, final ClientAppTO clientAppTO) {
        checkType(type, clientAppUtilsFactory.getInstance(clientAppTO));

        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.find(clientAppTO.getKey());
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + clientAppTO.getKey() + " not found");
                }
                binder.update(oidcrp, clientAppTO);
                oidcrpDAO.save(oidcrp);
                break;

            case SAML2SP:
            default:
                SAML2SP saml2sp = saml2spDAO.find(clientAppTO.getKey());
                if (saml2sp == null) {
                    throw new NotFoundException("Client app " + clientAppTO.getKey() + " not found");
                }
                binder.update(saml2sp, clientAppTO);
                saml2spDAO.save(saml2sp);
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_DELETE + "')")
    public void delete(final ClientAppType type, final String key) {
        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.find(key);
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }
                oidcrpDAO.delete(oidcrp);
                break;

            case SAML2SP:
            default:
                SAML2SP saml2sp = saml2spDAO.find(key);
                if (saml2sp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }
                saml2spDAO.delete(saml2sp);
        }
    }

    @Override
    protected ClientAppTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ClientAppTO) {
                    key = ((ClientAppTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                ClientApp clientApp = saml2spDAO.find(key);
                if (clientApp == null) {
                    clientApp = oidcrpDAO.find(key);
                }

                return binder.getClientAppTO(clientApp);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
