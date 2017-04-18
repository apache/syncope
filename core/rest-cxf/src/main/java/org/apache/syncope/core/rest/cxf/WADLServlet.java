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
package org.apache.syncope.core.rest.cxf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.apache.cocoon.pipeline.CachingPipeline;
import org.apache.cocoon.pipeline.Pipeline;
import org.apache.cocoon.sax.SAXPipelineComponent;
import org.apache.cocoon.sax.component.XMLGenerator;
import org.apache.cocoon.sax.component.XMLSerializer;
import org.apache.cocoon.sax.component.XSLTTransformer;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class WADLServlet extends HttpServlet {

    private static final long serialVersionUID = -6737005675471095560L;

    private static final Pattern SCHEMA_PATTERN = Pattern.compile("/schema_(.*)_(.*)\\.html");

    protected void finish(final Pipeline<SAXPipelineComponent> pipeline, final HttpServletResponse response)
            throws ServletException, IOException {

        pipeline.addComponent(XMLSerializer.createHTML4Serializer());
        pipeline.setup(response.getOutputStream());
        try {
            pipeline.execute();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        Matcher schemaMatcher = SCHEMA_PATTERN.matcher(request.getServletPath());

        WadlGenerator wadlGenerator = ApplicationContextProvider.getApplicationContext().getBean(WadlGenerator.class);
        String wadl = wadlGenerator.getWadl();

        Pipeline<SAXPipelineComponent> pipeline = new CachingPipeline<>();
        pipeline.addComponent(new XMLGenerator(wadl));
        if ("/index.html".equals(request.getServletPath())) {
            XSLTTransformer xslt = new XSLTTransformer(getClass().getResource("/wadl2html/index.xsl"));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("contextPath", request.getContextPath());
            xslt.setParameters(parameters);

            pipeline.addComponent(xslt);

            finish(pipeline, response);
        } else if (schemaMatcher.matches()) {
            XSLTTransformer xslt = new XSLTTransformer(getClass().getResource("/wadl2html/schema.xsl"));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("contextPath", request.getContextPath());
            parameters.put("schema-position", schemaMatcher.group(1));
            parameters.put("schema-prefix", schemaMatcher.group(2));
            xslt.setParameters(parameters);

            pipeline.addComponent(xslt);

            finish(pipeline, response);
        } else if ("/syncope.wadl".equals(request.getServletPath())) {
            response.setContentType(MediaType.APPLICATION_XML);

            InputStream in = new ByteArrayInputStream(wadl.getBytes());
            OutputStream out = response.getOutputStream();
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
        } else {
            throw new ServerException("URL not supported: " + request.getRequestURI());
        }
    }

}
