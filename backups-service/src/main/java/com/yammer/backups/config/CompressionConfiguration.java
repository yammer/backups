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

import com.google.common.collect.ImmutableSet;
import com.yammer.backups.api.CompressionCodec;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal")
public class CompressionConfiguration {

    @Valid
    @NotNull
    private CompressionCodec codec = CompressionCodec.SNAPPY;

    @Valid
    @NotNull
    private Set<String> fileExtensions = ImmutableSet.of(".gz", ".bz2", ".zip");

    public CompressionCodec getCodec() {
        return codec;
    }

    public Set<String> getFileExtensions() {
        return fileExtensions;
    }
}
