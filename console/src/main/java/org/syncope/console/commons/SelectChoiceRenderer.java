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
package org.syncope.console.commons;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

public class SelectChoiceRenderer implements IChoiceRenderer {

    private static final long serialVersionUID = -3242441544405909243L;

    @Override
    public Object getDisplayValue(Object obj) {
        if (obj instanceof SelectOption) {
            return ((SelectOption) obj).getDisplayValue();
        } else {
            return obj.toString();
        }
    }

    @Override
    public String getIdValue(Object obj, int i) {
        return obj.toString();
    }
}
