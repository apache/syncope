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
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.GroupableRelatable;

public interface User extends
        GroupableRelatable<User, UMembership, UPlainAttr, AnyObject, URelationship> {

    String getUsername();

    void setUsername(String username);

    String getToken();

    Date getTokenExpireTime();

    void generateToken(int tokenLength, int tokenExpireTime);

    void removeToken();

    boolean checkToken(String token);

    boolean hasTokenExpired();

    Date getChangePwdDate();

    void setChangePwdDate(Date changePwdDate);

    CipherAlgorithm getCipherAlgorithm();

    void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm);

    boolean canDecodePassword();

    String getClearPassword();

    void removeClearPassword();

    String getPassword();

    void setEncodedPassword(String password, CipherAlgorithm cipherAlgoritm);

    void setPassword(String password, CipherAlgorithm cipherAlgoritm);

    List<String> getPasswordHistory();

    boolean verifyPasswordHistory(String password, int size);

    SecurityQuestion getSecurityQuestion();

    void setSecurityQuestion(SecurityQuestion securityQuestion);

    String getSecurityAnswer();

    void setSecurityAnswer(String securityAnswer);

    Integer getFailedLogins();

    void setFailedLogins(Integer failedLogins);

    Date getLastLoginDate();

    void setLastLoginDate(Date lastLoginDate);

    Boolean isSuspended();

    void setSuspended(Boolean suspended);

    boolean isMustChangePassword();

    void setMustChangePassword(boolean mustChangePassword);

    boolean add(Role role);

    List<? extends Role> getRoles();

    String getLastRecertificator();

    void setLastRecertificator(String lastRecertificator);

    Date getLastRecertification();

    void setLastRecertification(Date lastRecertificion);

}
