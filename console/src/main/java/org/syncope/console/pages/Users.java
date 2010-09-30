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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.UserTO;

import org.syncope.console.commons.SearchConditionWrapper;
import org.syncope.console.rest.SchemaRestClient;

import org.syncope.console.rest.ConfigurationsRestClient;

import org.syncope.console.rest.UsersRestClient;

/**
 * Users WebPage.
 */
public class Users extends BasePage {

    @SpringBean(name = "usersRestClient")
    UsersRestClient usersRestClient;

    @SpringBean(name = "schemaRestClient")
    SchemaRestClient schemaRestClient;

    @SpringBean(name = "configurationsRestClient")
    ConfigurationsRestClient configurationsRestClient;

    final ModalWindow createUserWin;
    final ModalWindow editUserWin;
    final ModalWindow changeAttribsViewWin;

    final int WIN_ATTRIBUTES_HEIGHT = 515;
    final int WIN_ATTRIBUTES_WIDTH = 775;

    final int WIN_USER_HEIGHT = 680;
    final int WIN_USER_WIDTH = 1133;

    WebMarkupContainer usersContainer;

    List<String> columnsList;

    /** Response flag set by the Modal Window after the operation is completed*/
    boolean operationResult = false;

    FeedbackPanel feedbackPanel;

    final ModalWindow searchUsersWin;
    List<SearchConditionWrapper> searchConditionsList;
    
    public Users(PageParameters parameters) {
        super(parameters);

        setupSearchConditions();

        add(createUserWin = new ModalWindow("createUserWin"));
        add(editUserWin = new ModalWindow("editUserWin"));
        add(changeAttribsViewWin = new ModalWindow("changeAttributesViewWin"));
        add(searchUsersWin = new ModalWindow("searchUsersWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );

        add(feedbackPanel);
        
        //table's columnsList = attributes to view
        final IModel columns = new LoadableDetachableModel() {

            protected Object load() {

                ConfigurationTO configuration = configurationsRestClient.readConfiguration("users.attributes.view");

                columnsList = new ArrayList<String>();

                if (configuration != null && !configuration.getConfValue().equals("")) {
                    String conf = configuration.getConfValue();
                    StringTokenizer st = new StringTokenizer(conf, ";");

                    while (st.hasMoreTokens()) {
                        columnsList.add(st.nextToken());
                    }
                }

                Collections.sort(columnsList);
                return columnsList;
            }
        };

        ListView columnsView = new ListView("usersSchema",columns) {

            @Override
            protected void populateItem(final ListItem item) {
                final String name =
                        (String) item.getDefaultModelObject();
                
                item.add(new Label("attribute", name));
            }
        };

        final IModel users = new LoadableDetachableModel() {

            protected Object load() {
                return usersRestClient.getAllUsers().getUsers();
            }
        };

        ListView usersView = new ListView("users",users) {

            @Override
            protected void populateItem(final ListItem item) {
                final UserTO userTO =
                        (UserTO) item.getDefaultModelObject();

                item.add(new Label("id",userTO.getId()+""));

                item.add(new Label("status",userTO.getStatus()));

                if(userTO.getToken() != null && !userTO.getToken().equals(""))
                    item.add(new Label("token",getString("tokenValued")));
                else
                    item.add(new Label("token",getString("tokenNotValued")));

                item.add(new ListView("selectedAttributes", attributesToDisplay(userTO)) {

                    @Override
                    protected void populateItem(ListItem item) {
                        AttributeWrapper attribute =
                                (AttributeWrapper) item.getDefaultModelObject();

                        for(String name : columnsList){

                             if( name.equalsIgnoreCase(attribute.getKey())) {
                                 item.add(new Label("name",attribute.getValue()));
                             }
                             else if(!name.equalsIgnoreCase(attribute.getKey())) {

                             }
                                   
                        }

                    }

                });

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final UserTO userTO =
                                (UserTO) item.getDefaultModelObject();

                        editUserWin.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                UserModalPage form = new UserModalPage
                                        (Users.this, editUserWin, userTO, false);
                                return form;
                            }
                        });

                        editUserWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        usersRestClient.deleteUser(userTO.getId()+"");

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(usersContainer);
                    }
                };

