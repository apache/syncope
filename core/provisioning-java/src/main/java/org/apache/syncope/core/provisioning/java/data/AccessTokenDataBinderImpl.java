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
package org.apache.syncope.core.provisioning.java.data;

import java.util.Date;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenDataBinderImpl implements AccessTokenDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "owner" };

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public void create(final String key, final String body, final Date expiryTime) {
        AccessToken accessToken = entityFactory.newEntity(AccessToken.class);
        accessToken.setKey(key);
        accessToken.setBody(body);
        accessToken.setExpiryTime(expiryTime);
        accessToken.setOwner(AuthContextUtils.getUsername());

        accessTokenDAO.save(accessToken);
    }

    @Override
    public void update(final AccessToken accessToken, final String body, final Date expiryTime) {
        accessToken.setBody(body);
        accessToken.setExpiryTime(expiryTime);

        accessTokenDAO.save(accessToken);
    }

    @Override
    public AccessTokenTO getAccessTokenTO(final AccessToken accessToken) {
        AccessTokenTO accessTokenTO = new AccessTokenTO();
        BeanUtils.copyProperties(accessToken, accessTokenTO, IGNORE_PROPERTIES);
        accessTokenTO.setOwner(accessToken.getOwner());

        return accessTokenTO;
    }
}
