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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import java.io.IOException;

import org.apache.syncope.ide.eclipse.plugin.Activator;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

public final class HTMLTemplateManager {

    private static final String CUSTOM_TEMPLATES_KEY
        = Activator.PLUGIN_ID + ".customtemplates";
    private static HTMLTemplateManager INSTANCE;
    private TemplateStore fStore;
    private ContributionContextTypeRegistry fRegistry;

    private HTMLTemplateManager() {
    }

    public static HTMLTemplateManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HTMLTemplateManager();
        }
        return INSTANCE;
    }

    public TemplateStore getTemplateStore() {
        if (fStore == null) {
            fStore = new ContributionTemplateStore(getContextTypeRegistry(),
                    Activator.getDefault().getPreferenceStore(), CUSTOM_TEMPLATES_KEY);
            try {
                fStore.load();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return fStore;
    }

    public ContextTypeRegistry getContextTypeRegistry() {
        if (fRegistry == null) {
            fRegistry = new ContributionContextTypeRegistry();
            fRegistry.addContextType(HTMLContextType.CONTEXT_TYPE);
        }
        return fRegistry;
    }

}
