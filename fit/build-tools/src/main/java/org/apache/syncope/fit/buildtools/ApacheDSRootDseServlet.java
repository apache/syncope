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
package org.apache.syncope.fit.buildtools;

import java.io.PrintWriter;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;

@WebServlet(urlPatterns = "/apacheDS")
public class ApacheDSRootDseServlet extends HttpServlet {

    private static final long serialVersionUID = 1514567335969002735L;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException {
        try {
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            out.println("*** ApacheDS RootDSE ***\n");

            DirContext ctx = new InitialDirContext(this.createEnv());

            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(new String[] { "*", "+" });
            ctls.setSearchScope(SearchControls.OBJECT_SCOPE);

            NamingEnumeration<SearchResult> result = ctx.search("", "(objectClass=*)", ctls);
            if (result.hasMore()) {
                SearchResult entry = result.next();
                Attributes as = entry.getAttributes();

                NamingEnumeration<String> ids = as.getIDs();
                while (ids.hasMore()) {
                    String id = ids.next();
                    Attribute attr = as.get(id);
                    for (int i = 0; i < attr.size(); ++i) {
                        out.println(id + ": " + attr.get(i));
                    }
                }
            }
            ctx.close();

            out.flush();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Creates an environment configuration for JNDI access.
     */
    private Properties createEnv() {
        // Fetch directory service from servlet context
        ServletContext servletContext = this.getServletContext();
        DirectoryService directoryService = (DirectoryService) servletContext.getAttribute(DirectoryService.JNDI_KEY);

        Properties env = new Properties();
        env.put(DirectoryService.JNDI_KEY, directoryService);
        env.put(Context.PROVIDER_URL, "");
        env.put(Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName());

        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        env.put(Context.SECURITY_CREDENTIALS, "secret");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        return env;
    }
}
