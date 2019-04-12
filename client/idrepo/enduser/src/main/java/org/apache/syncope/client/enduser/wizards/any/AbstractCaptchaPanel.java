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
package org.apache.syncope.client.enduser.wizards.any;

import org.apache.syncope.client.enduser.markup.html.form.AjaxCaptchaFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public abstract class AbstractCaptchaPanel<T> extends Panel {

    private static final long serialVersionUID = -4310189409064713307L;

    protected String randomText;

    protected final Model<String> captchaText = new Model<>();

    protected final CaptchaImageResource captchaImageResource;

    public AbstractCaptchaPanel(final String id) {
        super(id);
        this.setOutputMarkupId(true);

        captchaImageResource = createCaptchaImageResource();
        final Image captchaImage = new Image("image", captchaImageResource);
        captchaImage.setOutputMarkupId(true);
        add(captchaImage);

        AjaxButton reloadCaptchaButton = new AjaxButton("reloadButton") {

            private static final long serialVersionUID = -957948639666058749L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                captchaImageResource.invalidate();
                target.add(captchaImage);
            }

        };
        reloadCaptchaButton.setDefaultFormProcessing(false);
        add(reloadCaptchaButton);

        add(new AjaxCaptchaFieldPanel("captcha", "captcha", captchaText)
                .setOutputMarkupId(true)
                .setOutputMarkupPlaceholderTag(true));
    }

    protected abstract CaptchaImageResource createCaptchaImageResource();

    protected abstract void reload();

    public abstract boolean captchaCheck();
}
