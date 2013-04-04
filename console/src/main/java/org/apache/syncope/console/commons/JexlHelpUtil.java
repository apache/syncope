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
package org.apache.syncope.console.commons;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;

public final class JexlHelpUtil {

    private static final String JEXL_SYNTAX_URL = "http://commons.apache.org/jexl/reference/syntax.html";

    private JexlHelpUtil() {
        // private constructor for static utility class
    }

    public static WebMarkupContainer getJexlHelpWebContainer(final String wicketId) {
        final WebMarkupContainer jexlHelp = new WebMarkupContainer(wicketId);
        jexlHelp.setVisible(false);
        jexlHelp.setOutputMarkupPlaceholderTag(true);
        jexlHelp.setOutputMarkupId(true);
        jexlHelp.add(new ExternalLink("jexlLink", JEXL_SYNTAX_URL));
        return jexlHelp;
    }

    public static AjaxLink<Void> getAjaxLink(final WebMarkupContainer wmc, final String wicketId) {
        AjaxLink<Void> questionMarkJexlHelp = new AjaxLink<Void>(wicketId) {

            private static final long serialVersionUID = -1838017408000591382L;

            private boolean toogle = false;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                // using bitwise inversion as suggested by 
                // http://pmd.sourceforge.net/pmd-4.2.5/rules/controversial.html#BooleanInversion
                toogle ^= true;
                wmc.setVisible(toogle);
                target.add(wmc);
            }
        };
        return questionMarkJexlHelp;
    }
}
