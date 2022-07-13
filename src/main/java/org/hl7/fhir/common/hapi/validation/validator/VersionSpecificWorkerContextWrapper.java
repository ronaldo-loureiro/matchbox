package org.hl7.fhir.common.hapi.validation.validator;

import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

//!\\ Only work with the R4 fhir version

public class VersionSpecificWorkerContextWrapper extends I18nBase implements IWorkerContext {
	private static final Logger ourLog = LoggerFactory.getLogger(VersionSpecificWorkerContextWrapper.class);
	private final ValidationSupportContext myValidationSupportContext;
	private final IVersionTypeConverter myModelConverter;
	private final LoadingCache<ResourceKey, IBaseResource> myFetchResourceCache;
	private org.hl7.fhir.r5.model.Parameters myExpansionProfile;

	public VersionSpecificWorkerContextWrapper(ValidationSupportContext theValidationSupportContext, IVersionTypeConverter theModelConverter) {
		myValidationSupportContext = theValidationSupportContext;
		myModelConverter = theModelConverter;

		// see https://github.com/ahdis/matchbox/issues/51, otherwise an update of a structuredefinition takes 10 second
		// long timeoutMillis = 10 * DateUtils.MILLIS_PER_SECOND;
		long timeoutMillis = 1 * DateUtils.MILLIS_PER_SECOND;
		if (System.getProperties().containsKey(ca.uhn.fhir.rest.api.Constants.TEST_SYSTEM_PROP_VALIDATION_RESOURCE_CACHES_MS)) {
			timeoutMillis = Long.parseLong(System.getProperty(Constants.TEST_SYSTEM_PROP_VALIDATION_RESOURCE_CACHES_MS));
		}

		myFetchResourceCache = Caffeine.newBuilder()
			.expireAfterWrite(timeoutMillis, TimeUnit.MILLISECONDS)
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

	public static ConceptValidationOptions convertConceptValidationOptions(ValidationOptions theOptions) {
		ConceptValidationOptions retVal = new ConceptValidationOptions();
		if (theOptions.isGuessSystem()) {
			retVal = retVal.setInferSystem(true);
		}
		return retVal;
	}

	@Nonnull
	public static VersionSpecificWorkerContextWrapper newVersionSpecificWorkerContextWrapper(IValidationSupport theValidationSupport) {
		return new VersionSpecificWorkerContextWrapper(new ValidationSupportContext(theValidationSupport), new VersionTypeConverterR4());
	}

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
		return null;
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
		return false;
	}

	@Override
	public PackageDetails getPackage(PackageVersion packageVersion) {
		return null;
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
		return null;
	}

	@Override
	public PackageVersion getPackageForUrl(String s) {
		return null;
	}

	@Override
	public void generateSnapshot(StructureDefinition input) throws FHIRException {
		throw new NotImplementedException();
	}

	@Override
	public void generateSnapshot(StructureDefinition theStructureDefinition, boolean theB) {
		// nothing yet
	}

	@Override
	public org.hl7.fhir.r5.model.Parameters getExpansionParameters() {
		return myExpansionProfile;
	}

	@Override
	public void setExpansionProfile(org.hl7.fhir.r5.model.Parameters expParameters) {
		myExpansionProfile = expParameters;
	}

	@Override
	public List<StructureDefinition> allStructures() {
		throw new NotImplementedException();
	}

	@Override
	public List<StructureDefinition> getStructures() {
		return allStructures();
	}

	@Override
	public void cacheResource(Resource res) {
		throw new UnsupportedOperationException(Msg.code(660));
	}

	@Override
	public void cacheResourceFromPackage(Resource res, PackageVersion packageDetails) throws FHIRException {

	}

	@Override
	public void cachePackage(PackageDetails packageDetails, List<PackageVersion> list) {

	}

