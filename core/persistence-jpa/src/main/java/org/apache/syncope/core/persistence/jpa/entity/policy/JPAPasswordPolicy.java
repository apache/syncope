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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPAPasswordPolicy.TABLE)
public class JPAPasswordPolicy extends AbstractPolicy implements PasswordPolicy {

    private static final long serialVersionUID = 9138550910385232849L;

    public static final String TABLE = "PasswordPolicy";

    @NotNull
    private Boolean allowNullPassword = false;

    private int historyLength;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Rule",
            joinColumns =
            @JoinColumn(name = "policy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "policy_id", "implementation_id" }))
    private List<JPAImplementation> rules = new ArrayList<>();

    @Override
    public boolean isAllowNullPassword() {
        return allowNullPassword;
    }

    @Override
    public void setAllowNullPassword(final boolean allowNullPassword) {
        this.allowNullPassword = allowNullPassword;
    }

    @Override
    public int getHistoryLength() {
        return historyLength;
    }

    @Override
    public void setHistoryLength(final int historyLength) {
        this.historyLength = historyLength;
    }

    @Override
    public boolean add(final Implementation rule) {
        checkType(rule, JPAImplementation.class);
        checkImplementationType(rule, IdRepoImplementationType.PASSWORD_RULE);
        return rules.contains((JPAImplementation) rule) || rules.add((JPAImplementation) rule);
    }

    @Override
    public List<? extends Implementation> getRules() {
        return rules;
    }
}
