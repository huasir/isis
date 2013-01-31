/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.viewer.scimpi.dispatcher.view.action;

import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.BlockContent;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagOrderException;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.AbstractElementProcessor;

public class Parameter extends AbstractElementProcessor {

    @Override
    public void process(final TemplateProcessor templateProcessor, RequestState state) {
        final BlockContent blockContent = templateProcessor.peekBlock();
        if (!(blockContent instanceof ActionContent)) {
            throw new TagOrderException(templateProcessor);
        }

        final String field = templateProcessor.getOptionalProperty(PARAMETER_NUMBER);
        final String value = templateProcessor.getRequiredProperty(VALUE);
        final ActionContent block = (ActionContent) blockContent;
        block.setParameter(field, value);
    }

    @Override
    public String getName() {
        return "parameter";
    }

}
