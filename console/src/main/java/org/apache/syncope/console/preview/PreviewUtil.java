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

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.console.init.PreviewPanelClassInitializer;
import org.apache.syncope.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.wicket.Component;
import org.apache.wicket.util.crypt.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@org.springframework.stereotype.Component
public class PreviewUtil {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PreviewUtil.class);

    @Autowired
    private PreviewPanelClassInitializer previewPanelClassInitializer;

    public Component getPreviewer(final String mimeType, final String file) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        final Class<? extends AbstractBinaryPreviewer> previewer = StringUtils.isBlank(file)
                ? null
                : previewPanelClassInitializer.getClass(mimeType);

        return previewer == null
                ? null
                : ((AbstractBinaryPreviewer) Class.forName(previewer.getName()).
                getConstructor(String.class, String.class, byte[].class).newInstance(
                        new Object[] { "previewer", mimeType, Base64.decodeBase64(file) })).preview();
    }

    public Component getPreviewer(final String mimeType, final byte[] file) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        final Class<? extends AbstractBinaryPreviewer> previewer = previewPanelClassInitializer.getClass(mimeType);

        return previewer == null
                ? null
                : ((AbstractBinaryPreviewer) Class.forName(previewer.getName()).
                getConstructor(String.class, String.class, byte[].class).newInstance(
                        new Object[] { "previewer", mimeType, file })).preview();
    }
}
