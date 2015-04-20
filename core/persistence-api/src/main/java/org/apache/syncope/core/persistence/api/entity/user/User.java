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

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public interface User extends Subject<UPlainAttr, UDerAttr, UVirAttr> {

    boolean addRole(Role role);

    boolean addMembership(Membership membership);

    boolean canDecodePassword();

    boolean checkToken(String token);

    void generateToken(int tokenLength, int tokenExpireTime);

    Date getChangePwdDate();

    CipherAlgorithm getCipherAlgorithm();

    String getClearPassword();

    Integer getFailedLogins();

    Date getLastLoginDate();

    List<? extends Role> getRoles();

    Membership getMembership(Long groupKey);

    List<? extends Membership> getMemberships();

    Set<? extends ExternalResource> getOwnResources();

    String getPassword();

    List<String> getPasswordHistory();

    Set<Long> getGroupKeys();

    List<Group> getGroups();

    String getSecurityAnswer();

    SecurityQuestion getSecurityQuestion();

    String getStatus();

    String getToken();

    Date getTokenExpireTime();

    String getUsername();

    String getWorkflowId();

    boolean hasTokenExpired();

    Boolean isSuspended();

    void removeClearPassword();

    boolean removeRole(Role role);

    boolean removeMembership(Membership membership);

    void removeToken();

    void setChangePwdDate(Date changePwdDate);

    void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm);

    void setEncodedPassword(String password, CipherAlgorithm cipherAlgoritm);

    void setFailedLogins(Integer failedLogins);

    void setLastLoginDate(Date lastLoginDate);

    void setPassword(String password, CipherAlgorithm cipherAlgoritm);

    void setSecurityAnswer(String securityAnswer);

    void setSecurityQuestion(SecurityQuestion securityQuestion);

    void setStatus(String status);

    void setSuspended(Boolean suspended);

    void setUsername(String username);

    void setWorkflowId(String workflowId);

    boolean verifyPasswordHistory(String password, int size);

    @Override
    boolean addPlainAttr(UPlainAttr attr);

    @Override
    boolean removePlainAttr(UPlainAttr attr);

    @Override
    boolean addDerAttr(UDerAttr attr);

    @Override
    boolean removeDerAttr(UDerAttr derAttr);

    @Override
    boolean addVirAttr(UVirAttr attr);

    @Override
    boolean removeVirAttr(UVirAttr virAttr);

    @Override
    UPlainAttr getPlainAttr(String plainSchemaName);

    @Override
    List<? extends UPlainAttr> getPlainAttrs();

    @Override
    UDerAttr getDerAttr(String derSchemaName);

    @Override
    List<? extends UDerAttr> getDerAttrs();

    @Override
    UVirAttr getVirAttr(String virSchemaName);

    @Override
    List<? extends UVirAttr> getVirAttrs();

}
