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


package org.apache.isis.core.metamodel.spec;


import java.util.Collections;
import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.consent2.Consent;
import org.apache.isis.core.metamodel.consent2.InteractionInvocationMethod;
import org.apache.isis.core.metamodel.consent2.InteractionResult;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.hide.HiddenFacet;
import org.apache.isis.core.metamodel.facets.naming.describedas.DescribedAsFacet;
import org.apache.isis.core.metamodel.facets.naming.named.NamedFacet;
import org.apache.isis.core.metamodel.facets.object.aggregated.AggregatedFacet;
import org.apache.isis.core.metamodel.facets.object.callbacks.CreatedCallbackFacet;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.facets.object.ident.icon.IconFacet;
import org.apache.isis.core.metamodel.facets.object.ident.plural.PluralFacet;
import org.apache.isis.core.metamodel.facets.object.ident.title.TitleFacet;
import org.apache.isis.core.metamodel.facets.object.immutable.ImmutableFacet;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.facets.object.value.ValueFacet;
import org.apache.isis.core.metamodel.interactions2.InteractionContext;
import org.apache.isis.core.metamodel.interactions2.ObjectTitleContext;
import org.apache.isis.core.metamodel.interactions2.ObjectValidityContext;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionContainer;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociationContainer;

/**
 * Represents an entity or value (cf {@link java.lang.Class}) within the 
 * metamodel.
 * 
 * <p>
 * As specifications are cyclic (specifically a class will reference its 
 * subclasses, which in turn reference their superclass) they need be created 
 * first, and then later work out its internals.  Hence we create 
 * {@link ObjectSpecification}s as we need them, and then introspect them later.
 * 
 * <p>
 * REVIEW: why is there no Help method for classes?
 */
public interface ObjectSpecification extends Specification, ObjectActionContainer, ObjectAssociationContainer, Hierarchical, Dirtiable, DefaultProvider {

    public final static List<ObjectSpecification> EMPTY_LIST = Collections.emptyList();


    /**
     * @return
     */
    Class<?> getCorrespondingClass();

    /**
     * Returns an (immutable) "full" identifier for this specification.
     * 
     * <p>
     * This will be the fully qualified name of the Class object that
     * this object represents (i.e. it includes the package name).
     */
    String getFullIdentifier();
    
    /**
     * Returns an (immutable) "short" identifier for this specification.
     * 
     * <p>
     * This will be the class name without the package; any text up to and 
     * including the last period is removed.
     */
    String getShortIdentifier();

    /**
     * Returns the (singular) name for objects of this specification.
     * 
     * <p>
     * Corresponds to the {@link NamedFacet#value()} of {@link NamedFacet}; 
     * is not necessarily immutable. 
     */
    String getSingularName();

    /**
     * Returns the plural name for objects of this specification.
     * 
     * <p>
     * Corresponds to the {@link PluralFacet#value() value} of {@link PluralFacet}; 
     * is not necessarily immutable. 
     */
    String getPluralName();
    
    /**
     * Returns the description, if any, of the specification.
     * 
     * <p>
     * Corresponds to the {@link DescribedAsFacet#value()) value} of {@link DescribedAsFacet}; 
     * is not necessarily immutable. 
     */
    @Override
    String getDescription();

    /**
     * Returns the title string for the specified object.
     * 
     * <p>
     * Corresponds to the {@link TitleFacet#value()) value} of {@link TitleFacet}; 
     * is not necessarily immutable. 
     */
    String getTitle(ObjectAdapter adapter);

    /**
     * Returns the name of an icon to use for the specified object.
     * 
     * <p>
     * Corresponds to the {@link IconFacet#iconName(ObjectAdapter)) icon name} 
     * returned by the {@link IconFacet}; 
     * is not necessarily immutable. 
     */
    String getIconName(ObjectAdapter object);
    
    boolean isAbstract();


    ////////////////////////////////////////////////////////////////
    // TitleContext
    ////////////////////////////////////////////////////////////////

    /**
     * Create an {@link InteractionContext} representing an attempt to read the object's title.
     */
    ObjectTitleContext createTitleInteractionContext(
            AuthenticationSession session,
            InteractionInvocationMethod invocationMethod,
            ObjectAdapter targetObjectAdapter);



    ////////////////////////////////////////////////////////////////
    // ValidityContext, Validity
    ////////////////////////////////////////////////////////////////

