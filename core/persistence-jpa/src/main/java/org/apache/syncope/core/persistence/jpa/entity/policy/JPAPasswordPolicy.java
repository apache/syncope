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
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "passwordPolicy")
    private List<JPAPasswordRuleConfInstance> ruleConfs = new ArrayList<>();

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
    public boolean add(final PasswordRuleConf passwordRuleConf) {
        if (passwordRuleConf == null) {
            return false;
        }

        JPAPasswordRuleConfInstance instance = new JPAPasswordRuleConfInstance();
        instance.setPasswordPolicy(this);
        instance.setInstance(passwordRuleConf);

        return ruleConfs.add(instance);
    }

    @Override
    public void removeAllRuleConfs() {
        ruleConfs.clear();
    }

    @Override
    public List<PasswordRuleConf> getRuleConfs() {
        return ruleConfs.stream().map(input -> input.getInstance()).collect(Collectors.toList());
    }
}
