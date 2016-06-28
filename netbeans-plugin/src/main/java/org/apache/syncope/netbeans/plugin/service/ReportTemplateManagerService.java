/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.syncope.netbeans.plugin.service;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;

public class ReportTemplateManagerService {
    
    ReportTemplateService service;
    
    public ReportTemplateManagerService(String url, String userName, 
            String password) {
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(userName,password); 
        service = syncopeClient.getService(ReportTemplateService.class);
        
    }
    
    public List<ReportTemplateTO> list(){
        return service.list();
    }
    
    public boolean create(final ReportTemplateTO reportTemplateTO){
        return Response.Status.CREATED.getStatusCode() == 
                service.create(reportTemplateTO).getStatus();
    }
    
    public ReportTemplateTO read(String key){
        return service.read(key);
    }
        
    public boolean delete(String key){
        service.delete(key);
        return true;
    }
    
    public Object getFormat(String key, ReportTemplateFormat format){
        return service.getFormat(key, format).getEntity();
    }
    
    public void setFormat(String key, ReportTemplateFormat format,
            InputStream templateIn){
        service.setFormat(key, format, templateIn);
    }
    
    public boolean removeFormat(String key, ReportTemplateFormat format){
        return false;
    }
    
}
