package ch.ahdis.fhir.hapi.jpa.validation;

import ca.uhn.fhir.jpa.packages.PackageInstallOutcomeJson;
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec;
import ca.uhn.fhir.jpa.rp.r4.ImplementationGuideResourceProvider;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ch.ahdis.matchbox.util.MatchboxPackageInstallerImpl;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImplementationGuideProvider extends ImplementationGuideResourceProvider {

	@Autowired
	MatchboxPackageInstallerImpl packageInstallerSvc;

	// Contains implementation guides to download
	private Map<String, AppProperties.ImplementationGuide> implementationGuides;

	private void fillIGMap() {
		this.implementationGuides = new LinkedHashMap<>();

		this.implementationGuides.put("terminology",
			new AppProperties.ImplementationGuide(null, "hl7.terminology", "3.1.0"));

		this.implementationGuides.put("cda",
			new AppProperties.ImplementationGuide("https://github.com/ahdis/cda-core-2.0/releases/download/v0.0.3-dev/package.tgz", "hl7.fhir.cda", "2.1.0"));

		this.implementationGuides.put("cdach",
			new AppProperties.ImplementationGuide(null, "ch.fhir.ig.cda-fhir-maps", "0.3.0"));

		this.implementationGuides.put("iheformatcodefhir",
			new AppProperties.ImplementationGuide(null, "ihe.formatcode.fhir", "1.0.0"));

		this.implementationGuides.put("eprterm",
			new AppProperties.ImplementationGuide(null, "ch.fhir.ig.ch-epr-term", "2.0.7"));

		this.implementationGuides.put("core",
			new AppProperties.ImplementationGuide(null, "ch.fhir.ig.ch-core", "2.0.0"));

		this.implementationGuides.put("emed",
			new AppProperties.ImplementationGuide(null, "ch.fhir.ig.ch-emed", "2.0.0"));
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

	public PackageInstallOutcomeJson loadAll() {
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
