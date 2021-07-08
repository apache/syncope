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
package org.apache.syncope.client.enduser.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.enduser.pages.OIDCClientLogin;
import org.apache.syncope.client.enduser.pages.SelfRegistration;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.syncope.client.ui.commons.resources.oidcc4ui.CodeConsumerResource;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@Resource(
        key = OIDCC4UIConstants.URL_CONTEXT + ".codeConsumer",
        path = "/" + OIDCC4UIConstants.URL_CONTEXT + "/code-consumer")
public class EnduserCodeConsumerResource extends CodeConsumerResource {

    private static final long serialVersionUID = 8348262380011763032L;

    @Override
    protected Class<? extends WebPage> getLoginPageClass() {
        return OIDCClientLogin.class;
    }

    @Override
    protected Pair<Class<? extends WebPage>, PageParameters> getSelfRegInfo(final UserTO newUser)
            throws JsonProcessingException {
        return Pair.of(SelfRegistration.class,
                new PageParameters().add(SelfRegistration.NEW_USER_PARAM, MAPPER.writeValueAsString(newUser)));
    }
}
