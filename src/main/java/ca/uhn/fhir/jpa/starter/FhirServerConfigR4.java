package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.jobs.reindex.ReindexAppCtx;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.config.JpaConfig;
import ca.uhn.fhir.jpa.config.r4.JpaR4Config;
import ca.uhn.fhir.jpa.dao.JpaResourceDao;
import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ch.ahdis.fhir.hapi.jpa.validation.ImplementationGuideProvider;
import ch.ahdis.fhir.hapi.jpa.validation.JpaExtendedValidationSupportChain;
import ch.ahdis.matchbox.mappinglanguage.ConvertingWorkerContext;
import ch.ahdis.matchbox.util.MatchboxPackageInstallerImpl;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
@Import({StarterJpaConfig.class, JpaR4Config.class, ReindexAppCtx.class, JpaBatch2Config.class})
public class FhirServerConfigR4 {

	/**
	 * We override the paging provider definition so that we can customize the
	 * default/max page sizes for search results. You can set these however you
	 * want, although very large page sizes will require a lot of RAM.
	 */

	@Autowired
	FhirContext fhirContext;

	@Bean(name = "myImplementationGuideDaoR4")
	public JpaResourceDao<ImplementationGuide> daoImplementationGuideR4() {
		JpaResourceDao<ImplementationGuide> retVal;
		retVal = new JpaResourceDao<>();
		retVal.setResourceType(ImplementationGuide.class);
		retVal.setContext(fhirContext);
		return retVal;
	}

	@Bean(name = "myImplementationGuideRpR4")
	@Primary
	public ImplementationGuideProvider rpImplementationGuideR4() {
		return new ImplementationGuideProvider();
	}

	@Bean
	public ConvertingWorkerContext simpleWorkerContext() {
		try {
			return new ConvertingWorkerContext(this.jpaValidationSupportChain());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean(name = JpaConfig.JPA_VALIDATION_SUPPORT_CHAIN)
	@Primary
	public JpaExtendedValidationSupportChain jpaValidationSupportChain() {
		return new JpaExtendedValidationSupportChain();
	}

	@Bean(name = "myInstanceValidator")
	public IInstanceValidatorModule instanceValidator() {
		return new FhirInstanceValidator();
	}

	@Bean
	public MatchboxPackageInstallerImpl packageInstaller() {
		return new MatchboxPackageInstallerImpl();
	}

}
