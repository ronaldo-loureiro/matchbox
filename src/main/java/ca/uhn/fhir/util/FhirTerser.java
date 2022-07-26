package ca.uhn.fhir.util;

import ca.uhn.fhir.context.*;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition.ChildTypeEnum;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.base.composite.BaseContainedDt;
import ca.uhn.fhir.model.base.composite.BaseResourceReferenceDt;
import ca.uhn.fhir.model.primitive.StringDt;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.*;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substring;

/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2021 Smile CDR, Inc.
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

public class FhirTerser {

	private final FhirContext myContext;

	public FhirTerser(FhirContext theContext) {
		super();
		myContext = theContext;
	}

	private List<String> addNameToList(List<String> theCurrentList, BaseRuntimeChildDefinition theChildDefinition) {
		if (theChildDefinition == null)
			return null;
		if (theCurrentList == null || theCurrentList.isEmpty())
			return new ArrayList<>(Collections.singletonList(theChildDefinition.getElementName()));
		List<String> newList = new ArrayList<>(theCurrentList);
		newList.add(theChildDefinition.getElementName());
		return newList;
	}

	/**
	 * Returns a list containing all child elements (including the resource itself) which are <b>non-empty</b> and are either of the exact type specified, or are a subclass of that type.
	 * <p>
	 * For example, specifying a type of {@link StringDt} would return all non-empty string instances within the message. Specifying a type of {@link IResource} would return the resource itself, as
	 * well as any contained resources.
	 * </p>
	 * <p>
	 * Note on scope: This method will descend into any contained resources ({@link IResource#getContained()}) as well, but will not descend into linked resources (e.g.
	 * {@link BaseResourceReferenceDt#getResource()}) or embedded resources (e.g. Bundle.entry.resource)
	 * </p>
	 *
	 * @param theResource The resource instance to search. Must not be null.
	 * @param theType     The type to search for. Must not be null.
	 * @return Returns a list of all matching elements
	 */
	public <T extends IBase> List<T> getAllPopulatedChildElementsOfType(IBaseResource theResource, final Class<T> theType) {
		final ArrayList<T> retVal = new ArrayList<>();
		BaseRuntimeElementCompositeDefinition<?> def = myContext.getResourceDefinition(theResource);
		visit(newMap(), theResource, theResource, null, null, def, new IModelVisitor() {
			@SuppressWarnings("unchecked")
			@Override
			public void acceptElement(IBaseResource theOuterResource, IBase theElement, List<String> thePathToElement, BaseRuntimeChildDefinition theChildDefinition, BaseRuntimeElementDefinition<?> theDefinition) {
				if (theElement == null || theElement.isEmpty()) {
					return;
				}

				if (theType.isAssignableFrom(theElement.getClass())) {
					retVal.add((T) theElement);
				}
			}
		});
		return retVal;
	}

	public Object getSingleValueOrNull(IBase theTarget, String thePath) {
		Class<IBase> wantedType = IBase.class;
		return getSingleValueOrNull(theTarget, thePath, wantedType);
	}

	public <T extends IBase> T getSingleValueOrNull(IBase theTarget, String thePath, Class<T> theWantedType) {
		Validate.notNull(theTarget, "theTarget must not be null");
		Validate.notBlank(thePath, "thePath must not be empty");

		BaseRuntimeElementDefinition<?> def = myContext.getElementDefinition(theTarget.getClass());
		if (!(def instanceof BaseRuntimeElementCompositeDefinition)) {
			throw new IllegalArgumentException("Target is not a composite type: " + theTarget.getClass().getName());
		}

		BaseRuntimeElementCompositeDefinition<?> currentDef = (BaseRuntimeElementCompositeDefinition<?>) def;

		List<String> parts = parsePath(currentDef, thePath);

		List<T> retVal = getValues(currentDef, theTarget, parts, theWantedType);
		if (retVal.isEmpty()) {
			return null;
		}
		return retVal.get(0);
	}

	public Optional<String> getSinglePrimitiveValue(IBase theTarget, String thePath) {
		return getSingleValue(theTarget, thePath, IPrimitiveType.class).map(t -> t.getValueAsString());
	}

	public String getSinglePrimitiveValueOrNull(IBase theTarget, String thePath) {
		return getSingleValue(theTarget, thePath, IPrimitiveType.class).map(t -> t.getValueAsString()).orElse(null);
	}

	public <T extends IBase> Optional<T> getSingleValue(IBase theTarget, String thePath, Class<T> theWantedType) {
		return Optional.ofNullable(getSingleValueOrNull(theTarget, thePath, theWantedType));
	}

	private <T extends IBase> List<T> getValues(BaseRuntimeElementCompositeDefinition<?> theCurrentDef, IBase theCurrentObj, List<String> theSubList, Class<T> theWantedClass) {
		return getValues(theCurrentDef, theCurrentObj, theSubList, theWantedClass, false, false);
	}

