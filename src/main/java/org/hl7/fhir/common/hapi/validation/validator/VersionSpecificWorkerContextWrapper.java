package org.hl7.fhir.common.hapi.validation.validator;

import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.Msg;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.fhir.ucum.UcumService;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.TerminologyServiceException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.ParserType;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.terminologies.ValueSetExpander;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.ValidationContextCarrier;
import org.hl7.fhir.utilities.TimeTracker;
import org.hl7.fhir.utilities.TranslationServices;
import org.hl7.fhir.utilities.i18n.I18nBase;
import org.hl7.fhir.utilities.npm.BasePackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

//!\\ Only work with the R4 fhir version

public class VersionSpecificWorkerContextWrapper extends I18nBase implements IWorkerContext {
	private static final Logger ourLog = LoggerFactory.getLogger(VersionSpecificWorkerContextWrapper.class);
	private final ValidationSupportContext myValidationSupportContext;
	private final IVersionTypeConverter myModelConverter;
	private final LoadingCache<ResourceKey, IBaseResource> myFetchResourceCache;

	public VersionSpecificWorkerContextWrapper(ValidationSupportContext theValidationSupportContext, IVersionTypeConverter theModelConverter) {
		myValidationSupportContext = theValidationSupportContext;
		myModelConverter = theModelConverter;

		myFetchResourceCache = Caffeine.newBuilder()
			.expireAfterWrite(1000, TimeUnit.MILLISECONDS)
			.maximumSize(10000)
			.build(key -> {

				String fetchResourceName = key.getResourceName();

				Class<? extends IBaseResource> fetchResourceType;
				if (fetchResourceName.equals("Resource")) {
					fetchResourceType = null;
				} else {
					fetchResourceType = myValidationSupportContext.getRootValidationSupport().getFhirContext().getResourceDefinition(fetchResourceName).getImplementingClass();
				}

				IBaseResource fetched = myValidationSupportContext.getRootValidationSupport().fetchResource(fetchResourceType, key.getUri());

				Resource canonical = myModelConverter.toCanonical(fetched);

				if (canonical instanceof StructureDefinition) {
					StructureDefinition canonicalSd = (StructureDefinition) canonical;
					if (canonicalSd.getSnapshot().isEmpty()) {
						ourLog.info("Generating snapshot for StructureDefinition: {}", canonicalSd.getUrl());
						fetched = myValidationSupportContext.getRootValidationSupport().generateSnapshot(theValidationSupportContext, fetched, "", null, "");
						Validate.isTrue(fetched != null, "StructureDefinition %s has no snapshot, and no snapshot generator is configured", key.getUri());
						canonical = myModelConverter.toCanonical(fetched);
					}
				}

				return canonical;
			});

		setValidationMessageLanguage(getLocale());
	}

	// Used for conversion
	@Override
	public List<StructureDefinition> getStructures() {
		return allStructures();
	}

	@Override
	public Locale getLocale() {
		return new Locale("fr", "CH");
	}

	// Used for conversion
	@Override
	public <T extends Resource> T fetchResource(Class<T> class_, String uri) {
		ResourceKey key = new ResourceKey(class_.getSimpleName(), uri);
		@SuppressWarnings("unchecked")
		T retVal = (T) myFetchResourceCache.get(key);

		return retVal;
	}

	// Used for conversion
	@Override
	public String getOverrideVersionNs() {
		return null;
	}

	// Used for conversion
	@Override
	public String getVersion() {
		return "4.0.1";
	}

	// Used for conversion
	@Override
	public boolean isNoTerminologyServer() {
		return false;
	}

	// Used for conversion
	@Override
	public boolean supportsSystem(String system) {
		return false;
	}

	// Used by InstanceValidator during conversion
	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, String code, org.hl7.fhir.r5.model.ValueSet theValueSet) {
		return new ValidationResult(ValidationMessage.IssueSeverity.ERROR, "Validation failed");
	}

	// Used by InstanceValidator during conversion
	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, org.hl7.fhir.r5.model.Coding theCoding, org.hl7.fhir.r5.model.ValueSet theValueSet) {
		return new ValidationResult(ValidationMessage.IssueSeverity.ERROR, "Validation failed");
	}

	// Used by InstanceValidator during conversion
	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, org.hl7.fhir.r5.model.CodeableConcept code, org.hl7.fhir.r5.model.ValueSet theVs) {
		return new ValidationResult(ValidationMessage.IssueSeverity.ERROR, "Validation failed");
	}

	public interface IVersionTypeConverter {

		org.hl7.fhir.r5.model.Resource toCanonical(IBaseResource theNonCanonical);

		IBaseResource fromCanonical(org.hl7.fhir.r5.model.Resource theCanonical);

	}

	private static class ResourceKey {
		private final int myHashCode;
		private final String myResourceName;
		private final String myUri;

		private ResourceKey(String theResourceName, String theUri) {
			myResourceName = theResourceName;
			myUri = theUri;
			myHashCode = new HashCodeBuilder(17, 37)
				.append(myResourceName)
				.append(myUri)
				.toHashCode();
		}

		// Used
		@Override
		public boolean equals(Object theO) {
			if (this == theO) {
				return true;
			}

			if (theO == null || getClass() != theO.getClass()) {
				return false;
			}

			ResourceKey that = (ResourceKey) theO;

			return new EqualsBuilder()
				.append(myResourceName, that.myResourceName)
				.append(myUri, that.myUri)
				.isEquals();
		}

		// Used
		public String getResourceName() {
			return myResourceName;
		}

		// Used
		public String getUri() {
			return myUri;
		}

		// Used
		@Override
		public int hashCode() {
			return myHashCode;
		}
	}


