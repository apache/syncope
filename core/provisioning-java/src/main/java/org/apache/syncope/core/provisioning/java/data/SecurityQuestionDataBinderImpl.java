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

import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;

public class SecurityQuestionDataBinderImpl implements SecurityQuestionDataBinder {

    protected final EntityFactory entityFactory;

    public SecurityQuestionDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public SecurityQuestionTO getSecurityQuestionTO(final SecurityQuestion securityQuestion) {
        SecurityQuestionTO securityQuestionTO = new SecurityQuestionTO();
        securityQuestionTO.setKey(securityQuestion.getKey());
        securityQuestionTO.setContent(securityQuestion.getContent());

        return securityQuestionTO;
    }

    @Override
    public SecurityQuestion create(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestion result = entityFactory.newEntity(SecurityQuestion.class);
        update(result, securityQuestionTO);
        return result;
    }

    @Override
    public void update(final SecurityQuestion securityQuestion, final SecurityQuestionTO securityQuestionTO) {
        securityQuestion.setContent(securityQuestionTO.getContent());
    }
}
