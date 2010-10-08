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

import org.syncope.client.to.ConfigurationTO;
import org.syncope.console.rest.ConfigurationsRestClient;

/**
 * Class with utilities shared with other classes.
 */
public class Utility {
    private ConfigurationsRestClient configurationsRestClient;

    private ConfigurationTO configuration;
    /**
     * Get the rows number to display for single page, stored as configuration.
     * @param Page name
     */
    public int getPaginatorRowsToDisplay(
            String page){
       //Set rows to display to default value
       int rows = 5;

       String confProperty = null;

       if(page.equals("resources"))
           confProperty = Constants.CONF_RESOURCES_PAGINATOR_ROWS;

       else if(page.equals("configuration"))
           confProperty = Constants.CONF_CONFIGURATION_PAGINATOR_ROWS;

       else if(page.equals("users"))
           confProperty = Constants.CONF_USERS_PAGINATOR_ROWS;

       else if(page.equals("schema"))
           confProperty = Constants.CONF_SCHEMA_PAGINATOR_ROWS;

       else//Connectors final case
           confProperty = Constants.CONF_CONNECTORS_PAGINATOR_ROWS;

       configuration = configurationsRestClient.readConfiguration(
                confProperty);

       if (configuration == null || configuration.getConfValue() == null) {
            configuration = new ConfigurationTO();

            configuration.setConfKey(confProperty);
            configuration.setConfValue("5");

            configurationsRestClient.createConfiguration(configuration);

        }
       else {
           try{
           rows = new Integer(configuration.getConfValue());
           }
           catch(NumberFormatException ex) {
               configuration.setConfValue("5");
               configurationsRestClient.updateConfiguration(configuration);
               rows = 5;
           }
       }

       return rows;
    }

    public ConfigurationsRestClient getConfigurationsRestClient() {
        return configurationsRestClient;
    }

    public void setConfigurationsRestClient(ConfigurationsRestClient
            configurationsRestClient) {
        this.configurationsRestClient = configurationsRestClient;
    }
}
