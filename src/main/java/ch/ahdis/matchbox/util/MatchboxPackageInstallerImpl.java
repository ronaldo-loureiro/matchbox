package ch.ahdis.matchbox.util;

import ca.uhn.fhir.context.*;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.packages.*;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.util.SearchParameterUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.Uri;
import org.hl7.fhir.instance.model.api.*;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.ImplementationGuide.ImplementationGuideDefinitionComponent;
import org.hl7.fhir.r4.model.ImplementationGuide.ImplementationGuideDefinitionResourceComponent;
import org.hl7.fhir.utilities.npm.IPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.npm.NpmPackage.NpmPackageFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This is a copy of ca.uhn.fhir.jpa.packages.PackageInstallerSvcImpl
 * with the following modifications:
 * - Resources with status "draft" are also loaded
 * - examples are also loaded
 * Modifications are marked in source code comments with "MODIFIED"
 * <p>
 * //!\\ Work only with R4, R5 and DSTU3 FHIR version
 *
 * @author alexander kreutz
 */
public class MatchboxPackageInstallerImpl {

	private static final Logger ourLog = LoggerFactory.getLogger(MatchboxPackageInstallerImpl.class);

	@Autowired
	private ApplicationContext appCtx;

	@Autowired
	private JpaPackageCache myPackageCacheManager;

	private FhirContext myFhirContext;

	private DaoRegistry myDaoRegistry;

	/**
	 * Constructor
	 */
	public MatchboxPackageInstallerImpl() {
		super();
		this.myFhirContext = FhirContext.forR4();
	}

	@PostConstruct
	private void postConstruct() {
		this.myDaoRegistry = new DaoRegistry(this.myFhirContext);
		this.myDaoRegistry.setApplicationContext(appCtx);
	}

	/**
	 * Loads and installs an IG from a file on disk or the Simplifier repo using
	 * the {@link IPackageCacheManager}.
	 * <p>
	 * Installs the IG by persisting instances of the following types of resources:
	 * <p>
	 * - NamingSystem, CodeSystem, ValueSet, StructureDefinition (with snapshots),
	 * ConceptMap, SearchParameter, Subscription
	 * <p>
	 * Creates the resources if non-existent, updates them otherwise.
	 *
	 * @param theInstallationSpec The details about what should be installed
	 */
	public PackageInstallOutcomeJson install(PackageInstallationSpec theInstallationSpec) throws ImplementationGuideInstallationException {
		PackageInstallOutcomeJson retVal = new PackageInstallOutcomeJson();
		try {
			NpmPackage npmPackage = myPackageCacheManager.installPackage(theInstallationSpec);
			if (npmPackage == null) {
				throw new IOException("Package not found");
			}

			retVal.getMessage().addAll(JpaPackageCache.getProcessingMessages(npmPackage));

			if (theInstallationSpec.getInstallMode() == PackageInstallationSpec.InstallModeEnum.STORE_AND_INSTALL) {
				install(npmPackage, theInstallationSpec, retVal);
			}

		} catch (IOException e) {
			throw new ImplementationGuideInstallationException("Could not load NPM package " + theInstallationSpec.getName() + "#" + theInstallationSpec.getVersion(), e);
		}
		return retVal;
	}

	/**
	 * Installs a package and its dependencies.
	 * <p>
	 * Fails fast if one of its dependencies could not be installed.
	 *
	 * @throws ImplementationGuideInstallationException if installation fails
	 */
	private void install(NpmPackage npmPackage, PackageInstallationSpec theInstallationSpec, PackageInstallOutcomeJson theOutcome) throws ImplementationGuideInstallationException {
		String name = npmPackage.getNpm().get("name").getAsString();
		String version = npmPackage.getNpm().get("version").getAsString();

		List<String> installTypes = theInstallationSpec.getInstallResourceTypes();

		ourLog.info("Installing package: {}#{}", name, version);
		int[] count = new int[installTypes.size()];

		IBaseResource ig = null;

		for (int i = 0; i < installTypes.size(); i++) {
			Collection<IBaseResource> resources = parseResourcesOfType(installTypes.get(i), npmPackage);
			count[i] = resources.size();

			for (IBaseResource next : resources) {
				try {
					create(next, theOutcome);
				} catch (Exception e) {
					ourLog.debug("Failed to upload resource of type {} with ID {} - Error: {}", myFhirContext.getResourceType(next), next.getIdElement().getValue(), e.toString());
					throw new ImplementationGuideInstallationException(String.format("Error installing IG %s#%s: %s", name, version, e.toString()), e);
				}
			}
		}

		// Modified add log
		String log = String.format("Finished installation of package %s#%s:", name, version);
		ourLog.info(log);
		theOutcome.getMessage().add(log);

		for (int i = 0; i < count.length; i++) {
			// Modified add log
			log = String.format("-- Created or updated %s resources of type %s", count[i], installTypes.get(i));
			ourLog.info(log);
			theOutcome.getMessage().add(log);
		}

		if (ig == null) {
			log = String.format("No Implementaiton Guide provided for package %s#%s:", name, version);
			ourLog.info(log);
			theOutcome.getMessage().add(log);
		}

	}

