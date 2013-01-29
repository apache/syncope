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
package org.apache.syncope.console.pages;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class InfoModalPage extends BaseModalPage {

    private static final long serialVersionUID = 5558354927844399580L;

    @SpringBean(name = "site")
    private String siteUrl;

    @SpringBean(name = "license")
    private String licenseUrl;

    public InfoModalPage(final String version, final String coreVersion) {
        super();

        add(new ExternalLink("syncopeLink", siteUrl));
        add(new ExternalLink("licenseLink", licenseUrl));
        add(new Label("consoleVersion", version));
        add(new Label("coreVersion", coreVersion));
    }
}
