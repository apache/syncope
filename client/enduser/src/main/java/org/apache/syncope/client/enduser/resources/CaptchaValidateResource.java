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

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.model.CaptchaRequest;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptchaValidateResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaValidateResource.class);

    private static final long serialVersionUID = 6453101466981543020L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {

        LOG.debug("Validate captcha request");

        AbstractResource.ResourceResponse response = new AbstractResource.ResourceResponse();
        try {
            HttpServletRequest currentRequest = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            if (!xsrfCheck(currentRequest)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            final CaptchaRequest enteredCaptcha = POJOHelper.deserialize(IOUtils.toString(currentRequest.
                    getInputStream()), CaptchaRequest.class);

            final String currentCaptcha = currentRequest.getSession().getAttribute(
                    SyncopeEnduserConstants.CAPTCHA_SESSION_KEY) == null
                            ? null
                            : currentRequest.getSession().getAttribute(SyncopeEnduserConstants.CAPTCHA_SESSION_KEY).
                            toString();

            if (StringUtils.isBlank(currentCaptcha) || enteredCaptcha == null) {
                LOG.info("Could not validate captcha: current session captcha or inserted captcha are empty or null");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(),
                        "ErrorMessage{{ Could not validate captcha: current session captcha or entered captcha are "
                        + "empty or null }}");
            } else {
                LOG.info("Is entered captcha equal to current session captcha? {}", enteredCaptcha.getValue().equals(
                        currentCaptcha));

                response.setWriteCallback(new WriteCallback() {

                    @Override
                    public void writeData(final IResource.Attributes attributes) throws IOException {
                        attributes.getResponse().
                                write(String.valueOf(enteredCaptcha.getValue().equals(currentCaptcha)));
                    }
                });
                response.setStatusCode(Response.Status.OK.getStatusCode());
            }
        } catch (Exception e) {
            LOG.error("Could not validate captcha", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder().append(
                    "ErrorMessage{{ Could not validate captcha ")
                    .append(e.getMessage()).append(" }}").toString());
        }
        return response;
    }
}
