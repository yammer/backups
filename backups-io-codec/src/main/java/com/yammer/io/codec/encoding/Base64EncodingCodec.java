package com.yammer.io.codec.encoding;

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

import com.yammer.io.codec.StreamCodec;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Base64EncodingCodec implements StreamCodec {
    @Override
    public OutputStream output(OutputStream out) throws IOException {
        return new Base64OutputStream(out);
    }

    @Override
    public InputStream input(InputStream in) throws IOException {
        return new Base64InputStream(in);
    }
}
