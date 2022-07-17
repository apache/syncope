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
package org.apache.syncope.core.spring.security;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;

public class TestPasswordPolicy implements PasswordPolicy {

    private static final long serialVersionUID = 4978614846223679095L;

    private final List<Implementation> rules = new ArrayList<>();

    public TestPasswordPolicy(final Implementation rule) {
        rules.add(rule);
    }

    @Override
    public String getKey() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void setName(final String description) {
        // nothing to do
    }

    @Override
    public boolean isAllowNullPassword() {
        return false;
    }

    @Override
    public void setAllowNullPassword(final boolean allowNullPassword) {
        // nothing to do
    }

    @Override
    public int getHistoryLength() {
        return 0;
    }

    @Override
    public void setHistoryLength(final int historyLength) {
        // nothing to do
    }

    @Override
    public boolean add(final Implementation rule) {
        return this.rules.add(rule);
    }

    @Override
    public List<? extends Implementation> getRules() {
        return this.rules;
    }
}
