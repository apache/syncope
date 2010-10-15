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
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.core.persistence.beans.SyncopeConfiguration;

@Component
@Transactional(rollbackFor = {
    Throwable.class
})
public class ConfigurationDataBinder {

    public SyncopeConfiguration createSyncopeConfiguration(
            final ConfigurationTO configurationTO) {

        SyncopeConfiguration syncopeConfiguration = new SyncopeConfiguration();
        syncopeConfiguration.setConfKey(configurationTO.getConfKey());
        syncopeConfiguration.setConfValue(configurationTO.getConfValue());

        return syncopeConfiguration;
    }

    public ConfigurationTO getConfigurationTO(
            final SyncopeConfiguration syncopeConfiguration) {

        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setConfKey(syncopeConfiguration.getConfKey());
        configurationTO.setConfValue(syncopeConfiguration.getConfValue());

        return configurationTO;
    }
}
