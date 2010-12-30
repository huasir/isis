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


package org.apache.isis.viewer.wicket.model.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.authentication.AuthenticationSession;
import org.apache.isis.core.metamodel.consent2.Consent;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.object.parseable.ParseableFacet;
import org.apache.isis.core.metamodel.facets.propparam.validate.mandatory.MandatoryFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.viewer.wicket.model.mementos.ActionParameterMemento;
import org.apache.isis.viewer.wicket.model.mementos.ObjectAdapterMemento;
import org.apache.isis.viewer.wicket.model.mementos.PropertyMemento;
import org.apache.isis.viewer.wicket.model.models.ScalarModel.Kind;
import org.apache.isis.viewer.wicket.model.nof.AuthenticationSessionAccessor;
import org.apache.isis.viewer.wicket.model.util.ClassLoaders;
import org.apache.wicket.Session;

/**
 * Represents a scalar of an entity, either a {@link Kind#PROPERTY property} or a
 * {@link Kind#PARAMETER parameter}.
 * 
 * <p>
 * Is the backing model to each of the fields that appear in forms (for entities or
 * action dialogs).
 */
public class ScalarModel extends EntityModel {

	private static final long serialVersionUID = 1L;

	public enum Kind {
		PROPERTY {
			@Override
			public String getName(ScalarModel scalarModel) {
				return scalarModel.getPropertyMemento().getProperty().getName();
			}

			@Override
			public ObjectSpecification getScalarTypeSpec(
					ScalarModel scalarModel) {
				return scalarModel.getPropertyMemento().getType()
						.getSpecification();
			}

			@Override
			public String getIdentifier(ScalarModel scalarModel) {
				return scalarModel.getPropertyMemento().getIdentifier();
			}

			@Override
			public String getLongName(ScalarModel scalarModel) {
				final String specShortName = scalarModel.parentObjectAdapterMemento.getSpecMemento().getSpecification().getShortIdentifier();
				return specShortName + "-" + scalarModel.getPropertyMemento().getProperty().getId();
			}

			@Override
			public String disable(ScalarModel scalarModel) {
				ObjectAdapter parentAdapter = scalarModel.parentObjectAdapterMemento
						.getObjectAdapter();
				OneToOneAssociation property = scalarModel.getPropertyMemento()
						.getProperty();
				try {
					AuthenticationSession session = scalarModel
							.getAuthenticationSession();
					Consent usable = property.isUsable(session, parentAdapter);
					return usable.isAllowed() ? null : usable.getReason();
				} catch (Exception ex) {
					return ex.getLocalizedMessage();
				}
			}

			@Override
			public String parseAndValidate(ScalarModel scalarModel,
					String proposedPojoAsStr) {
				OneToOneAssociation property = scalarModel.getPropertyMemento()
						.getProperty();
				ParseableFacet parseableFacet = property
						.getFacet(ParseableFacet.class);
				if (parseableFacet == null) {
					parseableFacet = property.getSpecification().getFacet(
							ParseableFacet.class);
				}
				try {
					ObjectAdapter parentAdapter = scalarModel.parentObjectAdapterMemento
							.getObjectAdapter();
					final ObjectAdapter currentValue = property.get(parentAdapter);
					ObjectAdapter proposedAdapter = parseableFacet
							.parseTextEntry(currentValue, proposedPojoAsStr);
					Consent valid = property.isAssociationValid(parentAdapter,
							proposedAdapter);
					return valid.isAllowed() ? null : valid.getReason();
				} catch (Exception ex) {
					return ex.getLocalizedMessage();
				}
			}

			@Override
			public String validate(ScalarModel scalarModel,
					ObjectAdapter proposedAdapter) {
				ObjectAdapter parentAdapter = scalarModel.parentObjectAdapterMemento
						.getObjectAdapter();
				OneToOneAssociation property = scalarModel.getPropertyMemento()
						.getProperty();
				try {
					Consent valid = property.isAssociationValid(parentAdapter,
							proposedAdapter);
					return valid.isAllowed() ? null : valid.getReason();
				} catch (Exception ex) {
					return ex.getLocalizedMessage();
				}
			}

			@Override
			public boolean isRequired(ScalarModel scalarModel) {
				FacetHolder facetHolder = scalarModel.getPropertyMemento().getProperty();
				return isRequired(facetHolder);
			}

			@Override
			public <T extends Facet> T getFacet(ScalarModel scalarModel, Class<T> facetType) {
				FacetHolder facetHolder = scalarModel.getPropertyMemento().getProperty();
				return facetHolder.getFacet(facetType);
			}

            @Override
            public List<ObjectAdapter> getChoices(ScalarModel scalarModel) {
                final PropertyMemento propertyMemento = scalarModel.getPropertyMemento();
                final OneToOneAssociation property = propertyMemento.getProperty();
                final ObjectAdapter[] choices = property.getChoices(scalarModel.parentObjectAdapterMemento.getObjectAdapter());
                return choicesAsList(choices);
            }
		},
		PARAMETER {
			@Override
			public String getName(ScalarModel scalarModel) {
				return scalarModel.getParameterMemento()
						.getActionParameter().getName();
			}

			@Override
			public ObjectSpecification getScalarTypeSpec(
					ScalarModel scalarModel) {
				return scalarModel.getParameterMemento()
						.getSpecification();
			}

			@Override
			public String getIdentifier(ScalarModel scalarModel) {
				return "" + scalarModel.getParameterMemento().getNumber();
			}

			@Override
			public String getLongName(ScalarModel scalarModel) {
				final ObjectAdapterMemento adapterMemento = scalarModel.getObjectAdapterMemento();
				if (adapterMemento == null) {
					// shouldn't happen
					return null;
				}
				final String specShortName = adapterMemento.getSpecMemento().getSpecification().getShortIdentifier();
				final String parmId = scalarModel.getParameterMemento().getActionParameter().getIdentifier().toNameIdentityString();
				return specShortName + "-" + parmId + "-" + scalarModel.getParameterMemento().getNumber();
			}

			@Override
			public String disable(ScalarModel scalarModel) {
				// always enabled
				return null;
			}

			@Override
			public String parseAndValidate(ScalarModel scalarModel,
					String proposedPojoAsStr) {
				ObjectActionParameter parameter = scalarModel.getParameterMemento().getActionParameter();
				ParseableFacet parseableFacet = parameter
						.getFacet(ParseableFacet.class);
				if (parseableFacet == null) {
					parseableFacet = parameter.getSpecification().getFacet(
							ParseableFacet.class);
				}
				try {
					ObjectAdapter parentAdapter = scalarModel.parentObjectAdapterMemento
							.getObjectAdapter();
					String invalidReasonIfAny = parameter.isValid(parentAdapter,
							proposedPojoAsStr);
					return invalidReasonIfAny;
				} catch (Exception ex) {
					return ex.getLocalizedMessage();
				}
			}

			@Override
			public String validate(ScalarModel scalarModel,
					ObjectAdapter proposedAdapter) {
				// TODO - not supported in NOF 4.0.0
				return null;
			}

			@Override
			public boolean isRequired(ScalarModel scalarModel) {
				FacetHolder facetHolder = scalarModel.getParameterMemento().getActionParameter();
				return isRequired(facetHolder);
			}
			

			@Override
			public <T extends Facet> T getFacet(ScalarModel scalarModel, Class<T> facetType) {
				FacetHolder facetHolder = scalarModel.getParameterMemento().getActionParameter();
				return facetHolder.getFacet(facetType);
			}

            @Override
            public List<ObjectAdapter> getChoices(ScalarModel scalarModel) {
                final ActionParameterMemento parameterMemento = scalarModel.getParameterMemento();
                final ObjectActionParameter actionParameter = parameterMemento.getActionParameter();
                final ObjectAdapter[] choices = actionParameter.getChoices(scalarModel.parentObjectAdapterMemento.getObjectAdapter());
                return choicesAsList(choices);
            }
		};
		
