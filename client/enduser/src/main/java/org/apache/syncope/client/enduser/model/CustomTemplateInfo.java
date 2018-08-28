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
package org.apache.syncope.client.enduser.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomTemplateInfo implements Serializable {

    private static final long serialVersionUID = -3422125754029851539L;

    private Map<String, CustomTemplate> templates = new LinkedHashMap<>();

    private CustomTemplateWizard wizard = new CustomTemplateWizard();

    private Map<String, List<String>> generalAssets = new LinkedHashMap<>();

    public CustomTemplateInfo() {
    }

    public Map<String, CustomTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(final Map<String, CustomTemplate> templates) {
        this.templates = templates;
    }

    public CustomTemplateWizard getWizard() {
        return wizard;
    }

    public void setWizard(final CustomTemplateWizard wizard) {
        this.wizard = wizard;
    }

    public Map<String, List<String>> getGeneralAssets() {
        return generalAssets;
    }

    public void setGeneralAssets(final Map<String, List<String>> generalAssets) {
        this.generalAssets = generalAssets;
    }

    public CustomTemplateInfo templates(final Map<String, CustomTemplate> templates,
            final CustomTemplateWizard wizard, final Map<String, List<String>> generalAssets) {

        this.templates = templates;
        this.wizard = wizard;
        this.generalAssets = generalAssets;
        return this;
    }

}
