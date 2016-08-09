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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;

public class SecurityQuestionRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3167730221361895176L;

    public List<SecurityQuestionTO> list() {
        return getService(SecurityQuestionService.class).list();
    }

    public void create(final SecurityQuestionTO securityQuestionTO) {
        getService(SecurityQuestionService.class).create(securityQuestionTO);
    }

    public void update(final SecurityQuestionTO securityQuestionTO) {
        getService(SecurityQuestionService.class).update(securityQuestionTO);
    }

    public void delete(final String securityQuestionKey) {
        getService(SecurityQuestionService.class).delete(securityQuestionKey);
    }

    public SecurityQuestionTO readByUser(final String username) {
        return getService(SecurityQuestionService.class).readByUser(username);
    }

}
