package org.hl7.fhir.common.hapi.validation.validator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.TypeDetails;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.hl7.fhir.r5.utils.validation.IValidationPolicyAdvisor;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.IInstanceValidatorModule;
import ca.uhn.fhir.validation.IValidationContext;
import ch.ahdis.matchbox.mappinglanguage.ConvertingWorkerContext;

@SuppressWarnings({"PackageAccessibility", "Duplicates"})
public class FhirInstanceValidator extends BaseValidatorBridge implements IInstanceValidatorModule {

	private boolean myAnyExtensionsAllowed = true;
	private BestPracticeWarningLevel myBestPracticeWarningLevel;
	private IValidationSupport myValidationSupport;
	private boolean noTerminologyChecks = false;
	private boolean noExtensibleWarnings = false;
	private boolean noBindingMsgSuppressed = false;
	private volatile VersionSpecificWorkerContextWrapper myWrappedWorkerContext;
	private boolean errorForUnknownProfiles = true;
	private List<String> myExtensionDomains = Collections.emptyList();
	private IValidatorResourceFetcher validatorResourceFetcher;
	private IValidationPolicyAdvisor validatorPolicyAdvisor;

	/**
	 * Constructor which uses the given validation support
	 *
	 * @param theValidationSupport The validation support
	 */
	public FhirInstanceValidator(IValidationSupport theValidationSupport) {
		myValidationSupport = theValidationSupport;
	}

	/**
	 * Returns the "best practice" warning level (default is {@link BestPracticeWarningLevel#Hint}).
	 * <p>
	 * The FHIR Instance Validator has a number of checks for best practices in terms of FHIR usage. If this setting is
	 * set to {@link BestPracticeWarningLevel#Error}, any resource data which does not meet these best practices will be
	 * reported at the ERROR level. If this setting is set to {@link BestPracticeWarningLevel#Ignore}, best practice
	 * guielines will be ignored.
	 * </p>
	 *
	 * @see #setBestPracticeWarningLevel(BestPracticeWarningLevel)
	 */
	public BestPracticeWarningLevel getBestPracticeWarningLevel() {
		return myBestPracticeWarningLevel;
	}

	/**
	 * Sets the "best practice warning level". When validating, any deviations from best practices will be reported at
	 * this level.
	 * <p>
	 * The FHIR Instance Validator has a number of checks for best practices in terms of FHIR usage. If this setting is
	 * set to {@link BestPracticeWarningLevel#Error}, any resource data which does not meet these best practices will be
	 * reported at the ERROR level. If this setting is set to {@link BestPracticeWarningLevel#Ignore}, best practice
	 * guielines will be ignored.
	 * </p>
	 *
	 * @param theBestPracticeWarningLevel The level, must not be <code>null</code>
	 */
	public void setBestPracticeWarningLevel(BestPracticeWarningLevel theBestPracticeWarningLevel) {
		Validate.notNull(theBestPracticeWarningLevel);
		myBestPracticeWarningLevel = theBestPracticeWarningLevel;
	}

	/**
	 * If set to {@literal true} (default is true) extensions which are not known to the
	 * validator (e.g. because they have not been explicitly declared in a profile) will
	 * be validated but will not cause an error.
	 */
	public boolean isAnyExtensionsAllowed() {
		return myAnyExtensionsAllowed;
	}

	public boolean isErrorForUnknownProfiles() {
		return errorForUnknownProfiles;
	}

	public void setErrorForUnknownProfiles(boolean errorForUnknownProfiles) {
		this.errorForUnknownProfiles = errorForUnknownProfiles;
	}

	/**
	 * If set to {@literal true} (default is false) the valueSet will not be validate
	 */
	public boolean isNoTerminologyChecks() {
		return noTerminologyChecks;
	}

	/**
	 * If set to {@literal true} (default is false) the valueSet will not be validate
	 */
	public void setNoTerminologyChecks(final boolean theNoTerminologyChecks) {
		noTerminologyChecks = theNoTerminologyChecks;
	}

	/**
	 * If set to {@literal true} (default is false) no extensible warnings suppressed
	 */
	public boolean isNoExtensibleWarnings() {
		return noExtensibleWarnings;
	}

	/**
	 * If set to {@literal true} (default is false) no extensible warnings is suppressed
	 */
	public void setNoExtensibleWarnings(final boolean theNoExtensibleWarnings) {
		noExtensibleWarnings = theNoExtensibleWarnings;
	}

	/**
	 * If set to {@literal true} (default is false) no binding message is suppressed
	 */
	public boolean isNoBindingMsgSuppressed() {
		return noBindingMsgSuppressed;
	}

	public List<String> getExtensionDomains() {
		return myExtensionDomains;
	}

	@Override
	protected List<ValidationMessage> validate(IValidationContext<?> theValidationCtx) {
		VersionSpecificWorkerContextWrapper wrappedWorkerContext = provideWorkerContext();

		return new ValidatorWrapper()
			.setAnyExtensionsAllowed(isAnyExtensionsAllowed())
			.setBestPracticeWarningLevel(getBestPracticeWarningLevel())
			.setErrorForUnknownProfiles(isErrorForUnknownProfiles())
			.setExtensionDomains(getExtensionDomains())
			.setValidatorResourceFetcher(validatorResourceFetcher)
			.setValidationPolicyAdvisor(validatorPolicyAdvisor)
			.setNoTerminologyChecks(isNoTerminologyChecks())
			.setNoExtensibleWarnings(isNoExtensibleWarnings())
			.setNoBindingMsgSuppressed(isNoBindingMsgSuppressed())
			.setValidatorResourceFetcher(getValidatorResourceFetcher())
			.setAssumeValidRestReferences(false)
			.validate(wrappedWorkerContext, theValidationCtx);
	}

	@Nonnull
	protected VersionSpecificWorkerContextWrapper provideWorkerContext() {
		VersionSpecificWorkerContextWrapper wrappedWorkerContext = myWrappedWorkerContext;
		if (wrappedWorkerContext == null) {
			// OE PATCH: using patched VersionSpecificWorkerContextWrapper
			try {
				wrappedWorkerContext = new ConvertingWorkerContext(myValidationSupport);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (FHIRException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		myWrappedWorkerContext = wrappedWorkerContext;
		return wrappedWorkerContext;
	}

	public void setValidatorPolicyAdvisor(IValidationPolicyAdvisor validatorPolicyAdvisor) {
		this.validatorPolicyAdvisor = validatorPolicyAdvisor;
	}

	public IValidatorResourceFetcher getValidatorResourceFetcher() {
		return validatorResourceFetcher;
	}

	public void setValidatorResourceFetcher(IValidatorResourceFetcher validatorResourceFetcher) {
		this.validatorResourceFetcher = validatorResourceFetcher;
	}
}