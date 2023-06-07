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

import java.io.Serializable;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.BinaryPreviewer;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.DefaultPreviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class PreviewUtils implements Serializable {

    private static final long serialVersionUID = 2765845550328631887L;

    protected static final Logger LOG = LoggerFactory.getLogger(PreviewUtils.class);

    protected final ClassPathScanImplementationLookup lookup;

    public PreviewUtils(final ClassPathScanImplementationLookup lookup) {
        this.lookup = lookup;
    }

    public BinaryPreviewer getPreviewer(final String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return new DefaultPreviewer(mimeType);
        }

        return Optional.ofNullable(lookup.getPreviewerClass(mimeType)).map(clazz -> {
            try {
                return ClassUtils.getConstructorIfAvailable(clazz, String.class).
                        newInstance(new Object[] { mimeType });
            } catch (Exception e) {
                LOG.error("While getting BinaryPreviewer for {}", mimeType, e);

                return new DefaultPreviewer(mimeType);
            }
        }).orElseGet(() -> new DefaultPreviewer(mimeType));
    }
}
