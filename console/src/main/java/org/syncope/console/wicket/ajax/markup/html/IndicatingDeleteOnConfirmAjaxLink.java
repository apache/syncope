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
package org.syncope.console.wicket.ajax.markup.html;

import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.model.IModel;

public abstract class IndicatingDeleteOnConfirmAjaxLink<T>
        extends IndicatingAjaxLink<T> {

    public IndicatingDeleteOnConfirmAjaxLink(final String id,
            final IModel<T> model) {

        super(id, model);
    }

    public IndicatingDeleteOnConfirmAjaxLink(final String id) {
        super(id);
    }

    @Override
    protected IAjaxCallDecorator getAjaxCallDecorator() {
        return new AjaxPreprocessingCallDecorator(
                super.getAjaxCallDecorator()) {

            @Override
            public CharSequence preDecorateScript(
                    final CharSequence script) {

                return "if (confirm('"
                        + getString("confirmDelete") + "'))"
                        + "{" + script + "}";
            }
        };
    }
}
