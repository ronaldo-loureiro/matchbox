package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.batch2.jobs.reindex.ReindexAppCtx;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoStructureDefinition;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.config.JpaConfig;
import ca.uhn.fhir.jpa.config.r4.JpaR4Config;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.jpa.validation.ValidatorPolicyAdvisor;
import ca.uhn.fhir.jpa.validation.ValidatorResourceFetcher;
import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ch.ahdis.fhir.hapi.jpa.validation.JpaExtendedValidationSupportChain;
import ch.ahdis.matchbox.mappinglanguage.ConvertingWorkerContext;
import ch.ahdis.matchbox.util.MatchboxPackageInstallerImpl;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import java.io.IOException;

@Configuration
@Conditional(OnR4Condition.class)
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

  @Bean(name="myStructureDefinitionDaoR4")
  public
    IFhirResourceDaoStructureDefinition<org.hl7.fhir.r4.model.StructureDefinition>
    daoStructureDefinitionR4() {
      ca.uhn.fhir.jpa.dao.r4.FhirResourceDaoStructureDefinitionR4 retVal;
    retVal = new ca.uhn.fhir.jpa.dao.r4.FhirResourceDaoStructureDefinitionR4();
    retVal.setResourceType(org.hl7.fhir.r4.model.StructureDefinition.class);
    retVal.setContext(fhirContext);
    return retVal;
  }

  @Bean(name = "myStructureMapDaoR4")
  public IFhirResourceDao<org.hl7.fhir.r4.model.StructureMap> daoStructureMapR4() {

    ca.uhn.fhir.jpa.dao.BaseHapiFhirResourceDao<org.hl7.fhir.r4.model.StructureMap> retVal;
    retVal = new ca.uhn.fhir.jpa.dao.JpaResourceDao<org.hl7.fhir.r4.model.StructureMap>();
    retVal.setResourceType(org.hl7.fhir.r4.model.StructureMap.class);
    retVal.setContext(fhirContext);
    return retVal;
  }

  @Bean(name = "myImplementationGuideDaoR4")
  public IFhirResourceDao<org.hl7.fhir.r4.model.ImplementationGuide> daoImplementationGuideR4() {

    ca.uhn.fhir.jpa.dao.BaseHapiFhirResourceDao<org.hl7.fhir.r4.model.ImplementationGuide> retVal;
    retVal = new ca.uhn.fhir.jpa.dao.JpaResourceDao<org.hl7.fhir.r4.model.ImplementationGuide>();
    retVal.setResourceType(org.hl7.fhir.r4.model.ImplementationGuide.class);
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
      ConvertingWorkerContext conv = new ConvertingWorkerContext(this.validationSupportChain());
      return conv;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean(name = JpaConfig.JPA_VALIDATION_SUPPORT_CHAIN)
  public JpaExtendedValidationSupportChain jpaValidationSupportChain() {
    return new JpaExtendedValidationSupportChain(fhirContext);
  }

  @Bean(name = "myInstanceValidator")
  public IInstanceValidatorModule instanceValidator() {
    FhirInstanceValidator val = new FhirInstanceValidator(validationSupportChain());
    val.setValidatorResourceFetcher(jpaValidatorResourceFetcher());
    val.setValidatorPolicyAdvisor(jpaValidatorPolicyAdvisor());
    val.setBestPracticeWarningLevel(BestPracticeWarningLevel.Warning);
    return val;
  }

  @Bean
  public ValidatorResourceFetcher jpaValidatorResourceFetcher() {
    return new ValidatorResourceFetcher();
  }

  @Bean
  public ValidatorPolicyAdvisor jpaValidatorPolicyAdvisor() {
    return new ValidatorPolicyAdvisor();
  }

  @Bean
  @Primary
  public IValidationSupport validationSupportChain() {
	  return jpaValidationSupportChain();
  }

  @Bean
  public MatchboxPackageInstallerImpl packageInstaller() {
    return new MatchboxPackageInstallerImpl();
  }

}
