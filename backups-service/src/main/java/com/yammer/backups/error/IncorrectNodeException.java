package com.yammer.backups.error;

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

public class IncorrectNodeException extends Exception {

    private static final long serialVersionUID = -4629569221556366920L;

    private final String incorrectNode;
    private final String correctNode;

    public IncorrectNodeException(String incorrectNode, String correctNode){
        this.incorrectNode = incorrectNode;
        this.correctNode = correctNode;
    }

    public String getIncorrectNode() {
        return incorrectNode;
    }

    public String getCorrectNode() {
        return correctNode;
    }
}
