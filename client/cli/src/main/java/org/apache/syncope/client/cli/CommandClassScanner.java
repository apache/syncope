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
package org.apache.syncope.client.cli;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

public class CommandClassScanner extends ClassPathScanningCandidateComponentProvider {

    public CommandClassScanner() {
        super(false);
        addIncludeFilter(new AnnotationTypeFilter(Command.class));
    }

    public final List<Class<? extends AbstractCommand>> getComponentClasses() throws IllegalArgumentException {
        final String basePackage = "org.apache.syncope.client.cli.commands";
        List<Class<? extends AbstractCommand>> classes = new ArrayList<>();
        for (final BeanDefinition candidate : findCandidateComponents(basePackage)) {
            @SuppressWarnings("unchecked")
            final Class<? extends AbstractCommand> cls = (Class<? extends AbstractCommand>) ClassUtils.resolveClassName(
                    candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
            classes.add(cls);
        }
        return classes;
    }
}