    /**
     * Create an {@link InteractionContext} representing an attempt to save the object.
     */
    ObjectValidityContext createValidityInteractionContext(
            AuthenticationSession session,
            InteractionInvocationMethod invocationMethod,
            ObjectAdapter targetObjectAdapter);

    /**
     * Determines whether the specified object is in a valid state (for example, so
     * can be persisted); represented as a {@link Consent}.
     */
    Consent isValid(ObjectAdapter adapter);

    /**
     * Determines whether the specified object is in a valid state (for example, so can
     * be persisted); represented as a {@link InteractionResult}.
     */
    InteractionResult isValidResult(ObjectAdapter adapter);


    ////////////////////////////////////////////////////////////////
    // Facets
    ////////////////////////////////////////////////////////////////

    /**
     * Determines if objects of this specification can be persisted or not. If it can be persisted (i.e. it
     * return something other than {@link Persistability}.TRANSIENT ObjectAdapter.isPersistent() will indicated
     * whether the object is persistent or not. If they cannot be persisted then {@link ObjectAdapter}.
     * {@link #persistability()} should be ignored.
     */
    Persistability persistability();

    /**
     * Determines if the object represents an value or object.
     *
     * <p>
     * In effect, means that it doesn't have the {@link CollectionFacet}, and therefore will return
     * NOT {@link #isCollection()}
     *
     * @see #isCollection().
     */
    boolean isNotCollection();

    /**
     * Determines if objects represents a collection.
     *
     * <p>
     * In effect, means has got {@link CollectionFacet}, and therefore will return NOT {@link #isNotCollection()}.
     *
     * @see #isNotCollection()
     */
    boolean isCollection();

    /**
     * Whether objects of this type are a collection or are intrinsically aggregated.
     *
     * <p>
     * In effect, means has got a {@link CollectionFacet} and/or got the {@link AggregatedFacet}.
     */
    boolean isCollectionOrIsAggregated();

    /**
     * Determines if objects of this type are values.
     *
     * <p>
     * In effect, means has got {@link ValueFacet}.
     */
    boolean isValue();

    /**
     * Determines if objects of this type are aggregated.
     *
     * <p>
     * In effect, means has got {@link AggregatedFacet} or {@link ValueFacet}.
     */
    boolean isAggregated();

    /**
     * Determines if objects of this type are either values or aggregated.
     *
     * @see #isValue()
     * @see #isAggregated()
     */
    boolean isValueOrIsAggregated();


    /**
     * Determines if objects of this type can be set up from a text entry string.
     *
     * <p>
     * In effect, means has got a {@link ParseableFacet}.
     */
    boolean isParseable();

    /**
     * Determines if objects of this type can be converted to a data-stream.
     *
     * <p>
     * In effect, means has got {@link EncodableFacet}.
     */
    boolean isEncodeable();

    /**
     * Whether has the {@link ImmutableFacet}.
     */
    boolean isImmutable();

    /**
     * Whether has the {@link HiddenFacet}
     */
    boolean isHidden();

    ////////////////////////////////////////////////////////////////
    // Creation
    ////////////////////////////////////////////////////////////////

    /**
     * Used by {@link ObjectSpecification#createObject(CreationMode)}
     */
    public enum CreationMode {
    	/**
    	 * Default all properties and call any {@link CreatedCallbackFacet created callbacks}.
    	 */
    	INITIALIZE,
    	NO_INITIALIZE
    }

    /**
     * Create and optionally {@link CreationMode#INITIALIZE initialize} object.
     */
    Object createObject(CreationMode creationMode);


    ////////////////////////////////////////////////////////////////
    // Service
    ////////////////////////////////////////////////////////////////

    boolean isService();

    public void markAsService();


    ////////////////////////////////////////////////////////////////
    // Introspection
    ////////////////////////////////////////////////////////////////

    /**
     * Builds actions and associations.
     * 
     * <p>
     * Is called prior to running the <tt>FacetDecoratorSet</tt>
     */
    public void introspectTypeHierarchyAndMembers();

    /**
     * Is called after to running the <tt>FacetDecoratorSet</tt>.
     * 
     * <p>
     * TODO: it's possible that this could be merged with {@link #introspectTypeHierarchyAndMembers()};
     * need to check though, because this would cause facets to be decorated at the end of
     * introspection, rather than midway as is currently.
     * 
     */
    public void updateFromFacetValues();

    public boolean isIntrospected();




}
