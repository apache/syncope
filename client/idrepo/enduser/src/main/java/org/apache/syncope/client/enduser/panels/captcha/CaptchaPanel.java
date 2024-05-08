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
package org.apache.syncope.client.enduser.panels.captcha;

import java.security.SecureRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class CaptchaPanel<T> extends Panel {

    private static final long serialVersionUID = -450657681453274465L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final RandomStringGenerator RANDOM_LETTERS = new RandomStringGenerator.Builder().
            usingRandom(RANDOM::nextInt).
            withinRange('a', 'z').
            get();

    private final Model<String> captchaText = new Model<>();

    private final CaptchaImageResource captchaImageResource;

    public CaptchaPanel(final String id) {
        super(id);

        captchaImageResource = new CaptchaImageResource() {

            private static final long serialVersionUID = 2436829189992948005L;

            @Override
            protected byte[] render() {
                getChallengeIdModel().setObject(RANDOM_LETTERS.generate(6));
                return super.render();
            }
        };
        Image captchaImage = new Image("image", captchaImageResource);
        captchaImage.setOutputMarkupId(true);
        add(captchaImage);

        add(new AjaxButton("reloadButton") {

            private static final long serialVersionUID = -957948639666058749L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                captchaImageResource.invalidate();
                target.add(captchaImage);
            }

        }.setDefaultFormProcessing(false));

        add(new AjaxTextFieldPanel("captcha", "captcha", captchaText).
                hideLabel().
                setOutputMarkupId(true).
                setOutputMarkupPlaceholderTag(true));
    }

    public boolean check() {
        boolean check = StringUtils.isBlank(captchaText.getObject())
                || StringUtils.isBlank(captchaImageResource.getChallengeId())
                ? false
                : captchaText.getObject().equals(captchaImageResource.getChallengeId());

        captchaImageResource.invalidate();
        return check;
    }
}
