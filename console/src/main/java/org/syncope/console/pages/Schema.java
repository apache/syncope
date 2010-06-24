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
package org.syncope.console.pages;

import java.util.LinkedHashMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.rest.SchemaRestClient;

/**
 * Schema WebPage.
 */
public class Schema extends BasePage
{
    @SpringBean(name = "schemaRestClient")
    SchemaRestClient restClient;

    public Schema(PageParameters parameters)
    {
        super(parameters);

        add(new ListView("attributesList",restClient.getAttributesList()) {

            @Override
            protected void populateItem(ListItem item) {
                LinkedHashMap attributeSchema = (LinkedHashMap) item.getDefaultModelObject();
            }
        });

       add(new ListView("derivedAttributesList",restClient.getDerivedAttributesList()) {

            @Override
            protected void populateItem(ListItem item) {
                LinkedHashMap attributeSchema = (LinkedHashMap) item.getDefaultModelObject();
            }
        });

        add(new ListView("userAttributesList",restClient.getUserAttributesList()) {

            @Override
            protected void populateItem(ListItem item) {
                LinkedHashMap attributeSchema = (LinkedHashMap) item.getDefaultModelObject();
            }
        });

        add(new ListView("userDerivedAttributesList",restClient.getUserDerivedAttributesList()) {

            @Override
            protected void populateItem(ListItem item) {
                LinkedHashMap attributeSchema = (LinkedHashMap) item.getDefaultModelObject();
            }
        });


    }
}
