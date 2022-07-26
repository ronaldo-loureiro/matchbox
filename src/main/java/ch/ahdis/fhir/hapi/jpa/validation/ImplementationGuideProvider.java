package ch.ahdis.fhir.hapi.jpa.validation;

import ca.uhn.fhir.jpa.packages.PackageInstallOutcomeJson;
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ch.ahdis.matchbox.util.MatchboxPackageInstallerImpl;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ImplementationGuideProvider {

	@Autowired
	MatchboxPackageInstallerImpl packageInstallerSvc;

	// Contains implementation guides to download
	private Map<String, AppProperties.ImplementationGuide> implementationGuides;

	@FunctionalInterface
	private interface PutIG {
		void accept(String key, String packageName, String name, String version);
	}

	private void fillIGMap() {
		this.implementationGuides = new LinkedHashMap<>();

		// The implementation guides in the target folder don't work
		String BASE_PATH = getClass().getResource("/igs").toString().split("/target")[0];

		PutIG putIG = (key, packageName, name, version) -> this.implementationGuides.put(key, new AppProperties.ImplementationGuide(
			String.format("%s/src/main/resources/igs/%s", BASE_PATH, packageName),
			name,
			version));

		putIG.accept("terminology", "ig-hl7-terminology-3-1-0.tgz", "hl7.terminology", "3.1.0");
		putIG.accept("cda", "ig-hl7-fhir-cda-2-1-0.tgz", "hl7.fhir.cda", "2.1.0");
		putIG.accept("cdach", "ig-cda-fhir-maps-0-3-0.tgz", "ch.fhir.ig.cda-fhir-maps", "0.3.0");
		putIG.accept("iheformatcodefhir", "ig-ihe-formatcode-fhir-1-0-0.tgz", "ihe.formatcode.fhir", "1.0.0");
		putIG.accept("eprterm", "ig-ch-epr-term-2-0-7.tgz", "ch.fhir.ig.ch-epr-term", "2.0.7");
		putIG.accept("core", "ig-ch-core-2-0-0.tgz", "ch.fhir.ig.ch-core", "2.0.0");
		putIG.accept("emed", "ig-ch-emed-2-0-0.tgz", "ch.fhir.ig.ch-emed", "2.0.0");

	}

	public PackageInstallOutcomeJson load(ImplementationGuide theResource, PackageInstallOutcomeJson install) {
		PackageInstallOutcomeJson installOutcome = packageInstallerSvc.install(new PackageInstallationSpec()
			.setPackageUrl(theResource.getUrl())
			.addInstallResourceTypes(
				"NamingSystem",
				"CodeSystem",
				"ValueSet",
				"StructureDefinition",
				"ConceptMap",
				"StructureMap",
				"ImplementationGuide")
			.setName(theResource.getName())
			.setVersion(theResource.getVersion())
			.setInstallMode(PackageInstallationSpec.InstallModeEnum.STORE_AND_INSTALL));

		if (install != null) {
			install.getMessage().addAll(installOutcome.getMessage());
			return install;
		}
		return installOutcome;
	}

	public PackageInstallOutcomeJson loadAll() throws IOException {
		PackageInstallOutcomeJson installOutcome = null;
		fillIGMap();
		if (this.implementationGuides != null) {
			for (AppProperties.ImplementationGuide guide : this.implementationGuides.values()) {
				ImplementationGuide ig = new ImplementationGuide();
				ig.setName(guide.getName());
				ig.setPackageId(guide.getName());
				ig.setUrl(guide.getUrl());
				ig.setVersion(guide.getVersion());
				installOutcome = load(ig, installOutcome);
			}
		}
		return installOutcome;
	}
}
