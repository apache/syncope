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
package org.syncope.core.rest.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.validation.attrvalue.Validator;
import org.syncope.core.rest.data.ConfigurationDataBinder;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConfigurationDataBinder configurationDataBinder;

    @PreAuthorize("hasRole('CONFIGURATION_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConfigurationTO create(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO) {

        LOG.debug("Configuration create called with parameters {}",
                configurationTO);

        SyncopeConf conf = configurationDataBinder.createSyncopeConfiguration(
                configurationTO);
        conf = confDAO.save(conf);

        response.setStatus(HttpServletResponse.SC_CREATED);

        return configurationDataBinder.getConfigurationTO(conf);
    }

    @PreAuthorize("hasRole('CONFIGURATION_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{key}")
    public void delete(@PathVariable("key") final String key)
            throws MissingConfKeyException {

        confDAO.find(key);
        confDAO.delete(key);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ConfigurationTO> list(HttpServletRequest request) {
        List<SyncopeConf> configurations =
                confDAO.findAll();
        List<ConfigurationTO> configurationTOs =
                new ArrayList<ConfigurationTO>(configurations.size());

        for (SyncopeConf configuration : configurations) {
            configurationTOs.add(
                    configurationDataBinder.getConfigurationTO(configuration));
        }

        return configurationTOs;
    }

    @PreAuthorize("hasRole('CONFIGURATION_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{key}")
    public ConfigurationTO read(HttpServletResponse response,
            @PathVariable("key") String key)
            throws MissingConfKeyException {

        ConfigurationTO result;
        try {
            SyncopeConf syncopeConfiguration =
                    confDAO.find(key);
            result = configurationDataBinder.getConfigurationTO(
                    syncopeConfiguration);
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + key
                    + "', returning null");

            result = new ConfigurationTO();
            result.setKey(key);
        }

        return result;
    }

    @PreAuthorize("hasRole('CONFIGURATION_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConfigurationTO update(final HttpServletResponse response,
            @RequestBody final ConfigurationTO configurationTO)
            throws MissingConfKeyException {

        SyncopeConf syncopeConfiguration =
                confDAO.find(configurationTO.getKey());

        syncopeConfiguration.setValue(configurationTO.getValue());

        return configurationDataBinder.getConfigurationTO(syncopeConfiguration);
    }

    @PreAuthorize("hasRole('CONFIGURATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/validators")
    public ModelAndView getValidators() {
        Reflections reflections = new Reflections(
                "org.syncope.core.persistence.validation");

        Set<Class<? extends Validator>> subTypes =
                reflections.getSubTypesOf(Validator.class);

        Set<String> validators = new HashSet<String>();
        for (Class validatorClass : subTypes) {
            if (!Modifier.isAbstract(validatorClass.getModifiers())) {
                validators.add(validatorClass.getName());
            }
        }

        ModelAndView result = new ModelAndView();
        result.addObject(validators);
        return result;
    }
}
