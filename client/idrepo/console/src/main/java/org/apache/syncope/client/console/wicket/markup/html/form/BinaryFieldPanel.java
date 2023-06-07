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
package org.apache.syncope.client.console.wicket.markup.html.form;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInputField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.PreviewUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.HttpResourceStream;
import org.apache.syncope.client.ui.commons.markup.html.form.BaseBinaryFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.BinaryFieldDownload;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.BinaryPreviewer;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;

public class BinaryFieldPanel extends BaseBinaryFieldPanel {

    private static final long serialVersionUID = 6264462604183088931L;

    @SpringBean
    protected PreviewUtils previewUtils;

    protected final String mimeType;

    protected final WebMarkupContainer container;

    protected final AjaxLink<Void> downloadLink;

    protected final Form<?> uploadForm;

    protected final Fragment emptyFragment;

    protected final BootstrapFileInputField fileUpload;

    protected final BinaryFieldDownload fileDownload;

    protected final BinaryPreviewer previewer;

    protected final IndicatingAjaxLink<Void> resetLink;

    protected final Bytes maxUploadSize;

    protected final IModel<String> model;

    protected final String fileKey;

    public BinaryFieldPanel(
            final String id,
            final String name,
            final IModel<String> model,
            final String mimeType,
            final String fileKey) {

        super(id, name, model);
        this.model = model;
        this.fileKey = fileKey;
        this.mimeType = mimeType;

        previewer = previewUtils.getPreviewer(mimeType);

        maxUploadSize = Bytes.megabytes(SyncopeWebApplication.get().getMaxUploadFileSizeMB());
        uploadForm = new StatelessForm<>("uploadForm");
        uploadForm.setMultiPart(true);
        add(uploadForm);

        container = new WebMarkupContainer("previewContainer");
        container.setOutputMarkupId(true);

        emptyFragment = new Fragment("panelPreview", "emptyFragment", container);
        emptyFragment.setOutputMarkupId(true);
        container.add(emptyFragment);
        uploadForm.add(container);

        field = new TextField<>("textField", model);
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        uploadForm.add(new Label("preview", StringUtils.isBlank(mimeType) ? StringUtils.EMPTY : '(' + mimeType + ')'));

        fileDownload = new BinaryFieldDownload(name, fileKey, mimeType, true) {

            private static final long serialVersionUID = 7203445884857810583L;

            @Override
            protected HttpResourceStream getResourceStream() {
                return new HttpResourceStream(new ResponseHolder(buildResponse()));
            }
        };

        add(fileDownload);

        downloadLink = new AjaxLink<>("downloadLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    fileDownload.initiate(target);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().onException(e);
                }
            }
        };
        downloadLink.setOutputMarkupId(true);
        uploadForm.add(downloadLink);

        FileInputConfig config = new FileInputConfig().
                showUpload(false).showRemove(false).showPreview(false).
                browseClass("btn btn-success").browseIcon("<i class=\"fas fa-folder-open\"></i> &nbsp;");
        String language = SyncopeConsoleSession.get().getLocale().getLanguage();
        if (!Locale.ENGLISH.getLanguage().equals(language)) {
            config.withLocale(language);
        }
        fileUpload = new BootstrapFileInputField("fileUpload", new ListModel<>(new ArrayList<>()), config);
        fileUpload.add(new AjaxFormSubmitBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                FileUpload uploaded = fileUpload.getFileUpload();
                if (uploaded != null) {
                    if (maxUploadSize != null && uploaded.getSize() > maxUploadSize.bytes()) {
                        // SYNCOPE-1213 manage directly max upload file size (if set in properties file)
                        SyncopeConsoleSession.get().error(getString("tooLargeFile").
                                replace("${maxUploadSizeB}", String.valueOf(maxUploadSize.bytes())).
                                replace("${maxUploadSizeMB}", String.valueOf(maxUploadSize.bytes() / 1000000L)));
                        ((BaseWebPage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                    } else {
                        byte[] uploadedBytes = uploaded.getBytes();
                        String uploadedEncoded = Base64.getEncoder().encodeToString(uploadedBytes);
                        field.setModelObject(uploadedEncoded);
                        target.add(field);

                        Component panelPreview = previewer.preview(uploadedBytes);
                        changePreviewer(panelPreview);
                        fileUpload.setModelObject(null);
                        uploadForm.addOrReplace(fileUpload);

                        setVisibleFileButtons(StringUtils.isNotBlank(uploadedEncoded));
                        downloadLink.setEnabled(StringUtils.isNotBlank(uploadedEncoded));

                        target.add(uploadForm);
                    }
                }
            }
        });
        uploadForm.add(fileUpload);

        resetLink = new IndicatingAjaxLink<>("resetLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                field.setModelObject(null);
                target.add(field);
                downloadLink.setEnabled(false);
                container.addOrReplace(emptyFragment);
                setVisibleFileButtons(false);
                target.add(uploadForm);
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        uploadForm.add(resetLink);
    }

    protected Response buildResponse() {
        return Response.ok(new ByteArrayInputStream(Base64.getMimeDecoder().decode(getModelObject()))).
                type(StringUtils.isBlank(mimeType) ? MediaType.APPLICATION_OCTET_STREAM : mimeType).
                header(HttpHeaders.LOCATION, StringUtils.EMPTY).
                build();
    }

    protected void changePreviewer(final Component panelPreview) {
        Fragment fragment = new Fragment("panelPreview", "previewFragment", container);
        fragment.add(panelPreview);
        container.addOrReplace(fragment);
        uploadForm.addOrReplace(container);
    }

    private void setVisibleFileButtons(final boolean visible) {
        resetLink.setVisible(visible);
        downloadLink.setVisible(visible);
    }

    @Override
    public BinaryFieldPanel clone() {
        LOG.debug("Custom clone for binary field panel...");
        return new BinaryFieldPanel(getId(), this.name, this.model, this.mimeType, this.fileKey);
    }

    @Override
    public FieldPanel<String> setNewModel(final IModel<String> model) {
        field.setModel(model);
        String modelObj = model.getObject();

        if (StringUtils.isNotBlank(modelObj)) {
            Optional.ofNullable(previewer.preview(modelObj)).ifPresent(this::changePreviewer);
        }

        downloadLink.setEnabled(StringUtils.isNotBlank(modelObj));
        setVisibleFileButtons(StringUtils.isNotBlank(modelObj));
        return this;
    }

    @Override
    protected void sendError(final Exception exception) {
        SyncopeConsoleSession.get().onException(exception);
    }

    @Override
    public FieldPanel<String> setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        fileUpload.setEnabled(!readOnly);
        return this;
    }
}
