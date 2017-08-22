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
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnyTypeDataBinderImpl implements AnyTypeDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(AnyTypeDataBinder.class);

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Resource(name = "adminUser")
    private String adminUser;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public AnyType create(final AnyTypeTO anyTypeTO) {
        AnyType anyType = entityFactory.newEntity(AnyType.class);
        update(anyType, anyTypeTO);

        Set<String> added = EntitlementsHolder.getInstance().addFor(anyType.getKey());

        if (!adminUser.equals(AuthContextUtils.getUsername())) {
            AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
            try {
                Set<SyncopeGrantedAuthority> authorities = new HashSet<>(POJOHelper.deserialize(
                        ENCRYPTOR.decode(new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                        new TypeReference<Set<SyncopeGrantedAuthority>>() {
                }));

                added.forEach(entitlement -> {
                    authorities.add(new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM));
                });

                accessToken.setAuthorities(ENCRYPTOR.encode(
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

        anyType.getClasses().clear();
        anyTypeTO.getClasses().forEach(anyTypeClassName -> {
            AnyTypeClass anyTypeClass = anyTypeClassDAO.find(anyTypeClassName);
            if (anyTypeClass == null) {
                LOG.debug("Invalid " + AnyTypeClass.class.getSimpleName() + "{}, ignoring...", anyTypeClassName);
            } else {
                anyType.add(anyTypeClass);
            }
        });
    }

    @Override
    public AnyTypeTO delete(final AnyType anyType) {
        AnyTypeTO deleted = getAnyTypeTO(anyType);

        anyTypeDAO.delete(anyType.getKey());

        final Set<String> removed = EntitlementsHolder.getInstance().removeFor(deleted.getKey());

        if (!adminUser.equals(AuthContextUtils.getUsername())) {
            AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
            try {
                Set<SyncopeGrantedAuthority> authorities = new HashSet<>(POJOHelper.deserialize(
                        ENCRYPTOR.decode(new String(accessToken.getAuthorities()), CipherAlgorithm.AES),
                        new TypeReference<Set<SyncopeGrantedAuthority>>() {
                }));

                authorities.removeAll(authorities.stream().
                        filter(authority -> removed.contains(authority.getAuthority())).collect(Collectors.toList()));

                accessToken.setAuthorities(ENCRYPTOR.encode(
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
        anyType.getClasses().forEach(anyTypeClass -> {
            anyTypeTO.getClasses().add(anyTypeClass.getKey());
        });

        return anyTypeTO;
    }

}