		private static List<ObjectAdapter> choicesAsList(ObjectAdapter[] choices) {
            if (choices != null && choices.length > 0) {
                return Arrays.asList(choices);
            }
            return Collections.emptyList();
		}

		public abstract String getName(ScalarModel scalarModel);

		public abstract ObjectSpecification getScalarTypeSpec(
				ScalarModel scalarModel);

		public abstract String getIdentifier(ScalarModel scalarModel);

		public abstract String disable(ScalarModel scalarModel);

		public abstract String parseAndValidate(
				ScalarModel scalarModel, String proposedPojoAsStr);

		public abstract String validate(ScalarModel scalarModel,
				ObjectAdapter proposedAdapter);

		public abstract String getLongName(ScalarModel scalarModel);

		public abstract boolean isRequired(ScalarModel scalarModel);

		public abstract <T extends Facet> T getFacet(ScalarModel scalarModel, Class<T> facetType);

		static boolean isRequired(FacetHolder facetHolder) {
			MandatoryFacet mandatoryFacet = facetHolder.getFacet(MandatoryFacet.class);
			boolean required = mandatoryFacet != null && !mandatoryFacet.isInvertedSemantics();
			return required;
		}

        public abstract List<ObjectAdapter> getChoices(ScalarModel scalarModel);
		
	}

	private final Kind kind;
	private final ObjectAdapterMemento parentObjectAdapterMemento;

	/**
	 * Populated only if {@link #getKind()} is {@link Kind#PARAMETER}
	 */
	private ActionParameterMemento parameterMemento;

