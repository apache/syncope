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
package org.apache.syncope.client.cli.commands.question;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;

public class QuestionSyncopeOperations {

    private final SecurityQuestionService securityQuestionService = SyncopeServices.get(SecurityQuestionService.class);

    public List<SecurityQuestionTO> list() {
        return securityQuestionService.list();
    }

    public void delete(final String questionKey) {
        securityQuestionService.delete(questionKey);
    }

    public SecurityQuestionTO read(final String questionKey) {
        return securityQuestionService.read(questionKey);
    }

    public SecurityQuestionTO readByUser(final String username) {
        return securityQuestionService.readByUser(username);
    }
}
