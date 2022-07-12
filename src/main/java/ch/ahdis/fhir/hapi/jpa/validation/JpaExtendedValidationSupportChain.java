package ch.ahdis.fhir.hapi.jpa.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.dao.JpaPersistedResourceValidationSupport;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;

public class JpaExtendedValidationSupportChain extends JpaValidationSupportChain {
	private final FhirContext myFhirContext;

	@Autowired
	@Qualifier("myJpaValidationSupport")
	public IValidationSupport myJpaValidationSupport;

	private IValidationSupport myDefaultProfileValidationSupport;

	private SnapshotGeneratingValidationSupport snapshotGeneratingValidationSupport;
	private CommonCodeSystemsTerminologyService commonCodeSystemsTerminologyService;

	public JpaExtendedValidationSupportChain(FhirContext theFhirContext) {
		super(theFhirContext);
		this.myFhirContext = theFhirContext;
	}

	@PostConstruct
	public void postConstruct() {
		snapshotGeneratingValidationSupport = new SnapshotGeneratingValidationSupport(myFhirContext);
		addValidationSupport(snapshotGeneratingValidationSupport);
		commonCodeSystemsTerminologyService = new CommonCodeSystemsTerminologyService(myFhirContext);
		addValidationSupport(commonCodeSystemsTerminologyService);

		addValidationSupport(myJpaValidationSupport);
		// matchbox 2.1.0 we moved DefaultValidaton Support down below, because it looks like myDefaultProfileValidationSupport contains a complete DICOM CodeSystem and
		// we want the one from hl7.terminology 3.1.0 (check http://localhost:8080/matchbox/fhir/CodeSystem?url=http://dicom.nema.org/resources/ontology/DCM)
		// https://github.com/ahdis/matchbox/issues/50
		myDefaultProfileValidationSupport = new DefaultProfileValidationSupport(myFhirContext);
		addValidationSupport(myDefaultProfileValidationSupport);
	}
}
