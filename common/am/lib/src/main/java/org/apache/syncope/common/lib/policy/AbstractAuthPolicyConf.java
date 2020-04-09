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
package org.apache.syncope.common.lib.policy;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlType
@XmlSeeAlso({ DefaultAuthPolicyConf.class })
public abstract class AbstractAuthPolicyConf implements Serializable, AuthPolicyConf {

    private static final long serialVersionUID = 9185127128182430142L;

    private String name;

    private AuthPolicyCriteriaConf criteria;

    public AbstractAuthPolicyConf() {
        setName(getClass().getName());
    }

    public AbstractAuthPolicyConf(final String name) {
        setName(name);
    }

    @Override
    public AuthPolicyCriteriaConf getCriteria() {
        return criteria;
    }

    public void setCriteria(final AuthPolicyCriteriaConf criteria) {
        this.criteria = criteria;
    }

    @Override
    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }
}
