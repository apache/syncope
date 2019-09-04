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

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Automatically loads available javadocs from class loader (when {@link java.net.URLClassLoader}).
 */
public class WadlGenerator extends org.apache.cxf.jaxrs.model.wadl.WadlGenerator implements EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(WadlGenerator.class);

    private Environment env;

    private boolean inited = false;

    private String wadl = null;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    private void init() {
        synchronized (this) {
            if (!inited) {
                URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
                if (javaDocURLs == null) {
                    String[] javaDocPaths = JavaDocUtils.getJavaDocPaths(env);
                    if (javaDocPaths != null) {
                        try {
                            super.setJavaDocPaths(javaDocPaths);
                        } catch (Exception e) {
                            LOG.error("Could not set javadoc paths from {}", List.of(javaDocPaths), e);
                        }
                    }
                } else {
                    super.setJavaDocURLs(javaDocURLs);
                }

                inited = true;
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext context) {
        init();
        super.filter(context);
    }

    public String getWadl() {
        synchronized (this) {
            if (wadl == null) {
                init();

                List<Class<?>> resourceClasses = new ArrayList<>();
                try {
                    List<Class<? extends Annotation>> anns = new ArrayList<>();
                    anns.add(Path.class);
                    Map<Class<? extends Annotation>, Collection<Class<?>>> discoveredClasses =
                            ClasspathScanner.findClasses(ClasspathScanner.parsePackages(
                                    "org.apache.syncope.common.rest.api.service"),
                                    anns);
                    if (discoveredClasses.containsKey(Path.class)) {
                        resourceClasses.addAll(discoveredClasses.get(Path.class));
                    }
                } catch (Exception e) {
                    // ignore
                }

                List<ClassResourceInfo> classResourceInfos = new ArrayList<>();
                for (final Class<?> beanClass : resourceClasses) {
                    Optional<ClassResourceInfo> cri = classResourceInfos.stream().filter(c
                            -> c.isCreatedFromModel() && c.isRoot() && c.getServiceClass().isAssignableFrom(beanClass)).
                            findFirst();
                    if (cri.isPresent()) {
                        if (!InjectionUtils.isConcreteClass(cri.get().getServiceClass())) {
                            cri = Optional.of(new ClassResourceInfo(cri.get()));
                            classResourceInfos.add(cri.get());
                        }
                        cri.get().setResourceClass(beanClass);
                    } else {
                        cri = Optional.ofNullable(ResourceUtils.createClassResourceInfo(
                                beanClass, beanClass, true, true, BusFactory.getDefaultBus()));
                        cri.ifPresent(classResourceInfos::add);
                    }
                }

                wadl = generateWADL("/", classResourceInfos, false, null, null).toString();
            }
        }

        return wadl;
    }
}