	@SuppressWarnings("unchecked")
	private <T extends IBase> List<T> getValues(BaseRuntimeElementCompositeDefinition<?> theCurrentDef, IBase theCurrentObj, List<String> theSubList, Class<T> theWantedClass, boolean theCreate, boolean theAddExtension) {
		String name = theSubList.get(0);
		List<T> retVal = new ArrayList<>();

		BaseRuntimeChildDefinition nextDef = theCurrentDef.getChildByNameOrThrowDataFormatException(name);
		List<? extends IBase> values = nextDef.getAccessor().getValues(theCurrentObj);

		if (theSubList.size() == 1) {
			for (IBase next : values) {
				if (next != null) {
					if (theWantedClass == null || theWantedClass.isAssignableFrom(next.getClass())) {
						retVal.add((T) next);
					}
				}
			}
		}
		return retVal;
	}

	/**
	 * Returns values stored in an element identified by its path. The list of values is of
	 * type {@link Object}.
	 *
	 * @param theElement The element to be accessed. Must not be null.
	 * @param thePath    The path for the element to be accessed.@param theElement The resource instance to be accessed. Must not be null.
	 * @return A list of values of type {@link Object}.
	 */
	public List<IBase> getValues(IBase theElement, String thePath) {
		Class<IBase> wantedClass = IBase.class;
		return getValues(theElement, thePath, wantedClass);
	}

	/**
	 * Returns values stored in an element identified by its path. The list of values is of
	 * type <code>theWantedClass</code>.
	 *
	 * @param theElement     The element to be accessed. Must not be null.
	 * @param thePath        The path for the element to be accessed.
	 * @param theWantedClass The desired class to be returned in a list.
	 * @param <T>            Type declared by <code>theWantedClass</code>
	 * @return A list of values of type <code>theWantedClass</code>.
	 */
	public <T extends IBase> List<T> getValues(IBase theElement, String thePath, Class<T> theWantedClass) {
		BaseRuntimeElementCompositeDefinition<?> def = (BaseRuntimeElementCompositeDefinition<?>) myContext.getElementDefinition(theElement.getClass());
		List<String> parts = parsePath(def, thePath);
		return getValues(def, theElement, parts, theWantedClass);
	}

	private List<String> parsePath(BaseRuntimeElementCompositeDefinition<?> theElementDef, String thePath) {
		List<String> parts = new ArrayList<>();

		int currentStart = 0;
		boolean inSingleQuote = false;
		for (int i = 0; i < thePath.length(); i++) {
			switch (thePath.charAt(i)) {
				case '\'':
					inSingleQuote = !inSingleQuote;
					break;
				case '.':
					if (!inSingleQuote) {
						parts.add(thePath.substring(currentStart, i));
						currentStart = i + 1;
					}
					break;
			}
		}

		parts.add(thePath.substring(currentStart));

		if (parts.size() > 0 && parts.get(0).equals(theElementDef.getName())) {
			parts = parts.subList(1, parts.size());
		}

		if (parts.size() < 1) {
			throw new ConfigurationException("Invalid path: " + thePath);
		}
		return parts;
	}

	public Map<Object, Object> newMap() {
		return new IdentityHashMap<>();
	}

	private void visit(Map<Object, Object> theStack, IBaseResource theResource, IBase theElement, List<String> thePathToElement, BaseRuntimeChildDefinition theChildDefinition,
							 BaseRuntimeElementDefinition<?> theDefinition, IModelVisitor theCallback) {
		List<String> pathToElement = addNameToList(thePathToElement, theChildDefinition);

		if (theStack.put(theElement, theElement) != null) {
			return;
		}

		theCallback.acceptElement(theResource, theElement, pathToElement, theChildDefinition, theDefinition);

		BaseRuntimeElementDefinition<?> def = theDefinition;
		if (def.getChildType() == ChildTypeEnum.CONTAINED_RESOURCE_LIST) {
			Class<? extends IBase> clazz = theElement.getClass();
			def = myContext.getElementDefinition(clazz);
			Validate.notNull(def, "Unable to find element definition for class: %s", clazz);
		}

		if (theElement instanceof IBaseReference) {
			IBaseResource target = ((IBaseReference) theElement).getResource();
			if (target != null) {
				if (target.getIdElement().hasIdPart() == false || target.getIdElement().isLocal()) {
					RuntimeResourceDefinition targetDef = myContext.getResourceDefinition(target);
					visit(theStack, target, target, pathToElement, null, targetDef, theCallback);
				}
			}
		}

		switch (def.getChildType()) {
			case ID_DATATYPE:
			case PRIMITIVE_XHTML_HL7ORG:
			case PRIMITIVE_XHTML:
			case PRIMITIVE_DATATYPE:
				// These are primitive types
				break;
			case RESOURCE:
			case RESOURCE_BLOCK:
			case COMPOSITE_DATATYPE: {
				BaseRuntimeElementCompositeDefinition<?> childDef = (BaseRuntimeElementCompositeDefinition<?>) def;
				List<BaseRuntimeChildDefinition> childrenAndExtensionDefs = childDef.getChildrenAndExtension();
				for (BaseRuntimeChildDefinition nextChild : childrenAndExtensionDefs) {

					List<?> values = nextChild.getAccessor().getValues(theElement);

					if (values != null) {
						for (Object nextValueObject : values) {
							IBase nextValue;
							try {
								nextValue = (IBase) nextValueObject;
							} catch (ClassCastException e) {
								String s = "Found instance of " + nextValueObject.getClass() + " - Did you set a field value to the incorrect type? Expected " + IBase.class.getName();
								throw new ClassCastException(s);
							}
							if (nextValue == null) {
								continue;
							}
							if (nextValue.isEmpty()) {
								continue;
							}
							BaseRuntimeElementDefinition<?> childElementDef;
							Class<? extends IBase> clazz = nextValue.getClass();
							childElementDef = nextChild.getChildElementDefinitionByDatatype(clazz);

							if (childElementDef == null) {
								childElementDef = myContext.getElementDefinition(clazz);
								Validate.notNull(childElementDef, "Unable to find element definition for class: %s", clazz);
							}

							if (nextChild instanceof RuntimeChildDirectResource) {
								// Don't descend into embedded resources
								theCallback.acceptElement(theResource, nextValue, null, nextChild, childElementDef);
							} else {
								visit(theStack, theResource, nextValue, pathToElement, nextChild, childElementDef, theCallback);
							}
						}
					}
				}
				break;
			}
			case CONTAINED_RESOURCES: {
				BaseContainedDt value = (BaseContainedDt) theElement;
				for (IResource next : value.getContainedResources()) {
					def = myContext.getResourceDefinition(next);
					visit(theStack, next, next, pathToElement, null, def, theCallback);
				}
				break;
			}
			case CONTAINED_RESOURCE_LIST:
			case EXTENSION_DECLARED:
			case UNDECL_EXT: {
				throw new IllegalStateException("state should not happen: " + def.getChildType());
			}
		}

		theStack.remove(theElement);

	}


