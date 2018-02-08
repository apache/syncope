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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Application;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

public class SyncopeOpenApiFeature extends OpenApiFeature {

    protected static class SyncopeDefaultApplication extends Application {

        private final Set<Class<?>> serviceClasses;

        SyncopeDefaultApplication(final List<ClassResourceInfo> cris, final Set<String> resourcePackages) {
            this.serviceClasses = cris.stream().map(ClassResourceInfo::getServiceClass).
                    filter(cls -> {
                        return resourcePackages == null || resourcePackages.isEmpty()
                                ? true
                                : resourcePackages.stream().
                                        anyMatch(pkg -> cls.getPackage().getName().startsWith(pkg));
                    }).
                    collect(Collectors.toSet());
        }

        @Override
        public Set<Class<?>> getClasses() {
            return serviceClasses;
        }
    }

    @Override
    protected Application getApplicationOrDefault(
            final Server server,
            final ServerProviderFactory factory,
            final JAXRSServiceFactoryBean sfb,
            final Bus bus) {

        ApplicationInfo appInfo = null;
        if (!isScan()) {
            appInfo = new ApplicationInfo(
                    new SyncopeDefaultApplication(sfb.getClassResourceInfo(), getResourcePackages()), bus);
            server.getEndpoint().put(Application.class.getName(), appInfo);
        }

        return (appInfo == null) ? null : appInfo.getProvider();
    }
}
