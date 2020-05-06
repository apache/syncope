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
package org.apache.syncope.core.persistence.api.dao;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

public interface PolicyDAO extends DAO<Policy> {

    <T extends Policy> T find(String key);

    <T extends Policy> List<T> find(Class<T> reference);

    List<AccountPolicy> findByAccountRule(Implementation accountRule);

    List<PasswordPolicy> findByPasswordRule(Implementation passwordRule);

    List<PullPolicy> findByPullCorrelationRule(Implementation correlationRule);

    List<PushPolicy> findByPushCorrelationRule(Implementation correlationRule);

    List<AccountPolicy> findByResource(ExternalResource resource);

    List<Policy> findAll();

    <T extends Policy> T save(T policy);

    <T extends Policy> void delete(T policy);
}
