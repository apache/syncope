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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.syncope.client.to.UserTO;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.RestClient;

/**
 * Users WebPage.
 */
public class Users extends BasePage{
     RestClient restClient;

    public Users(PageParameters parameters) {
        super(parameters);

        add(new TextField("search",new Model(getString("search"))));

        add(new Button("newUserBtn",new Model(getString("newUserBtn"))));

         restClient = ((SyncopeApplication)getApplication()).getRestClient();

        final Set<UserTO> users = restClient.getUserList();
        System.out.println("Numero di user: "+users.size());

        if(!users.isEmpty()){


        List<UserTO> userList = new ArrayList(users);

        add(new ListView("userList", userList) {

        @Override
        protected void populateItem(ListItem item) {
        //LinkedHashMap users = (LinkedHashMap) item.getDefaultModelObject();
        item.add(new Label("label", item.getModel()));
            }
            });
            
        }

        UserTO userTO = restClient.getUser();
        System.out.println(userTO.getId());
      
    }
}
