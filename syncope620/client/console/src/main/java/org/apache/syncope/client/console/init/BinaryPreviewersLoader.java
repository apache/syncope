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
package org.apache.syncope.client.console.init;

import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.client.console.preview.BinaryPreview;
import org.apache.syncope.client.console.preview.PreviewerClassScanner;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BinaryPreviewersLoader implements SyncopeConsoleLoader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BinaryPreviewersLoader.class);

    @Autowired
    private PreviewerClassScanner classScanner;

    private List<Class<? extends AbstractBinaryPreviewer>> classes;

    @Override
    public Integer getPriority() {
        return 0;
    }

    @Override
    public void load() {
        classes = classScanner.getComponentClasses();
    }

    public List<Class<? extends AbstractBinaryPreviewer>> getClasses() {
        LOG.debug("Returning loaded classes: {}", classes);
        return classes;
    }

    public Class<? extends AbstractBinaryPreviewer> getClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);
        Class<? extends AbstractBinaryPreviewer> previewer = null;
        for (Class<? extends AbstractBinaryPreviewer> candidate : classes) {
            LOG.debug("Evaluating previewer class {} for MIME type {}", candidate.getName(), mimeType);
            if (ArrayUtils.contains(candidate.getAnnotation(BinaryPreview.class).mimeTypes(), mimeType)) {
                LOG.debug("Found existing previewer for MIME type {}: {}", mimeType, candidate.getName());
                previewer = candidate;
            }
        }
        return previewer;
    }
}
