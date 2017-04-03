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

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptchaResource extends CaptchaImageResource {

    private static final long serialVersionUID = 8293404296348102926L;

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaResource.class);

    @Override
    protected byte[] render() {
        LOG.debug("Generate captcha");

        String captcha = RandomStringUtils.randomAlphabetic(6);
        HttpServletRequest request = ((HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest());
        // store the captcha in the current session
        request.getSession().setAttribute(SyncopeEnduserConstants.CAPTCHA_SESSION_KEY, captcha);

        getChallengeIdModel().setObject(captcha);
        return super.render();
    }

}
