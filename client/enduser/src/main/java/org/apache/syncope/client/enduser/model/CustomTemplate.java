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
import java.util.ArrayList;
import java.util.List;

public class CustomTemplate implements Serializable {

    private static final long serialVersionUID = -3870675034923683299L;

    private String templateUrl;

    private List<String> css = new ArrayList<>();

    private List<String> js = new ArrayList<>();

    public CustomTemplate() {
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(final String templateUrl) {
        this.templateUrl = templateUrl;
    }

    public List<String> getCss() {
        return css;
    }

    public void setCss(final List<String> css) {
        this.css = css;
    }

    public List<String> getJs() {
        return js;
    }

    public void setJs(final List<String> js) {
        this.js = js;
    }

    public CustomTemplate templateUrl(final String value) {
        this.templateUrl = value;
        return this;
    }

    public CustomTemplate css(final List<String> value) {
        this.css = value;
        return this;
    }

    public CustomTemplate js(final List<String> value) {
        this.js = value;
        return this;
    }

}
