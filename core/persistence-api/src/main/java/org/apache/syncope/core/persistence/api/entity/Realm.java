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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;

public interface Realm extends Entity {

    String getName();

    void setName(String name);

    Realm getParent();

    void setParent(Realm parent);

    String getFullPath();

    AccountPolicy getAccountPolicy();

    void setAccountPolicy(AccountPolicy accountPolicy);

    PasswordPolicy getPasswordPolicy();

    void setPasswordPolicy(PasswordPolicy passwordPolicy);

    AuthPolicy getAuthPolicy();

    void setAuthPolicy(AuthPolicy authPolicy);

    AccessPolicy getAccessPolicy();

    void setAccessPolicy(AccessPolicy accessPolicy);

    AttrReleasePolicy getAttrReleasePolicy();

    void setAttrReleasePolicy(AttrReleasePolicy policy);

    TicketExpirationPolicy getTicketExpirationPolicy();

    void setTicketExpirationPolicy(TicketExpirationPolicy policy);

    boolean add(Implementation action);

    List<? extends Implementation> getActions();

    boolean add(AnyTemplateRealm template);

    Optional<? extends AnyTemplateRealm> getTemplate(AnyType anyType);

    List<? extends AnyTemplateRealm> getTemplates();

    boolean add(ExternalResource resource);

    List<String> getResourceKeys();

    List<? extends ExternalResource> getResources();

}