/************************************************** Methods not used  **************************************************/

	@Override
	public List<CanonicalResource> allConformanceResources() {
		throw new UnsupportedOperationException(Msg.code(650));
	}

	@Override
	public String getLinkForUrl(String corePath, String url) {
		throw new UnsupportedOperationException(Msg.code(651));
	}

	@Override
	public Map<String, byte[]> getBinaries() {
		throw new NotImplementedException();
	}

	@Override
	public int loadFromPackage(NpmPackage pi, IContextResourceLoader loader) throws FHIRException {
		throw new UnsupportedOperationException(Msg.code(652));
	}

	@Override
	public int loadFromPackage(NpmPackage pi, IContextResourceLoader loader, String[] types) throws FHIRException {
		throw new UnsupportedOperationException(Msg.code(653));
	}

	@Override
	public int loadFromPackageAndDependencies(NpmPackage pi, IContextResourceLoader loader, BasePackageCacheManager pcm) throws FHIRException {
		throw new UnsupportedOperationException(Msg.code(654));
	}

	@Override
	public boolean hasPackage(String id, String ver) {
		throw new UnsupportedOperationException(Msg.code(655));
	}

	@Override
	public boolean hasPackage(PackageVersion packageVersion) {
		throw new NotImplementedException();
	}

	@Override
	public PackageDetails getPackage(PackageVersion packageVersion) {
		throw new NotImplementedException();
	}

	@Override
	public int getClientRetryCount() {
		throw new UnsupportedOperationException(Msg.code(656));
	}

	@Override
	public IWorkerContext setClientRetryCount(int value) {
		throw new UnsupportedOperationException(Msg.code(657));
	}

	@Override
	public TimeTracker clock() {
		throw new NotImplementedException();
	}

	@Override
	public PackageVersion getPackageForUrl(String s) {
		throw new NotImplementedException();
	}

	@Override
	public void generateSnapshot(StructureDefinition input) throws FHIRException {
		throw new NotImplementedException();
	}

	@Override
	public void generateSnapshot(StructureDefinition theStructureDefinition, boolean theB) {
		throw new NotImplementedException();
	}

	@Override
	public org.hl7.fhir.r5.model.Parameters getExpansionParameters() {
		throw new NotImplementedException();
	}

	@Override
	public void setExpansionProfile(org.hl7.fhir.r5.model.Parameters expParameters) {
		throw new NotImplementedException();
	}

	@Override
	public List<StructureDefinition> allStructures() {
		throw new NotImplementedException();
	}

	@Override
	public void cacheResource(Resource res) {
		throw new UnsupportedOperationException(Msg.code(660));
	}

	@Override
	public void cacheResourceFromPackage(Resource res, PackageVersion packageDetails) throws FHIRException {
		throw new NotImplementedException();
	}

	@Override
	public void cachePackage(PackageDetails packageDetails, List<PackageVersion> list) {
		throw new NotImplementedException();
	}

	@Override
	public ValueSetExpander.ValueSetExpansionOutcome expandVS(org.hl7.fhir.r5.model.ValueSet source, boolean cacheOk, boolean Hierarchical) {
		throw new NotImplementedException();
	}

	@Override
	public ValueSetExpander.ValueSetExpansionOutcome expandVS(org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent binding, boolean cacheOk, boolean Hierarchical) {
		throw new UnsupportedOperationException(Msg.code(663));
	}

	@Override
	public ValueSetExpander.ValueSetExpansionOutcome expandVS(ValueSet.ConceptSetComponent inc, boolean hierarchical, boolean noInactive) throws TerminologyServiceException {
		throw new UnsupportedOperationException(Msg.code(664));
	}

	@Override
	public void setLocale(Locale locale) {
		throw new NotImplementedException();
	}

	@Override
	public org.hl7.fhir.r5.model.CodeSystem fetchCodeSystem(String system) {
		throw new NotImplementedException();
	}

	@Override
	public CodeSystem fetchCodeSystem(String system, String verison) {
		throw new NotImplementedException();
	}

	@Override
	public Resource fetchResourceById(String type, String uri) {
		throw new UnsupportedOperationException(Msg.code(666));
	}

	@Override
	public <T extends Resource> T fetchResourceWithException(Class<T> class_, String uri) throws FHIRException {
		throw new NotImplementedException();
	}

	@Override
	public <T extends Resource> T fetchResource(Class<T> class_, String uri, String version) {
		throw new NotImplementedException();
	}

	@Override
	public <T extends Resource> T fetchResource(Class<T> class_, String uri, CanonicalResource canonicalForSource) {
		throw new UnsupportedOperationException(Msg.code(668));
	}

	@Override
	public List<org.hl7.fhir.r5.model.ConceptMap> findMapsForSource(String url) {
		throw new UnsupportedOperationException(Msg.code(669));
	}


	@Override
	public String getAbbreviation(String name) {
		throw new UnsupportedOperationException(Msg.code(670));
	}

	@Override
	public IParser getParser(ParserType type) {
		throw new UnsupportedOperationException(Msg.code(671));
	}

	@Override
	public IParser getParser(String type) {
		throw new UnsupportedOperationException(Msg.code(672));
	}

	@Override
	public List<String> getResourceNames() {
		throw new NotImplementedException();
	}

	@Override
	public Set<String> getResourceNamesAsSet() {
		throw new NotImplementedException();
	}

	@Override
	public org.hl7.fhir.r5.model.StructureMap getTransform(String url) {
		throw new UnsupportedOperationException(Msg.code(673));
	}

	@Override
	public void setOverrideVersionNs(String value) {
		throw new UnsupportedOperationException(Msg.code(674));
	}

	@Override
	public StructureDefinition fetchTypeDefinition(String typeName) {
		throw new NotImplementedException();
	}

	@Override
	public StructureDefinition fetchRawProfile(String url) {
		throw new NotImplementedException();
	}

	@Override
	public List<String> getTypeNames() {
		throw new UnsupportedOperationException(Msg.code(675));
	}

	@Override
	public UcumService getUcumService() {
		throw new UnsupportedOperationException(Msg.code(676));
	}

	@Override
	public void setUcumService(UcumService ucumService) {
		throw new UnsupportedOperationException(Msg.code(677));
	}

	@Override
	public String getSpecUrl() {
		throw new UnsupportedOperationException(Msg.code(678));
	}

	@Override
	public boolean hasCache() {
		throw new UnsupportedOperationException(Msg.code(679));
	}

	@Override
	public <T extends Resource> boolean hasResource(Class<T> class_, String uri) {
		throw new UnsupportedOperationException(Msg.code(680));
	}

	@Override
	public Set<String> getCodeSystemsUsed() {
		throw new UnsupportedOperationException(Msg.code(681));
	}

	@Override
	public List<org.hl7.fhir.r5.model.StructureMap> listTransforms() {
		throw new UnsupportedOperationException(Msg.code(682));
	}

	@Override
	public IParser newJsonParser() {
		throw new UnsupportedOperationException(Msg.code(683));
	}

	@Override
	public IResourceValidator newValidator() {
		throw new UnsupportedOperationException(Msg.code(684));
	}

	@Override
	public IParser newXmlParser() {
		throw new UnsupportedOperationException(Msg.code(685));
	}

	@Override
	public String oid2Uri(String code) {
		throw new UnsupportedOperationException(Msg.code(686));
	}

	@Override
	public ILoggingService getLogger() {
		throw new NotImplementedException();
	}

	@Override
	public void setLogger(ILoggingService logger) {
		throw new UnsupportedOperationException(Msg.code(687));
	}

	@Override
	public TranslationServices translator() {
		throw new UnsupportedOperationException(Msg.code(688));
	}

	@Override
	public ValueSetExpander.ValueSetExpansionOutcome expandVS(ValueSet source, boolean cacheOk, boolean heiarchical, boolean incompleteOk) {
		throw new NotImplementedException();
	}

	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, String system, String version, String code, String display) {
		throw new NotImplementedException();
	}

	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, String theSystem, String version, String theCode, String display, ValueSet theValueSet) {
		throw new NotImplementedException();
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, Coding code, ValueSet vs, ValidationContextCarrier ctxt) {
		throw new NotImplementedException();
	}

	@Override
	public void validateCodeBatch(ValidationOptions options, List<? extends CodingValidationRequest> codes, ValueSet vs) {
		throw new NotImplementedException();
	}

	@Override
	public List<String> getCanonicalResourceNames() {
		throw new NotImplementedException();
	}
}
