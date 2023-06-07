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
package org.apache.syncope.client.ui.commons.markup.html.form.preview;

import java.util.Base64;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BinaryPreviewer extends Panel {

    protected static final Logger LOG = LoggerFactory.getLogger(BinaryPreviewer.class);

    private static final long serialVersionUID = -2482706463911903025L;

    protected final String mimeType;

    public BinaryPreviewer(final String mimeType) {
        super("previewer");
        this.mimeType = mimeType;
    }

    public Component preview(final String uploaded) {
        return preview(Base64.getMimeDecoder().decode(uploaded));
    }

    public abstract Component preview(byte[] uploadedBytes);
}
