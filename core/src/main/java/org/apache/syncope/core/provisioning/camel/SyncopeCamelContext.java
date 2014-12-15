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

package org.apache.syncope.core.provisioning.camel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.model.Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


@Component
public class SyncopeCamelContext{

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeCamelContext.class);

    private CamelContext camelContext = null;                   
    
    public SyncopeCamelContext() { 
    }
    
    public CamelContext getContext(RouteDAO routeDAO){

        if(camelContext == null) camelContext = new SpringCamelContext(ApplicationContextProvider.getApplicationContext());              
        if(camelContext.getRouteDefinitions().isEmpty()){            
            
            List<CamelRoute> crl = routeDAO.findAll();
            LOG.info("{} route(s) are going to be loaded ", crl.size());                
            loadContext(routeDAO, crl);
                
            try {
                camelContext.start();
            } catch (Exception ex) {
                LOG.error("Error during staring camel context {}", ex);
            }
        }
        
        return camelContext;
    }
    
    public void loadContext(RouteDAO routeDAO, List<CamelRoute> crl){
        
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            JAXBContext jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            List rds = new ArrayList();

            for (int s = 0; s < crl.size(); s++) {

                InputStream is = new ByteArrayInputStream( URLDecoder.decode(crl.get(s).getRouteContent(), "UTF-8").getBytes());
                Document doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();
                Node routeEl = doc.getElementsByTagName("route").item(0);
                JAXBElement obj = unmarshaller.unmarshal(routeEl, RouteDefinition.class);
                //adding route definition to list                        
                rds.add(obj.getValue());
            }
            camelContext.addRouteDefinitions(rds);
        } catch (Exception ex) {
            LOG.error("Error during loading camel context {}", ex);
        }
    
    }
    
    public void reloadContext(RouteDAO routeDAO){
        
        List<CamelRoute> crl = routeDAO.findAll();
        if(camelContext == null) getContext(routeDAO);
        else {            
            if( ! camelContext.getRouteDefinitions().isEmpty()){                    
                for (Iterator<RouteDefinition> it = camelContext.getRouteDefinitions().iterator(); it.hasNext(); ) {
                    RouteDefinition ard = it.next();
                    it.remove();                       
                }                    
            }

            loadContext(routeDAO, crl);
        }
    }
    
    public void reloadContext(RouteDAO routeDAO, Long routeId){
        
        if(camelContext == null) getContext(routeDAO);
        else {            
            if( ! camelContext.getRouteDefinitions().isEmpty()){
                                
                camelContext.getRouteDefinitions().remove(routeId.intValue());
                List<CamelRoute> crl = new ArrayList<CamelRoute>();
                crl.add(routeDAO.find(routeId));
                loadContext(routeDAO, crl);
            }
                
        }
            
    }
    
    public List<RouteDefinition> getDefinitions(){
        return camelContext.getRouteDefinitions();
    }
}
