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

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.console.rest.ConfigurationRestClient;

/**
 * Class with utilities shared among classes.
 */
public class Utility {

    @Autowired
    private ConfigurationRestClient confRestClient;

    /**
     * Get the rows number to display for single page, stored as configuration.
     * @param confProperty
     */
    public int getPaginatorRowsToDisplay(final String confProperty) {
        //Set rows to display to default value
        int rows = 5;

        ConfigurationTO configuration =
                confRestClient.readConfiguration(confProperty);

        if (configuration == null || configuration.getConfValue() == null) {
            configuration = new ConfigurationTO();

            configuration.setConfKey(confProperty);
            configuration.setConfValue("5");

            confRestClient.createConfiguration(configuration);
        } else {
            try {
                rows = new Integer(configuration.getConfValue());
            } catch (NumberFormatException ex) {
                configuration.setConfValue("5");
                confRestClient.updateConfiguration(configuration);
                rows = 5;
            }
        }

        return rows;
    }

    /**
     * Paginator rows values populator.
     * @return List<Integer>
     */
    public List<Integer> paginatorRowsChooser() {
        List<Integer> list = new ArrayList<Integer>();

        list.add(5);
        list.add(10);
        list.add(15);

        return list;
    }

    /**
     * Update display rows for the section specified as configuration key.
     * @param confKey
     * @param rows number to store
     */
    public void updatePaginatorRows(String confKey, int rows) {
        ConfigurationTO config = new ConfigurationTO();

        config.setConfKey(confKey);
        config.setConfValue(String.valueOf(rows));

        confRestClient.updateConfiguration(config);
    }
}
