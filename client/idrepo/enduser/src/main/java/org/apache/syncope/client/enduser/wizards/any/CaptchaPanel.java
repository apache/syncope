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

import java.security.SecureRandom;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;

public class CaptchaPanel<T> extends AbstractCaptchaPanel<T> {

    private static final long serialVersionUID = 1169850573252481471L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final RandomStringGenerator RANDOM_LETTERS = new RandomStringGenerator.Builder().
            usingRandom(RANDOM::nextInt).
            withinRange('a', 'z').
            build();

    public CaptchaPanel(final String id) {
        super(id);
    }

    @Override
    protected CaptchaImageResource createCaptchaImageResource() {
        return new CaptchaImageResource() {

            private static final long serialVersionUID = 2436829189992948005L;

            @Override
            protected byte[] render() {
                randomText = RANDOM_LETTERS.generate(6);
                getChallengeIdModel().setObject(randomText);
                return super.render();
            }
        };
    }

    @Override
    protected void reload() {
        this.captchaImageResource.invalidate();
    }

}