                item.add(deleteLink);
            }
        };

        usersContainer = new WebMarkupContainer("usersContainer");
        usersContainer.add(usersView);
        usersContainer.add(columnsView);
        usersContainer.setOutputMarkupId(true);

        add(usersContainer);

        createUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserWin.setInitialHeight(WIN_USER_HEIGHT);
        createUserWin.setInitialWidth(WIN_USER_WIDTH);
        createUserWin.setPageMapName("create-user-modal");
        createUserWin.setCookieName("create-user-modal");

        editUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserWin.setInitialHeight(WIN_USER_HEIGHT);
        editUserWin.setInitialWidth(WIN_USER_HEIGHT);
        editUserWin.setPageMapName("edit-user-modal");
        editUserWin.setCookieName("edit-user-modal");

        changeAttribsViewWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        changeAttribsViewWin.setInitialHeight(WIN_ATTRIBUTES_HEIGHT);
        changeAttribsViewWin.setInitialWidth(WIN_ATTRIBUTES_WIDTH);
        changeAttribsViewWin.setPageMapName("change-attribs-modal");
        changeAttribsViewWin.setCookieName("change-attribs-modal");

        searchUsersWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        searchUsersWin.setInitialHeight(WIN_USER_HEIGHT);
        searchUsersWin.setInitialWidth(WIN_USER_HEIGHT);
        searchUsersWin.setPageMapName("search-users-modal");
        searchUsersWin.setCookieName("search-users-modal");

        setWindowClosedCallback(createUserWin, usersContainer);
        setWindowClosedCallback(editUserWin, usersContainer);

        setWindowClosedCallback(createUserWin, usersContainer);
        setWindowClosedCallback(editUserWin, usersContainer);

        setWindowClosedCallback(changeAttribsViewWin, usersContainer);

        add(new AjaxLink("createUserLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        UserModalPage form = new UserModalPage(Users.this,
                                createUserWin, new UserTO(), true);
                        return form;
                    }
                });

                createUserWin.show(target);
            }
        });

        add(new AjaxLink("changeAttributesViewLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                changeAttribsViewWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DisplayAttributesModalPage form = new DisplayAttributesModalPage(Users.this, changeAttribsViewWin, true);
                        return form;
                    }
                });

                changeAttribsViewWin.show(target);
            }
        });
        
       //TAB 2 - Search section start 
       /* PLACE SEARCH CODE HERE */
        
    }

    /**
     * Return the user's attributes columnsList to display, ordered
     * @param userTO instance
     * @return attributes columnsList to view depending the selection
     */
      public List<AttributeWrapper> attributesToDisplay(UserTO user) {
        Set<AttributeTO> attributes = user.getAttributes();
        List<AttributeWrapper> attributesList = new ArrayList<AttributeWrapper>();


        ConfigurationTO configuration = configurationsRestClient.readConfiguration("users.attributes.view");

        columnsList = new ArrayList<String>();
        
        if (configuration != null && !configuration.getConfValue().equals("")) {
            String conf = configuration.getConfValue();
            StringTokenizer st = new StringTokenizer(conf, ";");

            while (st.hasMoreTokens()) {
                columnsList.add(st.nextToken());
            }
        }
        
        Collections.sort(columnsList);

        AttributeWrapper attributeWrapper = null;

            boolean found = false;
            for (String name : columnsList) {
               for (AttributeTO attribute : attributes) {
                if (name.equals(attribute.getSchema()) && !found) {
                    attributeWrapper = new AttributeWrapper();
                    attributeWrapper.setKey(attribute.getSchema());
                        for(String value : attribute.getValues()){
                            attributeWrapper.setValue(value);
                            found=true;
                        }
                    attributesList.add(attributeWrapper);
                }
            }
               //case the attribute's value is blank
               if(!found){
               attributeWrapper = new AttributeWrapper();
               attributeWrapper.setKey(name);
               attributeWrapper.setValue("");
               
               attributesList.add(attributeWrapper);
               }
               else
               found = false;
        }



        return attributesList;
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if(operationResult){
                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);
                        operationResult = false;
                    }
                    }
                });
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    /**
     * Init search conditions list.
     */
    private void setupSearchConditions() {
        searchConditionsList = new ArrayList<SearchConditionWrapper>();

        searchConditionsList.add(new SearchConditionWrapper());
    }

    /**
     * Wrapper class for displaying attribute
     */
    public class AttributeWrapper {
        String key;
        String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}