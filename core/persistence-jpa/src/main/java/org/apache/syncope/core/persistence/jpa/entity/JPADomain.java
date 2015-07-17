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
package org.apache.syncope.core.persistence.jpa.entity;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.jpa.validation.entity.DomainCheck;

@Entity
@Table(name = JPADomain.TABLE)
@DomainCheck
public class JPADomain extends AbstractEntity<String> implements Domain {

    private static final long serialVersionUID = -5891241943464285840L;

    public static final String TABLE = "SyncopeDomain";

    @Id
    private String name;

    private String adminPwd;

    @Enumerated(EnumType.STRING)
    private CipherAlgorithm adminCipherAlgorithm;

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public void setKey(final String name) {
        this.name = name;
    }

    @Override
    public String getAdminPwd() {
        return adminPwd;
    }

    @Override
    public CipherAlgorithm getAdminCipherAlgorithm() {
        return adminCipherAlgorithm;
    }

    @Override
    public void setPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        try {
            this.adminPwd = Encryptor.getInstance().encode(password, cipherAlgoritm);
            this.adminCipherAlgorithm = cipherAlgoritm;
        } catch (Exception e) {
            LOG.error("Could not encode password", e);
            this.adminPwd = null;
        }
    }

}
