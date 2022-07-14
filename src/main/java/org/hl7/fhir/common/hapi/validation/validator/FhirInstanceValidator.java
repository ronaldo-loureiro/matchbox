package org.hl7.fhir.common.hapi.validation.validator;

import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ca.uhn.fhir.validation.IValidationContext;
import org.hl7.fhir.instance.model.api.IBaseResource;

@SuppressWarnings({"PackageAccessibility", "Duplicates"})
public class FhirInstanceValidator implements IInstanceValidatorModule {

	@Override
	public void validateResource(IValidationContext<IBaseResource> theCtx) {}
}