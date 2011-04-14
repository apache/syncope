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
package org.syncope.core.rest.data;

import org.springframework.stereotype.Component;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.core.persistence.beans.SyncopeConf;

@Component
public class ConfigurationDataBinder {

    public SyncopeConf createSyncopeConfiguration(
            final ConfigurationTO configurationTO) {

        SyncopeConf syncopeConfiguration = new SyncopeConf();
        syncopeConfiguration.setKey(configurationTO.getKey());
        syncopeConfiguration.setValue(configurationTO.getValue());

        return syncopeConfiguration;
    }

    public ConfigurationTO getConfigurationTO(
            final SyncopeConf syncopeConfiguration) {

        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey(syncopeConfiguration.getKey());
        configurationTO.setValue(syncopeConfiguration.getValue());

        return configurationTO;
    }
}
