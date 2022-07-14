package ch.ahdis.fhir.hapi.jpa.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;

public class JpaExtendedValidationSupportChain extends JpaValidationSupportChain {
	private final FhirContext myFhirContext;

	@Autowired
	@Qualifier("myJpaValidationSupport")
	public IValidationSupport myJpaValidationSupport;
	private IValidationSupport myDefaultProfileValidationSupport;

	public JpaExtendedValidationSupportChain() {
		super(FhirContext.forR4());
		this.myFhirContext = FhirContext.forR4();
	}

	@PostConstruct
	public void postConstruct() {
		addValidationSupport(myJpaValidationSupport);
		// matchbox 2.1.0 we moved DefaultValidaton Support down below, because it looks like myDefaultProfileValidationSupport contains a complete DICOM CodeSystem and
		// we want the one from hl7.terminology 3.1.0 (check http://localhost:8080/matchbox/fhir/CodeSystem?url=http://dicom.nema.org/resources/ontology/DCM)
		// https://github.com/ahdis/matchbox/issues/50
		myDefaultProfileValidationSupport = new DefaultProfileValidationSupport(myFhirContext);
		addValidationSupport(myDefaultProfileValidationSupport);
	}
}
