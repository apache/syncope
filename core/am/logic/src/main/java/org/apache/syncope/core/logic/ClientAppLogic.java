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
import java.util.Set;
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
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
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

        return stream.filter(clientApp -> {
            if (clientApp.getRealm() == null) {
                return true;
            }

            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(AMEntitlement.CLIENTAPP_LIST),
                    clientApp.getRealm());
            return RealmUtils.SubtreePredicate.of(authRealms).test(clientApp.getRealm());
        }).toList();
    }

    protected void typeCheck(final ClientAppType type, final ClientAppUtils clientAppUtils) {
        if (clientAppUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + clientAppUtils.getType());
            throw sce;
        }
    }

    protected void securityChecks(
            final Set<String> realms,
            final String realm,
            final ClientAppType type,
            final String key) {

        if (!RealmUtils.SubtreePredicate.of(realms).test(realm)) {
            throw new DelegatedAdministrationException(realm, type.name(), key);
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_READ + "')")
    @Transactional(readOnly = true)
    public <T extends ClientAppTO> T read(final ClientAppType type, final String key) {
        T clientApp;
        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("OIDCRPClientApp " + key));

                typeCheck(type, clientAppUtilsFactory.getInstance(oidcrp));

                clientApp = binder.getClientAppTO(oidcrp);
                break;

            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("CASSPClientApp " + key));

                typeCheck(type, clientAppUtilsFactory.getInstance(cassp));

                clientApp = binder.getClientAppTO(cassp);
                break;

            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.findById(key).
                        orElseThrow(() -> new NotFoundException("SAML2SPClientApp " + key));

                typeCheck(type, clientAppUtilsFactory.getInstance(saml2sp));

                clientApp = binder.getClientAppTO(saml2sp);
        }

        if (clientApp.getRealm() != null) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(AMEntitlement.CLIENTAPP_READ),
                    clientApp.getRealm());
            securityChecks(authRealms, clientApp.getRealm(), type, key);
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_CREATE + "')")
    public <T extends ClientAppTO> T create(final ClientAppType type, final ClientAppTO clientAppTO) {
        typeCheck(type, clientAppUtilsFactory.getInstance(clientAppTO));

        T clientApp;
        switch (type) {
            case OIDCRP:
                clientApp = binder.getClientAppTO(oidcRPClientAppDAO.save(binder.create(clientAppTO)));
                break;

            case CASSP:
                clientApp = binder.getClientAppTO(casSPClientAppDAO.save(binder.create(clientAppTO)));
                break;

            case SAML2SP:
            default:
                clientApp = binder.getClientAppTO(saml2SPClientAppDAO.save(binder.create(clientAppTO)));
        }

        if (clientApp.getRealm() != null) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(AMEntitlement.CLIENTAPP_CREATE),
                    clientApp.getRealm());
            securityChecks(authRealms, clientApp.getRealm(), type, null);
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_UPDATE + "')")
    public <T extends ClientAppTO> T update(final ClientAppType type, final ClientAppTO clientAppTO) {
        typeCheck(type, clientAppUtilsFactory.getInstance(clientAppTO));

        T clientApp;
        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("OIDCRPClientApp " + clientAppTO.getKey()));

                binder.update(oidcrp, clientAppTO);
                clientApp = binder.getClientAppTO(oidcRPClientAppDAO.save(oidcrp));
                break;

            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("CASSPClientApp " + clientAppTO.getKey()));

                binder.update(cassp, clientAppTO);
                clientApp = binder.getClientAppTO(casSPClientAppDAO.save(cassp));
                break;

            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.findById(clientAppTO.getKey()).
                        orElseThrow(() -> new NotFoundException("SAML2SPClientApp " + clientAppTO.getKey()));

                binder.update(saml2sp, clientAppTO);
                clientApp = binder.getClientAppTO(saml2SPClientAppDAO.save(saml2sp));
        }

        if (clientApp.getRealm() != null) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(AMEntitlement.CLIENTAPP_UPDATE),
                    clientApp.getRealm());
            securityChecks(authRealms, clientApp.getRealm(), type, null);
        }

        return clientApp;
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

        if (deleted.getRealm() != null) {
            Set<String> authRealms = RealmUtils.getEffective(
                    AuthContextUtils.getAuthorizations().get(AMEntitlement.CLIENTAPP_DELETE),
                    deleted.getRealm());
            securityChecks(authRealms, deleted.getRealm(), type, null);
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
