package hug;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.sched.ISchedulerService;
import ca.uhn.fhir.jpa.model.sched.ScheduledJobDefinition;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ch.ahdis.fhir.hapi.jpa.validation.ImplementationGuideProvider;
import ch.ahdis.fhir.hapi.jpa.validation.JpaExtendedValidationSupportChain;
import ch.ahdis.matchbox.mappinglanguage.*;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.dstu2016may.formats.FhirFormat;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.JsonParser;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.XmlParser;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.Property;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap;
import org.hl7.fhir.r5.utils.structuremap.StructureMapUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class Transform {


	protected ConvertingWorkerContext baseWorkerContext;

	private ImplementationGuideProvider igp;

	public Transform(FhirContext fhirContext, ImplementationGuideProvider igp, ConvertingWorkerContext baseWorkerContext) throws IOException {
//		final var supportChain = new JpaExtendedValidationSupportChain(fhirContext);
//		supportChain.postConstruct();
//		this.baseWorkerContext = new ConvertingWorkerContext(supportChain);

		this.baseWorkerContext = baseWorkerContext;
		this.igp = igp;

		this.igp.loadAll(false);

		try {
			transform("http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaChEmedMedicationTreatmentPlanDocumentToBundle", Manager.FhirFormat.XML);
			transform("http://fhir.ch/ig/cda-fhir-maps/StructureMap/BundleToCdaChEmedMedicationCardDocument", Manager.FhirFormat.JSON);
		} catch (IOException ignored) {

		}
	}

	public void transform(String source, Manager.FhirFormat format) throws IOException {
//		String source = "http://fhir.ch/ig/cda-fhir-maps/StructureMap/CdaChEmedMedicationTreatmentPlanDocumentToBundle";

		//File f = new File("./examples/StructureMap-BundleToCdaChEmedMedicationCardDocument.json");
//		File f = new File("./examples/StructureMap-CdaChEmedMedicationTreatmentPlanDocumentToBundle.json");
//		InputStream is = new FileInputStream(f);
//
//		ca.uhn.fhir.parser.IParser parser = baseWorkerContext.;
//		IBaseResource ibr = parser.parseResource(is);
//		org.hl7.fhir.r5.model.StructureMap map = baseWorkerContext.getTransform(ibr);

		StructureMap map  = baseWorkerContext.getTransform(source);
		if (map == null) {
			throw new UnprocessableEntityException("Map not available with canonical url "+ source);
		}

		switch (format) {
			case XML:
				transformDoc(map, baseWorkerContext, "./examples/MTP_01_valid.xml", Manager.FhirFormat.XML);
				break;
			case JSON:
				transformDoc(map, baseWorkerContext, "./examples/Bundle-2-7-MedicationCard.json", Manager.FhirFormat.JSON);
				break;
		}
	}

	private void transformDoc(StructureMap map, ConvertingWorkerContext fhirContext, String src_path, Manager.FhirFormat src_format) throws IOException {
		File f = new File(src_path);
		InputStream is = new FileInputStream(f);

		Element src = Manager.parseSingle(fhirContext, is, src_format);

		Element r = getTargetResourceFromStructureMap(map, fhirContext);
		if (r == null) {
			throw new UnprocessableEntityException("Target Structure can not be resolved from map, is the corresponding implmentation guide provided?");
		}

		StructureMapUtilities utils = new MatchboxStructureMapUtilities(fhirContext, new TransformSupportServices(fhirContext, new ArrayList<Base>()));
		utils.transform(null, src, map, r);
		ElementModelSorter.sort(r);

		if (r.isResource() && "Bundle".contentEquals(r.getType())) {
			Property bundleType = r.getChildByName("type");
			if (bundleType!=null && bundleType.getValues()!=null && "document".equals(bundleType.getValues().get(0).primitiveValue())) {
				removeBundleEntryIds(r);
			}
		}

		OutputStream os = new OutputStream() {
			private final StringBuilder strBuild = new StringBuilder();
			@Override
			public void write(int b) throws IOException {
				strBuild.append((char) b);
			}

			@Override
			public String toString() {
				return strBuild.toString();
			}
		};

		try {

			switch (src_format){
				case XML:
					new JsonParser(fhirContext).compose(r, os, IParser.OutputStyle.PRETTY, null);
					break;
				case JSON:
					new XmlParser(fhirContext).compose(r, os, IParser.OutputStyle.PRETTY, null);
					break;
			}

			System.out.println(os);

		} catch(FHIRException e) {
			os.write("Exception during Transform: ".getBytes());
			os.write(e.getMessage().getBytes());
		}

		os.close();
	}

	private Element getTargetResourceFromStructureMap(StructureMap map, IWorkerContext fhirContext) {
		String targetTypeUrl = null;
		for (StructureMap.StructureMapStructureComponent component : map.getStructure()) {
			if (component.getMode() == StructureMap.StructureMapModelMode.TARGET) {
				targetTypeUrl = component.getUrl();
				break;
			}
		}

		if (targetTypeUrl == null)
			throw new FHIRException("Unable to determine resource URL for target type "+targetTypeUrl);

		StructureDefinition structureDefinition = fhirContext.fetchResource(StructureDefinition.class, targetTypeUrl);

		if (structureDefinition == null)
			throw new FHIRException("Unable to determine StructureDefinition for target type "+targetTypeUrl);

		return Manager.build(fhirContext, structureDefinition);
	}

	private void removeBundleEntryIds(Element bundle) {
		List<Element> ids = bundle.getChildrenByName("id");
		for(Element id: ids) {
			bundle.getChildren().remove(id);
		}
		List<Element> entries = bundle.getChildrenByName("entry");
		for(Element entry : entries) {
			Property fullUrl = entry.getChildByName("fullUrl");
			if (fullUrl.getValues()!=null && fullUrl.getValues().get(0).primitiveValue().startsWith("urn:uuid:")) {
				Property resource = entry.getChildByName("resource");
				if (resource!=null && resource.getValues()!=null) {
					Element entryResource = (Element) resource.getValues().get(0);
					ids = entryResource.getChildrenByName("id");
					for(Element id: ids) {
						entryResource.getChildren().remove(id);
					}
				}
			}
		}
	}
}
