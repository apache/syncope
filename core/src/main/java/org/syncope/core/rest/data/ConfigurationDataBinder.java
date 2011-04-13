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
import org.syncope.client.to.KeyValueTO;
import org.syncope.core.persistence.beans.SyncopeConf;

@Component
public class ConfigurationDataBinder {

    public SyncopeConf createSyncopeConfiguration(
            final KeyValueTO configurationTO) {

        SyncopeConf syncopeConfiguration = new SyncopeConf();
        syncopeConfiguration.setConfKey(configurationTO.getKey());
        syncopeConfiguration.setConfValue(configurationTO.getValue());

        return syncopeConfiguration;
    }

    public KeyValueTO getConfigurationTO(
            final SyncopeConf syncopeConfiguration) {

        KeyValueTO configurationTO = new KeyValueTO();
        configurationTO.setKey(syncopeConfiguration.getConfKey());
        configurationTO.setValue(syncopeConfiguration.getConfValue());

        return configurationTO;
    }
}
