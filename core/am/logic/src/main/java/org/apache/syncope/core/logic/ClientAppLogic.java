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
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtils;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ClientAppLogic extends AbstractTransactionalLogic<ClientAppTO> {

    protected final ServiceOps serviceOps;

    protected final ClientAppUtilsFactory clientAppUtilsFactory;

    protected final ClientAppDataBinder binder;

    protected final CASSPClientAppDAO casSPClientAppDAO;

    protected final OIDCRPClientAppDAO oidcRPClientAppDAO;

    protected final SAML2SPClientAppDAO saml2SPClientAppDAO;

    public ClientAppLogic(
            final ServiceOps serviceOps,
            final ClientAppUtilsFactory clientAppUtilsFactory,
            final ClientAppDataBinder binder,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO) {

        this.serviceOps = serviceOps;
        this.clientAppUtilsFactory = clientAppUtilsFactory;
        this.binder = binder;
        this.casSPClientAppDAO = casSPClientAppDAO;
        this.oidcRPClientAppDAO = oidcRPClientAppDAO;
        this.saml2SPClientAppDAO = saml2SPClientAppDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_LIST + "')")
    public <T extends ClientAppTO> List<T> list(final ClientAppType type) {
        Stream<T> stream;

        switch (type) {
            case OIDCRP:
                stream = oidcRPClientAppDAO.findAll().stream().map(binder::getClientAppTO);
                break;
            case CASSP:
                stream = casSPClientAppDAO.findAll().stream().map(binder::getClientAppTO);
                break;
            case SAML2SP:
            default:
                stream = saml2SPClientAppDAO.findAll().stream().map(binder::getClientAppTO);
        }

        return stream.toList();
    }

    protected void checkType(final ClientAppType type, final ClientAppUtils clientAppUtils) {
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
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("OIDCRPClientApp " + key));

                checkType(type, clientAppUtilsFactory.getInstance(oidcrp));

                return binder.getClientAppTO(oidcrp);

            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("CASSPClientApp " + key));

                checkType(type, clientAppUtilsFactory.getInstance(cassp));

                return binder.getClientAppTO(cassp);

            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("SAML2SPClientApp " + key));

                checkType(type, clientAppUtilsFactory.getInstance(saml2sp));

                return binder.getClientAppTO(saml2sp);
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_CREATE + "')")
    public <T extends ClientAppTO> T create(final ClientAppType type, final ClientAppTO clientAppTO) {
        checkType(type, clientAppUtilsFactory.getInstance(clientAppTO));

        switch (type) {
            case OIDCRP:
                return binder.getClientAppTO(oidcRPClientAppDAO.save(binder.create(clientAppTO)));
            case CASSP:
                return binder.getClientAppTO(casSPClientAppDAO.save(binder.create(clientAppTO)));
            case SAML2SP:
            default:
                return binder.getClientAppTO(saml2SPClientAppDAO.save(binder.create(clientAppTO)));
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_UPDATE + "')")
    public <T extends ClientAppTO> T update(final ClientAppType type, final ClientAppTO clientAppTO) {
        checkType(type, clientAppUtilsFactory.getInstance(clientAppTO));

        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("OIDCRPClientApp " + clientAppTO.getKey()));

                binder.update(oidcrp, clientAppTO);
                return binder.getClientAppTO(oidcRPClientAppDAO.save(oidcrp));

            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("CASSPClientApp " + clientAppTO.getKey()));

                binder.update(cassp, clientAppTO);
                return binder.getClientAppTO(casSPClientAppDAO.save(cassp));

            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("SAML2SPClientApp " + clientAppTO.getKey()));

                binder.update(saml2sp, clientAppTO);
                return binder.getClientAppTO(saml2SPClientAppDAO.save(saml2sp));
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_DELETE + "')")
    public <T extends ClientAppTO> T delete(final ClientAppType type, final String key) {
        final T deleted;
        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("OIDCRPClientApp " + key));

                oidcRPClientAppDAO.delete(oidcrp);
                deleted = binder.getClientAppTO(oidcrp);
                break;

            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("CASSPClientApp " + key));

                casSPClientAppDAO.delete(cassp);
                deleted = binder.getClientAppTO(cassp);
                break;

            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("SAML2SPClientApp " + key));

                saml2SPClientAppDAO.delete(saml2sp);
                deleted = binder.getClientAppTO(saml2sp);
        }

        return deleted;
    }

    @Override
    protected ClientAppTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (ArrayUtils.isEmpty(args) || args.length != 2) {
            throw new UnresolvedReferenceException();
        }

        try {
            final String key;
            final ClientAppType type;

            if (args[0] instanceof ClientAppType clientAppType) {
                type = clientAppType;
            } else {
                throw new RuntimeException("Invalid ClientApp type");
            }

            if (args[1] instanceof String string) {
                key = string;
            } else if (args[1] instanceof ClientAppTO clientAppTO) {
                key = clientAppTO.getKey();
            } else {
                throw new RuntimeException("Invalid ClientApp key");
            }

            final ClientApp clientApp;
            switch (type) {
                case CASSP:
                    clientApp = casSPClientAppDAO.findById(key).orElseThrow();
                    break;
                case SAML2SP:
                    clientApp = saml2SPClientAppDAO.findById(key).orElseThrow();
                    break;
                case OIDCRP:
                    clientApp = oidcRPClientAppDAO.findById(key).orElseThrow();
                    break;
                default:
                    throw new RuntimeException("Unexpected ClientApp type");
            }

            return binder.getClientAppTO(clientApp);
        } catch (Throwable t) {
            throw new UnresolvedReferenceException();
        }
    }
}
