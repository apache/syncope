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
package org.apache.syncope.core.provisioning.java.data;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyTypeDataBinderImpl implements AnyTypeDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyTypeDataBinder.class);

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final EntityFactory entityFactory;

    public AnyTypeDataBinderImpl(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AccessTokenDAO accessTokenDAO,
            final EntityFactory entityFactory) {

        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public AnyType create(final AnyTypeTO anyTypeTO) {
        AnyType anyType = entityFactory.newEntity(AnyType.class);
        update(anyType, anyTypeTO);

        Set<String> added = EntitlementsHolder.getInstance().addFor(anyType.getKey());

        if (!securityProperties.getAdminUser().equals(AuthContextUtils.getUsername())) {
            AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername()).
                    orElseThrow(() -> new NotFoundException("AccessToken for " + AuthContextUtils.getUsername()));
            try {
                Set<SyncopeGrantedAuthority> authorities = new HashSet<>(POJOHelper.deserialize(
                        encryptorManager.getInstance().
                                decode(new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                        new TypeReference<Set<SyncopeGrantedAuthority>>() {
                }));

                added.forEach(e -> authorities.add(new SyncopeGrantedAuthority(e, SyncopeConstants.ROOT_REALM)));

                accessToken.setAuthorities(encryptorManager.getInstance().encode(
                        POJOHelper.serialize(authorities), CipherAlgorithm.AES).
                        getBytes());

                accessTokenDAO.save(accessToken);
            } catch (Exception e) {
                LOG.error("Could not fetch or store authorities", e);
            }
        }

        return anyType;
    }

    @Override
    public void update(final AnyType anyType, final AnyTypeTO anyTypeTO) {
        if (anyType.getKey() == null) {
            anyType.setKey(anyTypeTO.getKey());
        }
        if (anyType.getKind() == null) {
            anyType.setKind(anyTypeTO.getKind());
        }
        if (anyType.getKind() != anyTypeTO.getKind()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(AnyTypeKind.class.getSimpleName() + " cannot be changed");
            throw sce;
        }

        anyTypeTO.getClasses().forEach(anyTypeClassName -> anyTypeClassDAO.findById(anyTypeClassName).ifPresentOrElse(
                anyType::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", AnyTypeClass.class.getSimpleName(), anyTypeClassName)));
        anyType.getClasses().removeIf(c -> c == null || !anyTypeTO.getClasses().contains(c.getKey()));
    }

    @Override
    public AnyTypeTO delete(final AnyType anyType) {
        AnyTypeTO deleted = getAnyTypeTO(anyType);

        anyTypeDAO.deleteById(anyType.getKey());

        Set<String> removed = EntitlementsHolder.getInstance().removeFor(deleted.getKey());

        if (!securityProperties.getAdminUser().equals(AuthContextUtils.getUsername())) {
            AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername()).
                    orElseThrow(() -> new NotFoundException("AccessToken for " + AuthContextUtils.getUsername()));
            try {
                Set<SyncopeGrantedAuthority> authorities = new HashSet<>(POJOHelper.deserialize(
                        encryptorManager.getInstance().decode(
                                new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                        new TypeReference<Set<SyncopeGrantedAuthority>>() {
                }));

                authorities.removeAll(authorities.stream().
                        filter(authority -> removed.contains(authority.getAuthority())).toList());

                accessToken.setAuthorities(encryptorManager.getInstance().encode(
                        POJOHelper.serialize(authorities), CipherAlgorithm.AES).
                        getBytes());

                accessTokenDAO.save(accessToken);
            } catch (Exception e) {
                LOG.error("Could not fetch or store authorities", e);
            }
        }

        return deleted;
    }

    @Override
    public AnyTypeTO getAnyTypeTO(final AnyType anyType) {
        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey(anyType.getKey());
        anyTypeTO.setKind(anyType.getKind());
        anyTypeTO.getClasses().addAll(anyType.getClasses().stream().
                map(AnyTypeClass::getKey).toList());
        return anyTypeTO;
    }
}
