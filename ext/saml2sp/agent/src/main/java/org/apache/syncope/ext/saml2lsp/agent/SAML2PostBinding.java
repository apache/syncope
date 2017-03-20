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
package org.apache.syncope.ext.saml2lsp.agent;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SAML2PostBinding extends HttpServlet {

    private static final long serialVersionUID = 7969539245875799817L;

    protected static final Logger LOG = LoggerFactory.getLogger(SAML2PostBinding.class);

    protected void prepare(final HttpServletResponse response, final SAML2RequestTO requestTO) throws IOException {
        response.setContentType(MediaType.TEXT_HTML);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(""
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                + "  <body onLoad=\"document.forms[0].submit();\">"
                + "    <form action=\"" + requestTO.getIdpServiceAddress() + "\" method=\"POST\">"
                + "      <input type=\"hidden\" name=\"SAMLRequest\" value=\"" + requestTO.getContent() + "\"/>"
                + "      <input type=\"hidden\" name=\"RelayState\" value=\"" + requestTO.getRelayState() + "\"/>"
                + "      <input type=\"submit\" style=\"visibility: hidden;\"/>"
                + "    </form>"
                + "  </body>"
                + "</html>");
    }
}
