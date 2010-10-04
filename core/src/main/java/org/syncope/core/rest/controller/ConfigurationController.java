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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.ConfigurationTOs;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
import org.syncope.core.persistence.validation.AttributeValidator;
import org.syncope.core.rest.data.ConfigurationDataBinder;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {

    @Autowired
    private SyncopeConfigurationDAO syncopeConfigurationDAO;
    @Autowired
    private ConfigurationDataBinder configurationDataBinder;

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConfigurationTO create(HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody ConfigurationTO configurationTO) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("create called with parameters " + configurationTO);
        }

        SyncopeConfiguration syncopeConfiguration =
                configurationDataBinder.createSyncopeConfiguration(
                configurationTO);

        syncopeConfiguration =
                syncopeConfigurationDAO.save(syncopeConfiguration);

        response.setStatus(HttpServletResponse.SC_CREATED);

        return configurationDataBinder.getConfigurationTO(syncopeConfiguration);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{confKey}")
    public void delete(HttpServletResponse response,
            @PathVariable("confKey") String confKey)
            throws MissingConfKeyException {

        SyncopeConfiguration syncopeConfiguration =
                syncopeConfigurationDAO.find(confKey);
        syncopeConfigurationDAO.delete(confKey);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ConfigurationTOs list(HttpServletRequest request) {
        List<SyncopeConfiguration> configurations =
                syncopeConfigurationDAO.findAll();
        List<ConfigurationTO> configurationTOs =
                new ArrayList<ConfigurationTO>(configurations.size());

        for (SyncopeConfiguration configuration : configurations) {
            configurationTOs.add(
                    configurationDataBinder.getConfigurationTO(configuration));
        }

        ConfigurationTOs result = new ConfigurationTOs();
        result.setConfigurations(configurationTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{confKey}")
    public ConfigurationTO read(HttpServletResponse response,
            @PathVariable("confKey") String confKey)
            throws MissingConfKeyException {

        ConfigurationTO result = null;
        try {
            SyncopeConfiguration syncopeConfiguration =
                    syncopeConfigurationDAO.find(confKey);
            result = configurationDataBinder.getConfigurationTO(
                    syncopeConfiguration);
        } catch (MissingConfKeyException e) {
            LOG.error("Could not find configuration key '" + confKey
                    + "', returning null");

            result = new ConfigurationTO();
            result.setConfKey(confKey);
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConfigurationTO update(HttpServletResponse response,
            @RequestBody ConfigurationTO configurationTO)
            throws MissingConfKeyException {

        SyncopeConfiguration syncopeConfiguration =
                syncopeConfigurationDAO.find(configurationTO.getConfKey());

        syncopeConfiguration.setConfValue(configurationTO.getConfValue());

        return configurationDataBinder.getConfigurationTO(syncopeConfiguration);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/validators")
    public ModelAndView getValidators() {
        Reflections reflections = new Reflections(
                "org.syncope.core.persistence.validation");

        Set<Class<? extends AttributeValidator>> subTypes =
                reflections.getSubTypesOf(AttributeValidator.class);

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
