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
package org.apache.syncope.fit.console;

import java.io.Serializable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;

public final class TestPage<T extends Serializable, S extends Panel>
        extends WebPage implements IMarkupResourceStreamProvider {

    private static final long serialVersionUID = 483736530078975170L;

    public static final String FIELD = "field";

    private final Form<T> form;

    private final S fieldPanel;

    private TestPage(final S field, final Builder<T, S> builder) {
        this.form = builder.form;
        this.fieldPanel = field;

        field.setOutputMarkupId(builder.outputMarkupId);
        add(form);
        form.add(field);
    }

    public Form<T> getForm() {
        return form;
    }

    public S getFieldPanel() {
        return fieldPanel;
    }

    public static class Builder<T extends Serializable, S extends Panel> implements Serializable {

        private static final long serialVersionUID = 4882978420728876617L;

        private final Form<T> form;

        private boolean outputMarkupId;

        public Builder() {
            this.form = new Form<>("form");

        }

        public Builder<T, S> setOutputMarkupId(final boolean outputMarkupId) {
            this.outputMarkupId = outputMarkupId;
            return this;
        }

        public TestPage<T, S> build(final S field) {
            return new TestPage<>(field, this);
        }
    }

    @Override
    public IResourceStream getMarkupResourceStream(final MarkupContainer container,
            final Class<?> containerClass) {
        return new StringResourceStream("<html><body>"
                + "<form wicket:id=\"form\"><span wicket:id=\"field\"></span></form></body></html>");
    }
}
