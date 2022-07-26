package hug;

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

	@Autowired
	private ImplementationGuideProvider igp;

	@Bean
	public Transform transformBean() throws IOException {
		return new Transform(igp, convertingWorkerContext);
	}
}
