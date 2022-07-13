package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.binary.api.IBinaryStorageSvc;
import ca.uhn.fhir.jpa.binstore.DatabaseBlobBinaryStorageSvcImpl;
import ca.uhn.fhir.jpa.config.HibernatePropertiesProvider;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.config.PartitionSettings.CrossPartitionReferenceMode;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionDeliveryHandlerFactory;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.EmailSenderImpl;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.rest.server.mail.IMailSvc;
import ca.uhn.fhir.rest.server.mail.MailConfig;
import ca.uhn.fhir.rest.server.mail.MailSvc;
import com.google.common.base.Strings;
import org.hl7.fhir.dstu2.model.Subscription;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.HashSet;
import java.util.Optional;

/**
 * This is the primary configuration file for the example server
 */
@Configuration
@EnableTransactionManagement
public class FhirServerConfigCommon {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServerConfigCommon.class);

	public FhirServerConfigCommon(AppProperties appProperties) {
		ourLog.info("Server configured to " + (appProperties.getAllow_contains_searches() ? "allow" : "deny") + " contains searches");
		ourLog.info("Server configured to " + (appProperties.getAllow_multiple_delete() ? "allow" : "deny") + " multiple deletes");
		ourLog.info("Server configured to " + (appProperties.getAllow_external_references() ? "allow" : "deny") + " external references");
		ourLog.info("Server configured to " + (appProperties.getDao_scheduling_enabled() ? "enable" : "disable") + " DAO scheduling");
		ourLog.info("Server configured to " + (appProperties.getDelete_expunge_enabled() ? "enable" : "disable") + " delete expunges");
		ourLog.info("Server configured to " + (appProperties.getExpunge_enabled() ? "enable" : "disable") + " expunges");
		ourLog.info("Server configured to " + (appProperties.getAllow_override_default_search_params() ? "allow" : "deny") + " overriding default search params");
		ourLog.info("Server configured to " + (appProperties.getAuto_create_placeholder_reference_targets() ? "allow" : "disable") + " auto-creating placeholder references");
	}

	/**
	 * Configure FHIR properties around the the JPA server via this bean
	 */
	@Bean
	public DaoConfig daoConfig(AppProperties appProperties) {
		DaoConfig retVal = new DaoConfig();

		retVal.setIndexMissingFields(appProperties.getEnable_index_missing_fields() ? DaoConfig.IndexEnabledEnum.ENABLED : DaoConfig.IndexEnabledEnum.DISABLED);
		retVal.setAutoCreatePlaceholderReferenceTargets(appProperties.getAuto_create_placeholder_reference_targets());
		retVal.setEnforceReferentialIntegrityOnWrite(appProperties.getEnforce_referential_integrity_on_write());
		retVal.setEnforceReferentialIntegrityOnDelete(appProperties.getEnforce_referential_integrity_on_delete());
		retVal.setAllowContainsSearches(appProperties.getAllow_contains_searches());
		retVal.setAllowMultipleDelete(appProperties.getAllow_multiple_delete());
		retVal.setAllowExternalReferences(appProperties.getAllow_external_references());
		retVal.setSchedulingDisabled(!appProperties.getDao_scheduling_enabled());
		retVal.setDeleteExpungeEnabled(appProperties.getDelete_expunge_enabled());
		retVal.setExpungeEnabled(appProperties.getExpunge_enabled());

		Integer maxFetchSize = appProperties.getMax_page_size();
		retVal.setFetchSizeDefaultMaximum(maxFetchSize);
		ourLog.info("Server configured to have a maximum fetch size of " + (maxFetchSize == Integer.MAX_VALUE ? "'unlimited'" : maxFetchSize));

		Long reuseCachedSearchResultsMillis = appProperties.getReuse_cached_search_results_millis();
		retVal.setReuseCachedSearchResultsForMillis(reuseCachedSearchResultsMillis);
		ourLog.info("Server configured to cache search results for {} milliseconds", reuseCachedSearchResultsMillis);

		Long retainCachedSearchesMinutes = appProperties.getRetain_cached_searches_mins();
		retVal.setExpireSearchResultsAfterMillis(retainCachedSearchesMinutes * 60 * 1000);

		retVal.setFilterParameterEnabled(appProperties.getFilter_search_enabled());
		retVal.setAdvancedLuceneIndexing(appProperties.getAdvanced_lucene_indexing());
		retVal.setTreatBaseUrlsAsLocal(new HashSet<>(appProperties.getLocal_base_urls()));

		return retVal;
	}

	@Bean
	public PartitionSettings partitionSettings(AppProperties appProperties) {
		PartitionSettings retVal = new PartitionSettings();

		// Partitioning
		if (appProperties.getPartitioning() != null) {
			retVal.setPartitioningEnabled(true);
			retVal.setIncludePartitionInSearchHashes(appProperties.getPartitioning().getPartitioning_include_in_search_hashes());
			if (appProperties.getPartitioning().getAllow_references_across_partitions()) {
				retVal.setAllowReferencesAcrossPartitions(CrossPartitionReferenceMode.ALLOWED_UNQUALIFIED);
			} else {
				retVal.setAllowReferencesAcrossPartitions(CrossPartitionReferenceMode.NOT_ALLOWED);
			}
		}

		return retVal;
	}

	@Primary
	@Bean
	public HibernatePropertiesProvider jpaStarterDialectProvider(LocalContainerEntityManagerFactoryBean myEntityManagerFactory) {
		return new JpaHibernatePropertiesProvider(myEntityManagerFactory);
	}

	@Bean
	public ModelConfig modelConfig(AppProperties appProperties, DaoConfig daoConfig) {
		ModelConfig modelConfig = daoConfig.getModelConfig();
		modelConfig.setAllowContainsSearches(appProperties.getAllow_contains_searches());
		modelConfig.setAllowExternalReferences(appProperties.getAllow_external_references());
		modelConfig.setDefaultSearchParamsCanBeOverridden(appProperties.getAllow_override_default_search_params());

		modelConfig.setNormalizedQuantitySearchLevel(appProperties.getNormalized_quantity_search_level());
		modelConfig.setIndexOnContainedResources(appProperties.getEnable_index_contained_resource());
		modelConfig.setIndexIdentifierOfType(appProperties.getEnable_index_of_type());
		return modelConfig;
	}

	@Lazy
	@Bean
	public IBinaryStorageSvc binaryStorageSvc(AppProperties appProperties) {
		DatabaseBlobBinaryStorageSvcImpl binaryStorageSvc = new DatabaseBlobBinaryStorageSvcImpl();

		if (appProperties.getMax_binary_size() != null) {
			binaryStorageSvc.setMaximumBinarySize(appProperties.getMax_binary_size());
		}

		return binaryStorageSvc;
	}
}
