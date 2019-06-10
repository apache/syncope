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
package org.apache.syncope.core.persistence.jpa.entity.authentication;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationProcessor;
import org.apache.syncope.core.persistence.api.entity.policy.AuthenticationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthenticationPolicy;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractAuthenticationProcessor extends AbstractGeneratedKeyEntity
        implements AuthenticationProcessor {

    private static final long serialVersionUID = -1419270763197087924L;

    @Column(unique = true, nullable = false)
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @OneToOne(optional = false)
    private JPAAuthenticationPolicy authenticationPolicy;

    public AuthenticationPolicy getAuthenticationPolicy() {
        return authenticationPolicy;
    }

    public void setAuthenticationPolicy(final AuthenticationPolicy authenticationPolicy) {
        checkType(authenticationPolicy, JPAAuthenticationPolicy.class);
        this.authenticationPolicy = (JPAAuthenticationPolicy) authenticationPolicy;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }
}