	/**
	 * ============================= Utility methods ===============================
	 */

	// MODIFIED: This method has been reimplemented: also add example folder 
	private List<IBaseResource> parseResourcesOfType(String type, NpmPackage pkg) {
		ArrayList<IBaseResource> resources = new ArrayList<>();

		addFolder(type, pkg.getFolders().get("package"), resources);

		NpmPackageFolder exampleFolder = pkg.getFolders().get("example");
		if (exampleFolder != null) {
			try {
				pkg.indexFolder("example", exampleFolder);
				addFolder(type, exampleFolder, resources);
			} catch (IOException e) {
				throw new InternalErrorException("Cannot install resource of type " + type + ": Could not read example directory", e);
			}
		}
		return resources;
	}

	// MODIFIED: This utility method has been added. It is used by parseResourcesOfType(type, pkg)
	private void addFolder(String type, NpmPackageFolder folder, List<IBaseResource> resources) {
		if (folder == null) return;
		List<String> filesForType = folder.getTypes().get(type);
		if (filesForType == null) return;
		for (String file : filesForType) {
			try {
				byte[] content = folder.fetchFile(file);
				resources.add(myFhirContext.newJsonParser().parseResource(new String(content)));
			} catch (IOException e) {
				throw new InternalErrorException("Cannot install resource of type " + type + ": Could not fetch file " + file, e);
			}
		}
	}

	public void create(IBaseResource theResource, PackageInstallOutcomeJson theOutcome) {
		IFhirResourceDao dao = myDaoRegistry.getResourceDao(theResource.getClass());
		SearchParameterMap map = createSearchParameterMapFor(theResource);
		IBundleProvider searchResult = searchResource(dao, map);
		if (validForUpload(theResource)) {
			if (searchResult.isEmpty()) {

				ourLog.debug("Creating new resource matching {}", map.toNormalizedQueryString(myFhirContext));
				theOutcome.incrementResourcesInstalled(myFhirContext.getResourceType(theResource));

				updateResource(dao, theResource);
				ourLog.debug("Created resource with existing id");

			} else {
				ourLog.debug("Updating existing resource matching {}", map.toNormalizedQueryString(myFhirContext));
				theResource.setId(searchResult.getResources(0, 1).get(0).getIdElement().toUnqualifiedVersionless());
				DaoMethodOutcome outcome = updateResource(dao, theResource);
				if (!outcome.isNop()) {
					theOutcome.incrementResourcesInstalled(myFhirContext.getResourceType(theResource));
				}
			}

		}
	}

	private IBundleProvider searchResource(IFhirResourceDao theDao, SearchParameterMap theMap) {
		return theDao.search(theMap);
	}

	private DaoMethodOutcome updateResource(IFhirResourceDao theDao, IBaseResource theResource) {
		return theDao.update(theResource);
	}

	// MODIFIED: This method has been modified: Also allow Resources with status "draft"
	boolean validForUpload(IBaseResource theResource) {
		List<IPrimitiveType> statusTypes = myFhirContext.newFhirPath().evaluate(theResource, "status", IPrimitiveType.class);
		if (statusTypes.size() > 0) {
			// Modified condition
			if (!statusTypes.get(0).getValueAsString().equals("active") && !statusTypes.get(0).getValueAsString().equals("draft")) {
				return false;
			}
		}

		return true;
	}

	private SearchParameterMap createSearchParameterMapFor(IBaseResource resource) {
		if (resource.getClass().getSimpleName().equals("NamingSystem")) {
			String uniqueId = extractUniqeIdFromNamingSystem(resource);
			return SearchParameterMap.newSynchronous().add("value", new StringParam(uniqueId).setExact(true));
		} else {
			String url = extractUniqueUrlFromMetadataResource(resource);
			return SearchParameterMap.newSynchronous().add("url", new UriParam(url));
		}
	}

	private String extractUniqeIdFromNamingSystem(IBaseResource resource) {
		FhirTerser terser = myFhirContext.newTerser();
		IBase uniqueIdComponent = (IBase) terser.getSingleValueOrNull(resource, "uniqueId");
		if (uniqueIdComponent == null) {
			throw new ImplementationGuideInstallationException("NamingSystem does not have uniqueId component.");
		}
		IPrimitiveType<?> asPrimitiveType = (IPrimitiveType<?>) terser.getSingleValueOrNull(uniqueIdComponent, "value");
		return (String) asPrimitiveType.getValue();
	}

	private String extractUniqueUrlFromMetadataResource(IBaseResource resource) {
		FhirTerser terser = myFhirContext.newTerser();
		IPrimitiveType<?> asPrimitiveType = (IPrimitiveType<?>) terser.getSingleValueOrNull(resource, "url");
		return (String) asPrimitiveType.getValue();
	}
}
