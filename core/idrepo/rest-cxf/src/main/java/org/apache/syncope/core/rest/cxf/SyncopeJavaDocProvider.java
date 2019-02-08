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
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;

/**
 * Keep this class until CXF 3.3.1 is released, then use its parent class.
 */
public class SyncopeJavaDocProvider extends JavaDocProvider {

    public SyncopeJavaDocProvider(final URL... javaDocUrls) {
        super(javaDocUrls);
    }

    public SyncopeJavaDocProvider(final String... paths) throws Exception {
        super(paths);
    }

    @Override
    protected String getOperLink() {
        String operLink = "<A NAME=\"";
        return JAVA_VERSION == JAVA_VERSION_16
                ? operLink
                : JAVA_VERSION <= JAVA_VERSION_18
                        ? operLink.toLowerCase()
                        : "<a id=\"";
    }
}
