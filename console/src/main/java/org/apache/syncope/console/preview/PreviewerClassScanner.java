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
package org.apache.syncope.console.preview;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class PreviewerClassScanner extends ClassPathScanningCandidateComponentProvider {

    private static final String BASE_PATH = "org.apache.syncope.console.wicket.markup.html.form";

    public PreviewerClassScanner() {
        super(false);
        addIncludeFilter(new AnnotationTypeFilter(BinaryPreview.class));
    }

    @SuppressWarnings("unchecked")
    public final List<Class<? extends AbstractBinaryPreviewer>> getComponentClasses() {
        List<Class<? extends AbstractBinaryPreviewer>> classes =
                new ArrayList<Class<? extends AbstractBinaryPreviewer>>();
        for (BeanDefinition candidate : findCandidateComponents(BASE_PATH)) {
            classes.add((Class<AbstractBinaryPreviewer>) ClassUtils.resolveClassName(candidate.getBeanClassName(),
                    ClassUtils.getDefaultClassLoader()));
        }
        return classes;
    }
}
