package com.yammer.backups.config;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.authenticator.LdapConfiguration;
import com.yammer.io.codec.encryption.AESCipherEncryptionConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;
import io.dropwizard.util.Size;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@SuppressWarnings("FieldCanBeLocal")
public class BackupConfiguration extends Configuration {

    public static final Duration DEFAULT_BACKUP_REQUIRED_FREQUENCY = Duration.hours(25);
    public static final Duration DEFAULT_VERIFICATION_REQUIRED_FREQUENCY = Duration.days(8);
    public static final Duration DEFAULT_TIMEOUT_DURATION = Duration.days(1);
    public static final Size DEFAULT_CHUNK_SIZE = Size.gigabytes(10);

    @Valid
    @NotNull
    @JsonProperty("local")
    private LocalConfiguration localConfiguration;

    @Valid
    @NotNull
    @JsonProperty("offsite")
    private OffsiteConfiguration offsiteConfiguration;

    @Valid
    @NotNull
    @JsonProperty("encryption")
    private AESCipherEncryptionConfiguration encryptionConfiguration;

    @Valid
    @NotNull
    @JsonProperty("node")
    private NodeConfiguration nodeConfiguration;

    @Valid
    @NotNull
    @JsonProperty("ldap")
    private LdapConfiguration ldapConfiguration;

    @Valid
    @NotNull
    @JsonProperty("lock")
    private DistributedLockConfiguration lockConfiguration = new DistributedLockConfiguration();

    @Valid
    @NotNull
    @JsonProperty("token")
    private TokenConfiguration tokenConfiguration;

    @Valid
    @NotNull
    @JsonProperty("compression")
    private CompressionConfiguration compressionConfiguration = new CompressionConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private Duration backupRequiredFrequency = DEFAULT_BACKUP_REQUIRED_FREQUENCY;

    @Valid
    @NotNull
    @JsonProperty
    private Duration verificationRequiredFrequency = DEFAULT_VERIFICATION_REQUIRED_FREQUENCY;

    @Valid
    @NotNull
    @JsonProperty
    private Duration timeoutDuration = DEFAULT_TIMEOUT_DURATION;

    @Valid
    @NotNull
    @JsonProperty
    private Size chunkSize = DEFAULT_CHUNK_SIZE;

    public LocalConfiguration getLocalConfiguration() {
        return localConfiguration;
    }

    public OffsiteConfiguration getOffsiteConfiguration() {
        return offsiteConfiguration;
    }

    public AESCipherEncryptionConfiguration getEncryptionConfiguration() {
        return encryptionConfiguration;
    }

    public Duration getBackupRequiredFrequency() {
        return backupRequiredFrequency;
    }

    public Duration getVerificationRequiredFrequency() {
        return verificationRequiredFrequency;
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public Size getChunkSize() {
        return chunkSize;
    }

    public LdapConfiguration getLdapConfiguration() {
        return ldapConfiguration;
    }

    public NodeConfiguration getNodeConfiguration() {
        return nodeConfiguration;
    }

    public DistributedLockConfiguration getLockConfiguration() {
        return lockConfiguration;
    }

    public TokenConfiguration getTokenConfiguration() {
        return tokenConfiguration;
    }

    public CompressionConfiguration getCompressionConfiguration() {
        return compressionConfiguration;
    }
}
