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
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

/**
 * Automatically loads available javadocs from class loader (when {@link java.net.URLClassLoader}).
 */
public class WadlGenerator extends org.apache.cxf.jaxrs.model.wadl.WadlGenerator {

    private boolean inited = false;

    private String wadl = null;

    private void init() {
        synchronized (this) {
            if (!inited) {
                URL[] javaDocURLs = JavaDocUtils.getJavaDocURLs();
                if (javaDocURLs != null) {
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
                    ClassResourceInfo cri = IterableUtils.find(classResourceInfos, new Predicate<ClassResourceInfo>() {

                        @Override
                        public boolean evaluate(final ClassResourceInfo cri) {
                            return cri.isCreatedFromModel() && cri.isRoot()
                                    && cri.getServiceClass().isAssignableFrom(beanClass);
                        }
                    });
                    if (cri != null) {
                        if (!InjectionUtils.isConcreteClass(cri.getServiceClass())) {
                            cri = new ClassResourceInfo(cri);
                            classResourceInfos.add(cri);
                        }
                        cri.setResourceClass(beanClass);
                        continue;
                    }

                    cri = ResourceUtils.createClassResourceInfo(
                            beanClass, beanClass, true, true, BusFactory.getDefaultBus());
                    if (cri != null) {
                        classResourceInfos.add(cri);
                    }
                }

                wadl = generateWADL("/", classResourceInfos, false, null, null).toString();
            }
        }

        return wadl;
    }
}
