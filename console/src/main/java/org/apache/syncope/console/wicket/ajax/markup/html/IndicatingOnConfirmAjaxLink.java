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
package org.apache.syncope.console.wicket.ajax.markup.html;

import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;

public abstract class IndicatingOnConfirmAjaxLink<T> extends ClearIndicatingAjaxLink<T> {

    private static final long serialVersionUID = 2228670850922265663L;

    private final String msg;

    public IndicatingOnConfirmAjaxLink(final String id, final PageReference pageRef) {
        this(id, pageRef, "confirmDelete");
    }

    public IndicatingOnConfirmAjaxLink(final String id, final PageReference pageRef, final String msg) {
        super(id, pageRef);
        this.msg = msg;
    }

    @Override
    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);

        final AjaxCallListener ajaxCallListener = new AjaxCallListener() {

            private static final long serialVersionUID = 7160235486520935153L;

            @Override
            public CharSequence getPrecondition(final Component component) {
                return "if (!confirm('"
                        + getString(IndicatingOnConfirmAjaxLink.this.msg)
                        + "')) {return false;} else {return true;}";
            }
        };
        attributes.getAjaxCallListeners().add(ajaxCallListener);
    }
}
