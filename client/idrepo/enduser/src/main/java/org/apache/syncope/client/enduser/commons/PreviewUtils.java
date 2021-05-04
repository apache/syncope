/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.commons;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.init.EnduserInitializer;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.DefaultPreviewer;
import org.springframework.util.ClassUtils;

public class PreviewUtils {

    public static PreviewUtils getInstance() {
        return new PreviewUtils();
    }

    private final ClassPathScanImplementationLookup classPathScanImplementationLookup;

    public PreviewUtils() {
        classPathScanImplementationLookup = (ClassPathScanImplementationLookup) SyncopeEnduserApplication.get().
                getServletContext().getAttribute(EnduserInitializer.CLASSPATH_LOOKUP);
    }

    public AbstractBinaryPreviewer getDefaultPreviewer(final String mimeType) {
        return new DefaultPreviewer("previewer", mimeType);
    }

    public AbstractBinaryPreviewer getPreviewer(final String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return null;
        }

        Class<? extends AbstractBinaryPreviewer> previewer =
                classPathScanImplementationLookup.getPreviewerClass(mimeType);
        try {
            return previewer == null
                    ? null
                    : ClassUtils.getConstructorIfAvailable(previewer, String.class, String.class).
                    newInstance(new Object[] { "previewer", mimeType });
        } catch (Exception e) {
            return null;
        }
    }
}
