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
package org.apache.syncope.client.ui.commons;

import java.util.Locale;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.resource.IPropertiesFactory;
import org.apache.wicket.resource.Properties;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicMenuStringResourceLoader extends ClassStringResourceLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(DynamicMenuStringResourceLoader.class);

    public DynamicMenuStringResourceLoader() {
        super(DynamicMenuStringResourceLoader.class);
    }

    @Override
    public String loadStringResource(
            Class<?> clazz,
            String key,
            Locale locale,
            String style,
            String variation) {

        if (key != null && key.startsWith("menu.")) {
            Class<?> pageClass = DynamicMenuRegister.getPage(key);

            if (pageClass != null) {
                final String path = pageClass.getName().replace('.', '/');
                final IResourceNameIterator iter = newResourceNameIterator(path, locale, style, variation);
                final IPropertiesFactory propertiesFactory = getPropertiesFactory();

                while (iter.hasNext()) {
                    final String newPath = iter.next();
                    final Properties props = propertiesFactory.load(pageClass, newPath);

                    if (props != null) {
                        final String localeLabel = props.getString(key);
                        LOG.debug("Found label \"{}\" for key: {}", localeLabel, key);
                        return localeLabel;
                    }
                }
            }
        }

        return null;
    }
}