	/**
	 * Populated only if {@link #getKind()} is {@link Kind#PROPERTY}
	 */
	private PropertyMemento propertyMemento;

    /**
     * Creates a model representing an action parameter of an action of a parent object, with the 
     * {@link #getObject() value of this model} to be default value (if any) of that action parameter.
     */
	public ScalarModel(ObjectAdapterMemento parentObjectAdapterMemento, ActionParameterMemento apm) {
		this.kind = Kind.PARAMETER;
		this.parentObjectAdapterMemento = parentObjectAdapterMemento;
		this.parameterMemento = apm;

		final ObjectActionParameter actionParameter = parameterMemento.getActionParameter();
		final ObjectAdapter defaultAdapter = 
		    actionParameter.getDefault(parentObjectAdapterMemento.getObjectAdapter());
		setObject(defaultAdapter);
		
		setMode(Mode.EDIT);
	}

	/**
	 * Creates a model representing a property of a parent object, with the 
	 * {@link #getObject() value of this model} to be current value of the property.
	 */
	public ScalarModel(ObjectAdapterMemento parentObjectAdapterMemento,
			PropertyMemento pm) {
		this.kind = Kind.PROPERTY;
		this.parentObjectAdapterMemento = parentObjectAdapterMemento;
		this.propertyMemento = pm;
		
        final OneToOneAssociation property = propertyMemento.getProperty();
        ObjectAdapter associatedAdapter = property.get(parentObjectAdapterMemento.getObjectAdapter());

		setObject(associatedAdapter);
		setMode(Mode.VIEW);
	}

	/**
	 * Whether the scalar represents a {@link Kind#PROPERTY property} or a
	 * {@link Kind#PARAMETER}.
	 */
	public Kind getKind() {
		return kind;
	}

	public String getName() {
		return kind.getName(this);
	}

	/**
	 * Populated only if {@link #getKind()} is {@link Kind#PROPERTY}
	 */
	public PropertyMemento getPropertyMemento() {
		return propertyMemento;
	}

	/**
	 * Populated only if {@link #getKind()} is {@link Kind#PARAMETER}
	 */
	public ActionParameterMemento getParameterMemento() {
		return parameterMemento;
	}

	/**
	 * Overrides superclass' implementation, because a {@link ScalarModel} can
	 * know the {@link ObjectSpecification of} the {@link ObjectAdapter 
	 * adapter} without there necessarily being any adapter being
	 * {@link #setObject(ObjectAdapter) set}.
	 */
	@Override
	public ObjectSpecification getTypeOfSpecification() {
		return kind.getScalarTypeSpec(this);
	}

	public boolean isScalarTypeAnyOf(Class<?>... requiredClass) {
		String fullName = getTypeOfSpecification().getFullIdentifier();
		for (Class<?> requiredCls : requiredClass) {
			if (fullName.equals(requiredCls.getName())) {
				return true;
			}
		}
		return false;
	}

	public Class<?> getScalarType() {
		return ClassLoaders.forName(getTypeOfSpecification());
	}

	public String getObjectAsString() {
		ObjectAdapter adapter = getObject();
		if (adapter == null) {
			return null;
		}
		return adapter.titleString();
	}

	@Override
	public void setObject(ObjectAdapter adapter) {
		super.setObject(adapter); // associated value
	}

	public void setObjectAsString(String enteredText) {
		// parse text to get adapter
		ParseableFacet parseableFacet = getTypeOfSpecification().getFacet(
				ParseableFacet.class);
		if (parseableFacet == null) {
			throw new RuntimeException("unable to parse string for "
					+ getTypeOfSpecification().getFullIdentifier());
		}
		ObjectAdapter adapter = parseableFacet.parseTextEntry(getObject(),
				enteredText);

		setObject(adapter);
	}

	public String disable() {
		return kind.disable(this);
	}

	public String parseAndValidate(String proposedPojoAsStr) {
		return kind.parseAndValidate(this, proposedPojoAsStr);
	}

	public String validate(ObjectAdapter proposedAdapter) {
		return kind.validate(this, proposedAdapter);
	}

	/**
	 * Default implementation looks up from singleton, but can be overridden for
	 * testing.
	 */
	protected AuthenticationSession getAuthenticationSession() {
		return ((AuthenticationSessionAccessor) Session.get())
				.getAuthenticationSession();
	}

	public boolean isRequired() {
		return kind.isRequired(this);
	}

	public String getLongName() {
		return kind.getLongName(this);
	}

	public <T extends Facet> T getFacet(Class<T> facetType) {
		return kind.getFacet(this, facetType);
	}

    public List<ObjectAdapter> getChoices() {
        return kind.getChoices(this);
    }

}
