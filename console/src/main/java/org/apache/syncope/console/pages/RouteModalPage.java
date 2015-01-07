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

import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.RouteTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.rest.RouteRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;


public class RouteModalPage extends BaseModalPage{
    
    @SpringBean
    private RouteRestClient restClient;
    
     public RouteModalPage(final PageReference pageRef, final ModalWindow window,
            final RouteTO routeTO, final boolean createFlag){
         
        Form<RouteTO> routeForm = new Form<RouteTO>("routeDefForm");            
        
        final TextArea<String> routeDefArea = new TextArea<String>("routeContent", new PropertyModel<String>(routeTO, "routeContent"));       
        //routeDefArea.setOutputMarkupId(true);      
        
        routeForm.add(routeDefArea);
        routeForm.setModel(new CompoundPropertyModel<RouteTO>(routeTO));
        
        //routeDefArea.setMarkupId("routeContent");

        AjaxButton submit =
                new IndicatingAjaxButton(APPLY, new Model<String>(getString(SUBMIT)), routeForm) {

                    private static final long serialVersionUID = -958724007591692537L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            restClient.updateRoute(routeTO.getId(), ((RouteTO)form.getModelObject()).getRouteContent());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            
                            Configuration callerPage = (Configuration) pageRef.getPage();
                            callerPage.setModalResult(true);                            
                            window.close(target);
                        } catch (SyncopeClientException scee) {
                            error(getString(Constants.ERROR) + ": " + scee.getMessage());
                        }
                        target.add(feedbackPanel);
                    }

                    @Override
                    protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                        target.add(feedbackPanel);
                    }

                };

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                xmlRolesReader.getEntitlement("Routes", "update"));
        routeForm.add(submit);

        this.add(routeForm);
     }
}
