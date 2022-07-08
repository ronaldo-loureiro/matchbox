package hug;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import ch.ahdis.fhir.hapi.jpa.validation.ImplementationGuideProvider;
import ch.ahdis.matchbox.mappinglanguage.ConvertingWorkerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class Config {

	@Autowired
	ConvertingWorkerContext convertingWorkerContext;
/*
	@Autowired
	private ISchedulerService mySvc;*/

	@Autowired
	private FhirContext fhirContext;

	@Autowired
	private ImplementationGuideProvider igp;

	@Bean
	public Transform transformBean() throws IOException {
		return new Transform(fhirContext, igp, convertingWorkerContext);
	}



}
