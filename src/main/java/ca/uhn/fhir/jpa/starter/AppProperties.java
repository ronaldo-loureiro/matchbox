package ca.uhn.fhir.jpa.starter;


import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.config.DaoConfig.ClientIdStrategyEnum;
import ca.uhn.fhir.jpa.model.entity.NormalizedQuantitySearchLevel;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.google.common.collect.ImmutableList;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties
public class AppProperties {

	private boolean advanced_lucene_indexing = false;
	private boolean enable_index_of_type = false;
	private Boolean allow_contains_searches = false;
	private Boolean allow_external_references = false;
	private Boolean allow_multiple_delete = false;
	private Boolean allow_override_default_search_params = false;
	private Boolean auto_create_placeholder_reference_targets = false;
	private Boolean dao_scheduling_enabled = false;
	private Boolean delete_expunge_enabled = false;
	private Boolean enable_index_missing_fields = false;
	private Boolean enable_index_contained_resource = false;
	private Boolean enforce_referential_integrity_on_delete = false;
	private Boolean enforce_referential_integrity_on_write = false;
	private Boolean expunge_enabled = false;
	private Boolean filter_search_enabled = false;
	private Integer default_page_size = 20;
	private Integer max_page_size = Integer.MAX_VALUE;
	private Long retain_cached_searches_mins = 60L;
	private Long reuse_cached_search_results_millis = 60000L;
	private Partitioning partitioning = null;
	private NormalizedQuantitySearchLevel normalized_quantity_search_level = NormalizedQuantitySearchLevel.NORMALIZED_QUANTITY_SEARCH_NOT_SUPPORTED;
	private List<String> local_base_urls = new ArrayList<>();
	public Partitioning getPartitioning() {
		return partitioning;
	}
	public boolean getAdvanced_lucene_indexing() {
		return this.advanced_lucene_indexing;
	}
	public Boolean getAllow_contains_searches() {
		return allow_contains_searches;
	}
	public Boolean getAllow_external_references() {
		return allow_external_references;
	}
	public Boolean getAllow_multiple_delete() {
		return allow_multiple_delete;
	}
	public Boolean getAllow_override_default_search_params() {
		return allow_override_default_search_params;
	}
	public Boolean getAuto_create_placeholder_reference_targets() {
		return auto_create_placeholder_reference_targets;
	}
	public Integer getDefault_page_size() {
		return default_page_size;
	}
	public Boolean getDao_scheduling_enabled() {
		return dao_scheduling_enabled;
	}
	public Boolean getDelete_expunge_enabled() {
		return delete_expunge_enabled;
	}
	public Boolean getEnable_index_missing_fields() {
		return enable_index_missing_fields;
	}
	public Boolean getEnable_index_contained_resource() {
		return enable_index_contained_resource;
	}
	public Boolean getEnforce_referential_integrity_on_delete() {
		return enforce_referential_integrity_on_delete;
	}
	public Boolean getEnforce_referential_integrity_on_write() {
		return enforce_referential_integrity_on_write;
	}
	public Boolean getExpunge_enabled() {
		return expunge_enabled;
	}
	public Boolean getFilter_search_enabled() {
		return filter_search_enabled;
	}
	public Integer getMax_page_size() {
		return max_page_size;
	}
	public Long getRetain_cached_searches_mins() {
		return retain_cached_searches_mins;
	}
	public Long getReuse_cached_search_results_millis() {
		return reuse_cached_search_results_millis;
	}
	public NormalizedQuantitySearchLevel getNormalized_quantity_search_level() {
		return this.normalized_quantity_search_level;
	}
	public List<String> getLocal_base_urls() {
		return local_base_urls;
	}
	public boolean getEnable_index_of_type() {
		return enable_index_of_type;
	}

	public static class ImplementationGuide {
		private String url;
		private String name;
		private String version;

		public ImplementationGuide(String url, String name, String version) {
			this.url = url;
			this.name = name;
			this.version = version;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	public static class Partitioning {

		private Boolean partitioning_include_in_search_hashes = false;
		private Boolean allow_references_across_partitions = false;

		public Boolean getPartitioning_include_in_search_hashes() {
			return partitioning_include_in_search_hashes;
		}

		public Boolean getAllow_references_across_partitions() {
			return allow_references_across_partitions;
		}

	}
}
