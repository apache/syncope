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
package org.apache.syncope.core.persistence.api.entity.user;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;

public interface User extends
        Account,
        Groupable<User, UMembership, AnyObject, URelationship>,
        Relatable<User, AnyObject, URelationship> {

    String getToken();

    OffsetDateTime getTokenExpireTime();

    void generateToken(int tokenLength, int tokenExpireTime);

    void removeToken();

    boolean checkToken(String token);

    boolean hasTokenExpired();

    OffsetDateTime getChangePwdDate();

    void setChangePwdDate(OffsetDateTime changePwdDate);

    void addToPasswordHistory(String password);

    void removeOldestEntriesFromPasswordHistory(int n);

    List<String> getPasswordHistory();

    SecurityQuestion getSecurityQuestion();

    void setSecurityQuestion(SecurityQuestion securityQuestion);

    String getSecurityAnswer();

    void setSecurityAnswer(String securityAnswer);

    Integer getFailedLogins();

    void setFailedLogins(Integer failedLogins);

    OffsetDateTime getLastLoginDate();

    void setLastLoginDate(OffsetDateTime lastLoginDate);

    boolean isMustChangePassword();

    void setMustChangePassword(boolean mustChangePassword);

    boolean add(Role role);

    List<? extends Role> getRoles();

    boolean add(LinkedAccount account);

    Optional<? extends LinkedAccount> getLinkedAccount(String resource, String connObjectKeyValue);

    List<? extends LinkedAccount> getLinkedAccounts(String resource);

    List<? extends LinkedAccount> getLinkedAccounts();
}
