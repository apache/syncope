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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
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
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ClientAppLogic extends AbstractTransactionalLogic<ClientAppTO> {

    protected final ServiceOps serviceOps;

    protected final ClientAppUtilsFactory clientAppUtilsFactory;

    protected final ClientAppDataBinder binder;

    protected final CASSPClientAppDAO casSPClientAppDAO;

    protected final OIDCRPClientAppDAO oidcRPClientAppDAO;

    protected final SAML2SPClientAppDAO saml2SPClientAppDAO;

    protected final SecurityProperties securityProperties;

    public ClientAppLogic(
            final ServiceOps serviceOps,
            final ClientAppUtilsFactory clientAppUtilsFactory,
            final ClientAppDataBinder binder,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO,
            final SecurityProperties securityProperties) {

        this.serviceOps = serviceOps;
        this.clientAppUtilsFactory = clientAppUtilsFactory;
        this.binder = binder;
        this.casSPClientAppDAO = casSPClientAppDAO;
        this.oidcRPClientAppDAO = oidcRPClientAppDAO;
        this.saml2SPClientAppDAO = saml2SPClientAppDAO;
        this.securityProperties = securityProperties;
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

        return stream.collect(Collectors.toList());
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
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.find(key);
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }

                checkType(type, clientAppUtilsFactory.getInstance(oidcrp));

                return binder.getClientAppTO(oidcrp);
            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.find(key);
                if (cassp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }

                checkType(type, clientAppUtilsFactory.getInstance(cassp));

                return binder.getClientAppTO(cassp);
            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.find(key);
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
                return binder.getClientAppTO(oidcRPClientAppDAO.save(binder.create(clientAppTO)));
            case CASSP:
                return binder.getClientAppTO(casSPClientAppDAO.save(binder.create(clientAppTO)));
            case SAML2SP:
            default:
                return binder.getClientAppTO(saml2SPClientAppDAO.save(binder.create(clientAppTO)));
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_UPDATE + "')")
    public void update(final ClientAppType type, final ClientAppTO clientAppTO) {
        checkType(type, clientAppUtilsFactory.getInstance(clientAppTO));

        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.find(clientAppTO.getKey());
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + clientAppTO.getKey() + " not found");
                }
                binder.update(oidcrp, clientAppTO);
                oidcRPClientAppDAO.save(oidcrp);
                break;
            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.find(clientAppTO.getKey());
                if (cassp == null) {
                    throw new NotFoundException("Client app " + clientAppTO.getKey() + " not found");
                }
                binder.update(cassp, clientAppTO);
                casSPClientAppDAO.save(cassp);
                break;
            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.find(clientAppTO.getKey());
                if (saml2sp == null) {
                    throw new NotFoundException("Client app " + clientAppTO.getKey() + " not found");
                }
                binder.update(saml2sp, clientAppTO);
                saml2SPClientAppDAO.save(saml2sp);
        }
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_DELETE + "')")
    public void delete(final ClientAppType type, final String key) {
        switch (type) {
            case OIDCRP:
                OIDCRPClientApp oidcrp = oidcRPClientAppDAO.find(key);
                if (oidcrp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }
                oidcRPClientAppDAO.delete(oidcrp);
                break;
            case CASSP:
                CASSPClientApp cassp = casSPClientAppDAO.find(key);
                if (cassp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }
                casSPClientAppDAO.delete(cassp);
                break;
            case SAML2SP:
            default:
                SAML2SPClientApp saml2sp = saml2SPClientAppDAO.find(key);
                if (saml2sp == null) {
                    throw new NotFoundException("Client app " + key + " not found");
                }
                saml2SPClientAppDAO.delete(saml2sp);
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
                ClientApp clientApp = saml2SPClientAppDAO.find(key);
                if (clientApp == null) {
                    clientApp = oidcRPClientAppDAO.find(key);
                }

                return binder.getClientAppTO(clientApp);
            } catch (Throwable ex) {
                LOG.debug("Unresolved reference", ex);
                throw new UnresolvedReferenceException(ex);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.CLIENTAPP_PUSH + "')")
    public void pushToWA() {
        try {
            NetworkService wa = serviceOps.get(NetworkService.Type.WA);
            String basicAuthHeader = DefaultBasicAuthSupplier.getBasicAuthHeader(
                    securityProperties.getAnonymousUser(), securityProperties.getAnonymousKey());
            URI endpoint = URI.create(StringUtils.appendIfMissing(wa.getAddress(), "/")
                    + "actuator/registeredServices");
            HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder(endpoint).
                            header(HttpHeaders.AUTHORIZATION, basicAuthHeader).
                            header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).
                            GET().build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (KeymasterException e) {
            throw new NotFoundException("Could not find any WA instance", e);
        } catch (IOException | InterruptedException e) {
            throw new InternalServerErrorException("Errors while communicating with WA instance", e);
        }
    }
}
