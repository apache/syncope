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
package org.apache.syncope.core.persistence.jpa.entity.policy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPostProcessor;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPreProcessor;
import org.apache.syncope.core.persistence.api.entity.policy.AuthenticationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.authentication.JPAAuthenticationPostProcessor;
import org.apache.syncope.core.persistence.jpa.entity.authentication.JPAAuthenticationPreProcessor;

@Entity
@Table(name = JPAAuthenticationPolicy.TABLE)
public class JPAAuthenticationPolicy extends AbstractPolicy implements AuthenticationPolicy {

    private static final long serialVersionUID = -4190607009908888884L;

    public static final String TABLE = "AuthenticationPolicy";

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Conf",
            joinColumns =
            @JoinColumn(name = "authentication_policy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "authentication_policy_id", "implementation_id" }))
    private List<JPAImplementation> configurations = new ArrayList<>();

    private int maxAuthenticationAttempts;

    private int authenticationAttemptsInterval;

    private int authenticationFailureLockoutDuration;

    private String lockoutAttributeName;

    private String lockoutAttributeValue;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy =
            "authenticationPolicy")
    private JPAAuthenticationPostProcessor authenticationPostProcessor;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy =
            "authenticationPolicy")
    private JPAAuthenticationPreProcessor authenticationPreProcessor;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getMaxAuthenticationAttempts() {
        return maxAuthenticationAttempts;
    }

    @Override
    public int getAuthenticationAttemptsInterval() {
        return authenticationAttemptsInterval;
    }

    @Override
    public int getAuthenticationFailureLockoutDuration() {
        return authenticationFailureLockoutDuration;
    }

    @Override
    public String getLockoutAttributeName() {
        return lockoutAttributeName;
    }

    @Override
    public String getLockoutAttributeValue() {
        return lockoutAttributeValue;
    }

    @Override
    public AuthenticationPostProcessor getAuthenticationPostProcessor() {
        return authenticationPostProcessor;
    }

    @Override
    public AuthenticationPreProcessor getAuthenticationPreProcessor() {
        return authenticationPreProcessor;
    }

    @Override
    public List<? extends Implementation> getConfigurations() {
        return configurations;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setMaxAuthenticationAttempts(final int maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    @Override
    public void setAuthenticationAttemptsInterval(final int authenticationAttemptsInterval) {
        this.authenticationAttemptsInterval = authenticationAttemptsInterval;
    }

    @Override
    public void setAuthenticationFailureLockoutDuration(final int authenticationFailureLockoutDuration) {
        this.authenticationFailureLockoutDuration = authenticationFailureLockoutDuration;
    }

    @Override
    public void setLockoutAttributeName(final String lockoutAttributeName) {
        this.lockoutAttributeName = lockoutAttributeName;
    }

    @Override
    public void setLockoutAttributeValue(final String lockoutAttributeValue) {
        this.lockoutAttributeValue = lockoutAttributeValue;
    }

    @Override
    public void setAuthenticationPostProcessor(final AuthenticationPostProcessor authenticationPostProcessor) {
        checkType(authenticationPostProcessor, JPAAuthenticationPostProcessor.class);
        this.authenticationPostProcessor = (JPAAuthenticationPostProcessor) authenticationPostProcessor;
    }

    @Override
    public void setAuthenticationPreProcessor(final AuthenticationPreProcessor authenticationPreProcessor) {
        checkType(authenticationPreProcessor, JPAAuthenticationPreProcessor.class);
        this.authenticationPreProcessor = (JPAAuthenticationPreProcessor) authenticationPreProcessor;
    }

    @Override
    public boolean addConfiguration(final Implementation configuration) {
        checkType(configuration, JPAImplementation.class);
        checkImplementationType(configuration, AMImplementationType.AUTH_POLICY_CONFIGURATIONS);
        return configurations.contains((JPAImplementation) configuration)
                || configurations.add((JPAImplementation) configuration);
    }

}
