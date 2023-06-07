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

import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.io.IOUtils;

@BinaryPreview(mimeTypes = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
    RESTHeaders.APPLICATION_YAML, "application/x-yaml", "text/x-yaml", "text/yaml" })
public class BinaryTextPreviewer extends BinaryPreviewer {

    private static final long serialVersionUID = 3808379310090668773L;

    private String previewerId;

    public BinaryTextPreviewer(final String mimeType) {
        super(mimeType);
    }

    @Override
    public Component preview(final byte[] uploadedBytes) {
        Fragment fragment = new Fragment("preview", "noPreviewFragment", this);
        if (uploadedBytes.length > 0) {
            try {
                fragment = new Fragment("preview", "previewFragment", this);
                InputStream stream = new ByteArrayInputStream(uploadedBytes);
                TextArea<String> previewer = new TextArea<>("previewer", Model.of(IOUtils.toString(stream)));
                previewer.setOutputMarkupPlaceholderTag(true);
                previewerId = previewer.getMarkupId();
                fragment.add(previewer);
            } catch (IOException e) {
                LOG.error("Error evaluating text file", e);
            }
        }

        WebMarkupContainer previewContainer = new WebMarkupContainer("previewContainer");
        previewContainer.setOutputMarkupId(true);
        previewContainer.add(fragment);

        return this.addOrReplace(previewContainer);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        String options;
        switch (mimeType) {
            case MediaType.APPLICATION_JSON:
                options = "matchBrackets: true, autoCloseBrackets: true,";
                break;

            case MediaType.APPLICATION_XML:
                options = "autoCloseTags: true, mode: 'text/html',";
                break;

            case RESTHeaders.APPLICATION_YAML:
            case "application/x-yaml":
            case "text/x-yaml":
            case "text/yaml":
                options = "mode: 'yaml',";
                break;

            default:
                options = "mode: 'text/html',";
        }

        response.render(OnLoadHeaderItem.forScript(
                "var editor = CodeMirror.fromTextArea(document.getElementById('" + previewerId + "'), {"
                + "  readOnly: true, "
                + "  lineNumbers: true, "
                + "  lineWrapping: false, "
                + options
                + "  autoRefresh: true"
                + "});"
                + "editor.setSize('900', 250)"));
    }
}
