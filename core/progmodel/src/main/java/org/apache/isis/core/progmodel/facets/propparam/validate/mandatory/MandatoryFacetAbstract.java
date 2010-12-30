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


package org.apache.isis.core.progmodel.facets.propparam.validate.mandatory;

import org.apache.isis.applib.events.ValidityEvent;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.MarkerFacetAbstract;
import org.apache.isis.core.metamodel.facets.propparam.validate.mandatory.MandatoryFacet;
import org.apache.isis.core.metamodel.interactions2.ActionArgumentContext;
import org.apache.isis.core.metamodel.interactions2.PropertyModifyContext;
import org.apache.isis.core.metamodel.interactions2.ProposedHolder;
import org.apache.isis.core.metamodel.interactions2.ValidityContext;


public abstract class MandatoryFacetAbstract extends MarkerFacetAbstract implements MandatoryFacet {

    public static Class<? extends Facet> type() {
        return MandatoryFacet.class;
    }

    public MandatoryFacetAbstract(final FacetHolder holder) {
        super(type(), holder);
    }

    public String invalidates(final ValidityContext<? extends ValidityEvent> context) {
        if (!(context instanceof PropertyModifyContext) && !(context instanceof ActionArgumentContext)) {
            return null;
        }
        if (!(context instanceof ProposedHolder)) {
            // shouldn't happen, since both the above should hold a proposed value/argument
            return null;
        }
        final ProposedHolder proposedHolder = (ProposedHolder) context;
        return isRequiredButNull(proposedHolder.getProposed()) ? "Mandatory" : null;
    }
}
