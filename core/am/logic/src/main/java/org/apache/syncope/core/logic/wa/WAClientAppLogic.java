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
package org.apache.syncope.core.logic.wa;

import static org.apache.syncope.common.lib.types.ClientAppType.OIDCRP;
import static org.apache.syncope.common.lib.types.ClientAppType.SAML2SP;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCRP;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppBinder;

@Component
public class WAClientAppLogic {

    @Autowired
    private WAClientAppBinder binder;

    @Autowired
    private SAML2SPDAO saml2spDAO;

    @Autowired
    private OIDCRPDAO oidcrpDAO;

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WAClientApp> list() {
        List<WAClientApp> clientApps = new ArrayList<>();

        Stream.of(ClientAppType.values()).forEach(type -> {
            switch (type) {
                case OIDCRP:
                    clientApps.addAll(oidcrpDAO.findAll().stream().
                            map(binder::getWAClientApp).collect(Collectors.toList()));
                    break;

                case SAML2SP:
                default:
                    clientApps.addAll(saml2spDAO.findAll().stream().
                            map(binder::getWAClientApp).collect(Collectors.toList()));
            }
        });

        return clientApps;
    }

    private WAClientApp doRead(final Long clientAppId, final ClientAppType type) {
        WAClientApp clientApp = null;

        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.findByClientAppId(clientAppId);
                if (oidcrp != null) {
                    clientApp = binder.getWAClientApp(oidcrp);
                }
                break;

            case SAML2SP:
                SAML2SP saml2sp = saml2spDAO.findByClientAppId(clientAppId);
                if (saml2sp != null) {
                    clientApp = binder.getWAClientApp(saml2sp);
                }
                break;

            default:
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAClientApp read(final Long clientAppId, final ClientAppType type) {
        WAClientApp clientApp = null;
        if (type == null) {
            for (int i = 0; i < ClientAppType.values().length && clientApp == null; i++) {
                clientApp = doRead(clientAppId, ClientAppType.values()[i]);
            }
        } else {
            clientApp = doRead(clientAppId, type);
        }

        if (clientApp == null) {
            throw new NotFoundException(
                    "Client app with clientApp ID " + clientAppId + " and type " + type + " not found");
        }
        return clientApp;
    }

    private WAClientApp doRead(final String name, final ClientAppType type) {
        WAClientApp clientApp = null;

        switch (type) {
            case OIDCRP:
                OIDCRP oidcrp = oidcrpDAO.findByName(name);
                if (oidcrp != null) {
                    clientApp = binder.getWAClientApp(oidcrp);
                }
                break;

            case SAML2SP:
                SAML2SP saml2sp = saml2spDAO.findByName(name);
                if (saml2sp != null) {
                    clientApp = binder.getWAClientApp(saml2sp);
                }
                break;

            default:
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAClientApp read(final String name, final ClientAppType type) {
        WAClientApp clientApp = null;
        if (type == null) {
            for (int i = 0; i < ClientAppType.values().length && clientApp == null; i++) {
                clientApp = doRead(name, ClientAppType.values()[i]);
            }
        } else {
            clientApp = doRead(name, type);
        }

        if (clientApp == null) {
            throw new NotFoundException("Client app with name " + name + " with type " + type + " not found");
        }
        return clientApp;
    }
}
