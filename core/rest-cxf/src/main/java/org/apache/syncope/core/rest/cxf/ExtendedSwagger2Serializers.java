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

import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.jaxrs.swagger.DefaultSwagger2Serializers;
import org.apache.cxf.jaxrs.swagger.Swagger2Serializers;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.spring.ApplicationContextProvider;

public class ExtendedSwagger2Serializers extends DefaultSwagger2Serializers implements Swagger2Serializers {

    private List<String> domains;

    public ExtendedSwagger2Serializers() {
        super();

        URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
        if (javaDocURLs != null) {
            super.setJavaDocURLs(javaDocURLs);
        }
    }

    @Override
    protected void addParameters(final List<Parameter> parameters) {
        if (domains == null) {
            domains = new ArrayList<>(
                    ApplicationContextProvider.getApplicationContext().
                            getBean(DomainsHolder.class).getDomains().keySet());
        }

        boolean domainHeaderParameterFound = false;
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof HeaderParameter
                    && RESTHeaders.DOMAIN.equals(parameters.get(i).getName())) {

                domainHeaderParameterFound = true;
            }
        }
        if (!domainHeaderParameterFound) {
            HeaderParameter domainHeaderParameter = new HeaderParameter();
            domainHeaderParameter.setName(RESTHeaders.DOMAIN);
            domainHeaderParameter.setRequired(true);
            domainHeaderParameter.setType("string");
            domainHeaderParameter.setEnum(domains);
            domainHeaderParameter.setDefault(SyncopeConstants.MASTER_DOMAIN);

            parameters.add(domainHeaderParameter);
        }
    }
}