	/**
	 * Iterate through the whole resource and identify any contained resources. Optionally this method
	 * can also assign IDs and modify references where the resource link has been specified but not the
	 * reference text.
	 *
	 * @since 5.4.0
	 */
	public ContainedResources containResources(IBaseResource theResource, OptionsEnum... theOptions) {
		ContainedResources contained = new ContainedResources();

		List<? extends IBaseResource> containedResources = getContainedResourceList(theResource);
		for (IBaseResource next : containedResources) {
			String nextId = next.getIdElement().getValue();
			if (StringUtils.isNotBlank(nextId)) {
				if (!nextId.startsWith("#")) {
					nextId = '#' + nextId;
				}
				next.getIdElement().setValue(nextId);
			}
			contained.addContained(next);
		}

		return contained;
	}

	@SuppressWarnings("unchecked")
	private <T extends IBaseResource> List<T> getContainedResourceList(T theResource) {
		List<T> containedResources = Collections.emptyList();
		if (theResource instanceof IResource) {
			containedResources = (List<T>) ((IResource) theResource).getContained().getContainedResources();
		} else if (theResource instanceof IDomainResource) {
			containedResources = (List<T>) ((IDomainResource) theResource).getContained();
		}
		return containedResources;
	}

	public enum OptionsEnum {

		/**
		 * Should we modify the resource in the case that contained resource IDs are assigned
		 * during a {@link #containResources(IBaseResource, OptionsEnum...)} pass.
		 */
		MODIFY_RESOURCE,

		/**
		 * Store the results of the operation in the resource metadata and reuse them if
		 * subsequent calls are made.
		 */
		STORE_AND_REUSE_RESULTS
	}

	public static class ContainedResources {
		private long myNextContainedId = 1;
		private List<IBaseResource> myResourceList;
		private IdentityHashMap<IBaseResource, IIdType> myResourceToIdMap;

		public IIdType addContained(IBaseResource theResource) {
			IIdType existing = getResourceToIdMap().get(theResource);
			if (existing != null) {
				return existing;
			}

			IIdType newId = theResource.getIdElement();
			if (isBlank(newId.getValue())) {
				newId.setValue("#" + myNextContainedId++);
			} else {
				// Avoid auto-assigned contained IDs colliding with pre-existing ones
				String idPart = newId.getValue();
				if (substring(idPart, 0, 1).equals("#")) {
					idPart = idPart.substring(1);
					if (StringUtils.isNumeric(idPart)) {
						myNextContainedId = Long.parseLong(idPart) + 1;
					}
				}
			}

			getResourceToIdMap().put(theResource, newId);
			getOrCreateResourceList().add(theResource);
			return newId;
		}

		public List<IBaseResource> getContainedResources() {
			if (getResourceToIdMap() == null) {
				return Collections.emptyList();
			}
			return getOrCreateResourceList();
		}

		public IIdType getResourceId(IBaseResource theNext) {
			if (getResourceToIdMap() == null) {
				return null;
			}
			return getResourceToIdMap().get(theNext);
		}

		private List<IBaseResource> getOrCreateResourceList() {
			if (myResourceList == null) {
				myResourceList = new ArrayList<>();
			}
			return myResourceList;
		}

		private IdentityHashMap<IBaseResource, IIdType> getResourceToIdMap() {
			if (myResourceToIdMap == null) {
				myResourceToIdMap = new IdentityHashMap<>();
			}
			return myResourceToIdMap;
		}

	}

}
