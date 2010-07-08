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
package org.syncope.core.workflow.prcsiam;

import java.io.IOException;
import org.syncope.core.workflow.OSWorkflowComponent;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.Constants;

/**
 * TODO: remove ASAP!
 */
public class OpenAMCreate extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        Properties openAMProps = new Properties();
        ClientHttpResponse response = null;
        BufferedReader reader = null;
        try {
            openAMProps.load(getClass().getResourceAsStream(
                    "/WEB-INF/classes/openam.properties"));

            RestTemplate restTemplate =
                    (RestTemplate) context.getBean("restTemplate");

            ClientHttpRequest request =
                    restTemplate.getRequestFactory().createRequest(
                    new URI(openAMProps.getProperty("baseURL")
                    + "/identity/authenticate?username="
                    + openAMProps.getProperty("username")
                    + "&password="
                    + openAMProps.getProperty("password")),
                    HttpMethod.GET);
            response = request.execute();
            reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), "UTF-8"));
            String adminTokenId = reader.readLine();
            adminTokenId = adminTokenId.substring(adminTokenId.indexOf('=') + 1);
            response.getBody().close();
            reader.close();

            request = restTemplate.getRequestFactory().createRequest(
                    new URI(openAMProps.getProperty("baseURL")
                    + "/identity/create?identity_name="
                    + Utils.getUserId(syncopeUser)
                    + "&identity_attribute_names=userpassword"
                    + "&identity_attribute_values_userpassword="
                    + syncopeUser.getPassword()
                    + "&identity_attribute_names=sn&identity_attribute_values_sn="
                    + Utils.getUserId(syncopeUser)
                    + "&identity_attribute_names=cn&identity_attribute_values_cn="
                    + Utils.getUserId(syncopeUser)
                    + "&identity_attribute_names=inetuserstatus"
                    + "&identity_attribute_values_inetuserstatus="
                    + "inactive"
                    + "&identity_realm=/&identity_type=user&admin="
                    + URLEncoder.encode(adminTokenId, "UTF-8")),
                    HttpMethod.GET);
            request.execute();

            request = restTemplate.getRequestFactory().createRequest(
                    new URI(openAMProps.getProperty("baseURL")
                    + "/identity/logout?subjectid="
                    + URLEncoder.encode(adminTokenId, "UTF-8")),
                    HttpMethod.GET);
            request.execute();
        } catch (Throwable t) {
            log.error("While trying to create the user on OpenAM", t);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (response != null) {
                    response.getBody().close();
                }
            } catch (IOException e) {
            }
        }
    }
}
