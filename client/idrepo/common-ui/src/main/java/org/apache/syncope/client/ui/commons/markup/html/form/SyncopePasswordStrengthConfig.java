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
package org.apache.syncope.client.ui.commons.markup.html.form;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthConfig;
import de.agilecoders.wicket.jquery.AbstractConfig;
import de.agilecoders.wicket.jquery.IKey;
import de.agilecoders.wicket.jquery.Key;

public class SyncopePasswordStrengthConfig extends PasswordStrengthConfig {

    private static final long serialVersionUID = -5625052394514215251L;

    public enum KeyType {
        common,
        ui,
        rules;

    }

    public SyncopePasswordStrengthConfig() {
        super();

        withProgressExtraCssClasses("pwstrengthProgress").
                withShowVerdictsInsideProgressBar(true).
                withShowProgressBar(true);
    }

    protected <T> void put(final KeyType keyType, final IKey<T> key, final T value) {
        AbstractConfig ui = (AbstractConfig) all().get(keyType.name());
        ui.put(key, value);
    }

    public PasswordStrengthConfig withProgressExtraCssClasses(final String progressExtraCssClasses) {
        put(KeyType.ui, new Key<>("progressExtraCssClasses"), progressExtraCssClasses);
        return this;
    }
}
