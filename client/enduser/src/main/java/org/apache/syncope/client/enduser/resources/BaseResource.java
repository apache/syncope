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

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseResource extends AbstractResource {

    private static final long serialVersionUID = -7875801358718612782L;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseResource.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected final boolean xsrfCheck(final HttpServletRequest request) {
        final String requestXSRFHeader = request.getHeader(SyncopeEnduserConstants.XSRF_HEADER_NAME);
        return SyncopeEnduserApplication.get().isXsrfEnabled()
                ? StringUtils.isNotBlank(requestXSRFHeader)
                && SyncopeEnduserSession.get().getCookieUtils().
                        getCookie(SyncopeEnduserConstants.XSRF_COOKIE).getValue().equals(requestXSRFHeader)
                : true;
    }

    protected final boolean captchaCheck(final String enteredCaptcha, final Object currentCaptcha) {
        return SyncopeEnduserApplication.get().isCaptchaEnabled()
                ? StringUtils.isBlank(currentCaptcha.toString()) || enteredCaptcha == null
                ? false
                : enteredCaptcha.equals(currentCaptcha.toString())
                : true;
    }
}
