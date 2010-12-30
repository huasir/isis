/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.core.metamodel.facets.actions;

import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.actions.debug.DebugFacet;
import org.apache.isis.core.metamodel.facets.actions.exploration.ExplorationFacet;
import org.apache.isis.core.metamodel.facets.actions.prototype.PrototypeFacet;
import org.apache.isis.core.metamodel.spec.ActionType;

public final class ActionTypeFacets {
    
    private ActionTypeFacets(){}

    public static ActionType getType(FacetHolder facetHolder) {
    	Facet facet = facetHolder.getFacet(DebugFacet.class);
        if (facet != null) {
            return ActionType.DEBUG;
        }
        facet = facetHolder.getFacet(ExplorationFacet.class);
        if (facet != null) {
            return ActionType.EXPLORATION;
        }
        facet = facetHolder.getFacet(PrototypeFacet.class);
        if (facet != null) {
            return ActionType.PROTOTYPE;
        }
        return ActionType.USER;
    }

}
