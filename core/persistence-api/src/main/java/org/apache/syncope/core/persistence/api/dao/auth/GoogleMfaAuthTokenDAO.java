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
package org.apache.syncope.core.persistence.api.dao.auth;

import org.apache.syncope.core.persistence.api.dao.DAO;
import org.apache.syncope.core.persistence.api.entity.auth.GoogleMfaAuthToken;

import java.util.Date;
import java.util.List;

public interface GoogleMfaAuthTokenDAO extends DAO<GoogleMfaAuthToken> {
    GoogleMfaAuthToken find(String user, Integer otp);

    GoogleMfaAuthToken find(String key);

    GoogleMfaAuthToken save(GoogleMfaAuthToken token);

    long count(String user);

    long count();

    void deleteAll();

    boolean delete(Integer otp);

    boolean delete(String user);

    boolean delete(String user, Integer otp);

    boolean delete(Date expirationDate);

    List<GoogleMfaAuthToken> findTokensForUser(String user);
}
