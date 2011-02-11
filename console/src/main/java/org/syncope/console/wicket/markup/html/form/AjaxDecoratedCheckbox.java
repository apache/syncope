/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.wicket.markup.html.form;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.CancelEventIfNoAjaxDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;

/**
 * AjaxCheckBox allowing AjaxCallDecorator.
 */
public abstract class AjaxDecoratedCheckbox extends AjaxCheckBox {

    public AjaxDecoratedCheckbox(final String id) {
        this(id, null);
    }

    public AjaxDecoratedCheckbox(final String id, final IModel<Boolean> model) {
        super(id, model);

        add(new AjaxEventBehavior("onclick") {

            protected void onEvent(final AjaxRequestTarget target) {
                onUpdate(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new CancelEventIfNoAjaxDecorator(
                        AjaxDecoratedCheckbox.this.getAjaxCallDecorator());
            }
        });
    }

    /**
     * Returns ajax call decorator that will be used to decorate the ajax call.
     *
     * @return ajax call decorator
     */
    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return null;
    }
}
