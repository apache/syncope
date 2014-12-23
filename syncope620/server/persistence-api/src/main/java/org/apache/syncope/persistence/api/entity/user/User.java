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
package org.apache.syncope.persistence.api.entity.user;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.Subject;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.Role;

public interface User extends Subject<UNormAttr, UDerAttr, UVirAttr> {

    boolean addMembership(Membership membership);

    boolean canDecodePassword();

    boolean checkToken(String token);

    void generateToken(int tokenLength, int tokenExpireTime);

    Date getChangePwdDate();

    CipherAlgorithm getCipherAlgorithm();

    String getClearPassword();

    Integer getFailedLogins();

    Date getLastLoginDate();

    Membership getMembership(Long syncopeRoleId);

    List<Membership> getMemberships();

    Set<ExternalResource> getOwnResources();

    String getPassword();

    List<String> getPasswordHistory();

    Set<Long> getRoleIds();

    List<Role> getRoles();

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

    boolean removeMembership(Membership membership);

    void removeToken();

    void setChangePwdDate(Date changePwdDate);

    void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm);

    void setEncodedPassword(String password, CipherAlgorithm cipherAlgoritm);

    void setFailedLogins(Integer failedLogins);

    void setLastLoginDate(Date lastLoginDate);

    void setMemberships(List<Membership> memberships);

    void setPassword(String password, CipherAlgorithm cipherAlgoritm);

    void setSecurityAnswer(String securityAnswer);

    void setSecurityQuestion(SecurityQuestion securityQuestion);

    void setStatus(String status);

    void setSuspended(Boolean suspended);

    void setUsername(String username);

    void setWorkflowId(String workflowId);

    boolean verifyPasswordHistory(String password, int size);

    @Override
    boolean addNormAttr(UNormAttr attr);

    @Override
    boolean addDerAttr(UDerAttr attr);

    @Override
    boolean addVirAttr(UVirAttr attr);

    @Override
    UNormAttr getNormAttr(String normSchemaName);

    @Override
    List<UNormAttr> getNormAttrs();

    @Override
    UDerAttr getDerAttr(String derSchemaName);

    @Override
    List<UDerAttr> getDerAttrs();

    @Override
    UVirAttr getVirAttr(String virSchemaName);

    @Override
    List<UVirAttr> getVirAttrs();

    @Override
    boolean removeNormAttr(UNormAttr attr);

    @Override
    boolean removeDerAttr(UDerAttr derAttr);

    @Override
    boolean removeVirAttr(UVirAttr virAttr);

}