	@Nonnull
	private ValidationResult convertValidationResult(String theSystem, @Nullable IValidationSupport.CodeValidationResult theResult) {
		ValidationResult retVal = null;
		if (theResult != null) {
			String code = theResult.getCode();
			String display = theResult.getDisplay();
			String issueSeverity = theResult.getSeverityCode();
			String message = theResult.getMessage();
			if (isNotBlank(code)) {
				retVal = new ValidationResult(theSystem, new org.hl7.fhir.r5.model.CodeSystem.ConceptDefinitionComponent()
					.setCode(code)
					.setDisplay(display));
			} else if (isNotBlank(issueSeverity)) {
				retVal = new ValidationResult(ValidationMessage.IssueSeverity.fromCode(issueSeverity), message, ValueSetExpander.TerminologyServiceErrorClass.UNKNOWN);
			}

		}

		if (retVal == null) {
			retVal = new ValidationResult(ValidationMessage.IssueSeverity.ERROR, "Validation failed");
		}

		return retVal;
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
	public Locale getLocale() {
		return myValidationSupportContext.getRootValidationSupport().getFhirContext().getLocalizer().getLocale();
	}

	@Override
	public void setLocale(Locale locale) {
		// ignore
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
	public <T extends Resource> T fetchResource(Class<T> class_, String uri) {

		if (isBlank(uri)) {
			return null;
		}

		ResourceKey key = new ResourceKey(class_.getSimpleName(), uri);
		@SuppressWarnings("unchecked")
		T retVal = (T) myFetchResourceCache.get(key);

		return retVal;
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
		return fetchResource(class_, uri + "|" + version);
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
		return new ArrayList<>(myValidationSupportContext.getRootValidationSupport().getFhirContext().getResourceTypes());
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
	public String getOverrideVersionNs() {
		return null;
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
	public String getVersion() {
		return myValidationSupportContext.getRootValidationSupport().getFhirContext().getVersion().getVersion().getFhirVersionString();
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
	public boolean isNoTerminologyServer() {
		return false;
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
		return null;
	}

	@Override
	public void setLogger(ILoggingService logger) {
		throw new UnsupportedOperationException(Msg.code(687));
	}

	@Override
	public boolean supportsSystem(String system) {
		return myValidationSupportContext.getRootValidationSupport().isCodeSystemSupported(myValidationSupportContext, system);
	}

	@Override
	public TranslationServices translator() {
		throw new UnsupportedOperationException(Msg.code(688));
	}

	@Override
	public ValueSetExpander.ValueSetExpansionOutcome expandVS(ValueSet source, boolean cacheOk, boolean heiarchical, boolean incompleteOk) {
		return null;
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
	public ValidationResult validateCode(ValidationOptions theOptions, String code, org.hl7.fhir.r5.model.ValueSet theValueSet) {
		IBaseResource convertedVs = null;
		try {
			if (theValueSet != null) {
				convertedVs = myModelConverter.fromCanonical(theValueSet);
			}
		} catch (FHIRException e) {
			throw new InternalErrorException(Msg.code(690) + e);
		}

		ConceptValidationOptions validationOptions = convertConceptValidationOptions(theOptions).setInferSystem(true);

		return doValidation(convertedVs, validationOptions, null, code, null);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, org.hl7.fhir.r5.model.Coding theCoding, org.hl7.fhir.r5.model.ValueSet theValueSet) {
		IBaseResource convertedVs = null;

		try {
			if (theValueSet != null) {
				convertedVs = myModelConverter.fromCanonical(theValueSet);
			}
		} catch (FHIRException e) {
			throw new InternalErrorException(Msg.code(691) + e);
		}

		ConceptValidationOptions validationOptions = convertConceptValidationOptions(theOptions);
		String system = theCoding.getSystem();
		String code = theCoding.getCode();
		String display = theCoding.getDisplay();

		return doValidation(convertedVs, validationOptions, system, code, display);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions options, Coding code, ValueSet vs, ValidationContextCarrier ctxt) {
		return validateCode(options, code, vs);
	}

	@Override
	public void validateCodeBatch(ValidationOptions options, List<? extends CodingValidationRequest> codes, ValueSet vs) {
		for (CodingValidationRequest next : codes) {
			ValidationResult outcome = validateCode(options, next.getCoding(), vs);
			next.setResult(outcome);
		}
	}

	@Nonnull
	private ValidationResult doValidation(IBaseResource theValueSet, ConceptValidationOptions theValidationOptions, String theSystem, String theCode, String theDisplay) {
		IValidationSupport.CodeValidationResult result;
		if (theValueSet != null) {
			result = myValidationSupportContext.getRootValidationSupport().validateCodeInValueSet(myValidationSupportContext, theValidationOptions, theSystem, theCode, theDisplay, theValueSet);
		} else {
			result = myValidationSupportContext.getRootValidationSupport().validateCode(myValidationSupportContext, theValidationOptions, theSystem, theCode, theDisplay, null);
		}
		return convertValidationResult(theSystem, result);
	}

	@Override
	public ValidationResult validateCode(ValidationOptions theOptions, org.hl7.fhir.r5.model.CodeableConcept code, org.hl7.fhir.r5.model.ValueSet theVs) {
		for (Coding next : code.getCoding()) {
			ValidationResult retVal = validateCode(theOptions, next, theVs);
			if (retVal.isOk()) {
				return retVal;
			}
		}

		return new ValidationResult(ValidationMessage.IssueSeverity.ERROR, null);
	}

	@Override
	public List<String> getCanonicalResourceNames() {
		return null;
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

		public String getResourceName() {
			return myResourceName;
		}

		public String getUri() {
			return myUri;
		}

		@Override
		public int hashCode() {
			return myHashCode;
		}
	}
}



