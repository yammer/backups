package com.yammer.io.codec.compression;

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

import com.yammer.io.codec.StreamCodecTestWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class SnappyCompressionCodecTest {

    private static final byte[] BYTES = "Now this is the story all about how, My life got flipped, turned upside down, And I'd like to take a minute just sit right there, I'll tell you how I became the prince of a town called Bel-air.".getBytes();

    @Test
    public void testCompressDecompress() throws IOException {
        final StreamCodecTestWrapper codec = new StreamCodecTestWrapper(new SnappyCompressionCodec());

        final byte[] compressed = codec.encode(BYTES);
        Assert.assertFalse(Arrays.equals(BYTES, compressed));

        final byte[] decompressed = codec.decode(compressed);
        Assert.assertTrue(Arrays.equals(BYTES, decompressed));
    }
}
