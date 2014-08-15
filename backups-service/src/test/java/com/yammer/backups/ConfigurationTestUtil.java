package com.yammer.backups;

/*
 * #%L
 * Backups
 * %%
 * Copyright (C) 2013 - 2014 Microsoft Corporation
 * %%
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
 * #L%
 */

import com.google.common.base.Optional;
import com.yammer.backups.config.BackupConfiguration;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigurationTestUtil {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ConfigurationFactory<BackupConfiguration> CONFIGURATION_FACTORY = new ConfigurationFactory<>(BackupConfiguration.class, VALIDATOR, Jackson.newObjectMapper(), "dw");
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationTestUtil.class);

    private static final String CONFIGURATION_FILENAME = "conf/backups.yml.template";
    private static final Optional<File> CONFIGURATION_FILE;

    static {
        CONFIGURATION_FILE = findConfigurationFile(CONFIGURATION_FILENAME);
    }

    private static Optional<File> findConfigurationFile(File parent, String filename) {
        if (parent == null || !parent.isDirectory()) {
            return Optional.absent();
        }

        final File file = new File(parent, filename);
        if (file.exists()) {
            LOG.debug("Found configuration file at: {}", file.getAbsolutePath());
            return Optional.of(file);
        }

        return findConfigurationFile(parent.getParentFile(), filename);
    }

    private static Optional<File> findConfigurationFile(String filename) {
        final File parent = new File(ConfigurationTestUtil.class.getResource(".").getFile());
        return findConfigurationFile(parent, filename);
    }

    public static BackupConfiguration loadConfiguration() throws IOException, ConfigurationException {
        if (!CONFIGURATION_FILE.isPresent()) {
            throw new FileNotFoundException(String.format("Unable to find %s", CONFIGURATION_FILENAME));
        }

        return CONFIGURATION_FACTORY.build(CONFIGURATION_FILE.get());
    }
}
