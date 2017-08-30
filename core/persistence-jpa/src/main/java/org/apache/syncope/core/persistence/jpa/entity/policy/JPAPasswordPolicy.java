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
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPAPasswordPolicy.TABLE)
public class JPAPasswordPolicy extends AbstractPolicy implements PasswordPolicy {

    private static final long serialVersionUID = 9138550910385232849L;

    public static final String TABLE = "PasswordPolicy";

    @Basic
    @Min(0)
    @Max(1)
    private Integer allowNullPassword;

    private int historyLength;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Rule",
            joinColumns =
            @JoinColumn(name = "policy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> rules = new ArrayList<>();

    @Override
    public boolean isAllowNullPassword() {
        return isBooleanAsInteger(allowNullPassword);
    }

    @Override
    public void setAllowNullPassword(final boolean allowNullPassword) {
        this.allowNullPassword = getBooleanAsInteger(allowNullPassword);
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
        checkImplementationType(rule, ImplementationType.PASSWORD_RULE);
        return rules.contains((JPAImplementation) rule) || rules.add((JPAImplementation) rule);
    }

    @Override
    public List<? extends Implementation> getRules() {
        return rules;
    }
}
