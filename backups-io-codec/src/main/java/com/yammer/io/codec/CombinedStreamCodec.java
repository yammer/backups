package com.yammer.io.codec;

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

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CombinedStreamCodec implements StreamCodec {

    private final ImmutableList<StreamCodec> delegates;

    public CombinedStreamCodec(StreamCodec... delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
    }

    @Override
    public OutputStream output(OutputStream out) throws IOException {
        for (StreamCodec delegate : delegates) {
            out = delegate.output(out);
        }

        return out;
    }

    @Override
    public InputStream input(InputStream in) throws IOException {
        for (StreamCodec delegate : delegates) {
            in = delegate.input(in);
        }

        return in;
    }
}
