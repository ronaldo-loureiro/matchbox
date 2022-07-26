# matchbox 


[matchbox](https://matchbox.health) is a FHIR server based on the [hapifhir/hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) 
- load FHIR implementation guides from the local resources for conformance resources (StructureMap, CodeSystem, ValueSet, ConceptMap, NamingSystem, StructureDefinition).
- validation support: [server]/$validate for checking FHIR resources conforming to the loaded implementation guides
- FHIR Mapping Language endpoints for creation of StructureMaps and support for the [StructureMap/$transform](https://www.hl7.org/fhir/operation-structuremap-transform.html) operation

a public test server is hosted at [https://test.ahdis.ch/matchbox/fhir](https://test.ahdis.ch/matchbox/fhir) with a corresponding gui [https://test.ahdis.ch/matchbox/](https://test.ahdis.ch/matchbox/#)

## Prerequisites

- [This project](https://github.com/ahdis/matchbox) checked out. You may wish to create a GitHub Fork of the project and check that out instead so that you can customize the project and save the results to GitHub. Check out the main branch (master is kept in sync with [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter)
- Oracle Java (JDK) installed: Minimum JDK11 or newer.
- Apache Maven build tool (newest version)

## Running locally

### Using spring-boot

```bash
mvn clean install -DskipTests spring-boot:run
```