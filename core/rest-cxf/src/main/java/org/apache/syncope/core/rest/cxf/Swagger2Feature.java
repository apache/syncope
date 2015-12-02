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

import java.net.URL;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;

/**
 * Automatically loads available javadocs from class loader (when {@link java.net.URLClassLoader}).
 */
public class Swagger2Feature extends org.apache.cxf.jaxrs.swagger.Swagger2Feature {

    @Override
    public void initialize(final Server server, final Bus bus) {
        URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
        if (javaDocURLs != null) {
            super.setJavaDocURLs(javaDocURLs);
        }

        super.initialize(server, bus);
    }

}
