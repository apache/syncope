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

package org.apache.syncope.core.services;

import java.util.List;
import org.apache.syncope.common.services.RouteService;
import org.apache.syncope.common.to.RouteTO;
import org.apache.syncope.core.rest.controller.RouteController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl extends AbstractServiceImpl implements RouteService{

    @Autowired
    private RouteController controller;
    
    @Override
    public List<RouteTO> getRoutes() {
        
        return  controller.listRoutes();
    }
    
    @Override
    public RouteTO getRoute(Long id){
        
        return controller.readRoute(id);
    }

    @Override
    public void importRoute(Long id, RouteTO route) {
        controller.updateRoute(route);
    }
    
}