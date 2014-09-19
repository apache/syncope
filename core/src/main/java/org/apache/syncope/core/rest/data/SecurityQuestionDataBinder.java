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
package org.apache.syncope.core.rest.data;

import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.core.persistence.beans.SecurityQuestion;
import org.springframework.stereotype.Component;

@Component
public class SecurityQuestionDataBinder {

    public SecurityQuestionTO getSecurityQuestionTO(final SecurityQuestion securityQuestion) {
        SecurityQuestionTO securityQuestionTO = new SecurityQuestionTO();

        BeanUtils.copyProperties(securityQuestion, securityQuestionTO);

        return securityQuestionTO;
    }

    public SecurityQuestion create(final SecurityQuestionTO securityQuestionTO) {
        SecurityQuestion result = new SecurityQuestion();
        update(result, securityQuestionTO);
        return result;
    }

    public void update(final SecurityQuestion securityQuestion, final SecurityQuestionTO securityQuestionTO) {
        BeanUtils.copyProperties(securityQuestionTO, securityQuestion, "id");
    }
}
