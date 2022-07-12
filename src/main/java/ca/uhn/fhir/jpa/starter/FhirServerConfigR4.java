package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.jobs.reindex.ReindexAppCtx;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.config.JpaConfig;
import ca.uhn.fhir.jpa.config.r4.JpaR4Config;
import ca.uhn.fhir.jpa.dao.JpaResourceDao;
import ca.uhn.fhir.jpa.dao.BaseHapiFhirResourceDao;
import org.hl7.fhir.r4.model.ImplementationGuide;
import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ch.ahdis.fhir.hapi.jpa.validation.JpaExtendedValidationSupportChain;
import ch.ahdis.matchbox.mappinglanguage.ConvertingWorkerContext;
import ch.ahdis.matchbox.util.MatchboxPackageInstallerImpl;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
@Import({ StarterJpaConfig.class, JpaR4Config.class, ReindexAppCtx.class, JpaBatch2Config.class })
public class FhirServerConfigR4 {

  /**
   * We override the paging provider definition so that we can customize the
   * default/max page sizes for search results. You can set these however you
   * want, although very large page sizes will require a lot of RAM.
   */
  @Autowired
  AppProperties appProperties;

  @Autowired
  FhirContext fhirContext;

  @Bean(name = "myImplementationGuideDaoR4")
  public IFhirResourceDao<ImplementationGuide> daoImplementationGuideR4() {
    BaseHapiFhirResourceDao<ImplementationGuide> retVal;
    retVal = new JpaResourceDao<>();
    retVal.setResourceType(ImplementationGuide.class);
    retVal.setContext(fhirContext);
    return retVal;
  }

  @Bean(name = "myImplementationGuideRpR4")
  @Primary
  public ca.uhn.fhir.jpa.rp.r4.ImplementationGuideResourceProvider rpImplementationGuideR4() {
    ca.uhn.fhir.jpa.rp.r4.ImplementationGuideResourceProvider retVal;
    retVal = new ch.ahdis.fhir.hapi.jpa.validation.ImplementationGuideProvider();
    retVal.setContext(fhirContext);
    retVal.setDao(daoImplementationGuideR4());
    return retVal;
  }

  @Bean
  public ConvertingWorkerContext simpleWorkerContext() {
    try {
      ConvertingWorkerContext conv = new ConvertingWorkerContext(this.jpaValidationSupportChain());
      return conv;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean(name = JpaConfig.JPA_VALIDATION_SUPPORT_CHAIN)
  @Primary
  public JpaExtendedValidationSupportChain jpaValidationSupportChain() {
    return new JpaExtendedValidationSupportChain(fhirContext);
  }

  @Bean(name = "myInstanceValidator")
  public IInstanceValidatorModule instanceValidator() {
    FhirInstanceValidator val = new FhirInstanceValidator(null);
    return val;
  }

  @Bean
  public MatchboxPackageInstallerImpl packageInstaller() {
    return new MatchboxPackageInstallerImpl();
  }

}
