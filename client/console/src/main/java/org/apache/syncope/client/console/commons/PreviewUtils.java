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
package org.apache.syncope.client.console.commons;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.wicket.Component;
import org.apache.wicket.util.crypt.Base64;
import org.springframework.util.Assert;

public final class PreviewUtils {

    public static PreviewUtils getInstance() {
        return new PreviewUtils();
    }

    private static <T> Constructor<T> getConstructorIfAvailable(final Class<T> clazz, final Class<?>... paramTypes) {
        Assert.notNull(clazz, "Class must not be null");
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private final ClassPathScanImplementationLookup classPathScanImplementationLookup;

    private PreviewUtils() {
        classPathScanImplementationLookup = (ClassPathScanImplementationLookup) SyncopeConsoleApplication.get().
                getServletContext().getAttribute(ConsoleInitializer.CLASSPATH_LOOKUP);
    }

    public Component getPreviewer(final String mimeType, final String file)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<? extends AbstractBinaryPreviewer> previewer = StringUtils.isBlank(file)
                ? null
                : classPathScanImplementationLookup.getPreviewerClass(mimeType);

        return previewer == null
                ? null
                : getConstructorIfAvailable(previewer, String.class, String.class, byte[].class).
                newInstance(new Object[] { "previewer", mimeType, Base64.decodeBase64(file) }).
                preview();
    }

    public Component getPreviewer(final String mimeType, final byte[] file)
            throws InstantiationException, IllegalAccessException, InvocationTargetException {

        Class<? extends AbstractBinaryPreviewer> previewer = classPathScanImplementationLookup.getPreviewerClass(
                mimeType);

        return previewer == null
                ? null
                : getConstructorIfAvailable(previewer, String.class, String.class, byte[].class).
                newInstance(new Object[] { "previewer", mimeType, file }).
                preview();
    }
}
