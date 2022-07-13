package org.hl7.fhir.common.hapi.validation.validator;

import ca.uhn.fhir.validation.IValidationContext;
import ca.uhn.fhir.validation.IValidatorModule;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Base class for a bridge between the RI validation tools and HAPI
 */
abstract class BaseValidatorBridge implements IValidatorModule {

	public BaseValidatorBridge() {
		super();
	}

	@Override
	public void validateResource(IValidationContext<IBaseResource> theCtx) {
	}

}
