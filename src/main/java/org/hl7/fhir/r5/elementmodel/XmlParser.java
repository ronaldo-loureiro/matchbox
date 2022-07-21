package org.hl7.fhir.r5.elementmodel;

/*
  Copyright (c) 2011+, HL7, Inc.
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:
    
   * Redistributions of source code must retain the above copyright notice, this 
     list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright notice, 
     this list of conditions and the following disclaimer in the documentation 
     and/or other materials provided with the distribution.
   * Neither the name of HL7 nor the names of its contributors may be used to 
     endorse or promote products derived from this software without specific 
     prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
  POSSIBILITY OF SUCH DAMAGE.
  
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.FHIRFormatError;
import org.hl7.fhir.r5.conformance.ProfileUtilities;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element.SpecialElement;
import org.hl7.fhir.r5.elementmodel.ParserBase.NamedElement;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.ElementDefinition.PropertyRepresentation;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.model.Enumeration;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.utils.ToolingExtensions;
import org.hl7.fhir.r5.utils.formats.XmlLocationAnnotator;
import org.hl7.fhir.r5.utils.formats.XmlLocationData;
import org.hl7.fhir.utilities.ElementDecoration;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.xhtml.CDANarrativeFormat;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.hl7.fhir.utilities.xhtml.XhtmlParser;
import org.hl7.fhir.utilities.xml.IXMLWriter;
import org.hl7.fhir.utilities.xml.XMLUtil;
import org.hl7.fhir.utilities.xml.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class XmlParser extends ParserBase {
	public XmlParser(IWorkerContext context) {
		super(context);
	}

	public List<NamedElement> parse(InputStream stream) throws FHIRFormatError, DefinitionException, FHIRException, IOException {
		List<NamedElement> res = new ArrayList<>();
		Document doc = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// xxe protection
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);

			factory.setNamespaceAware(true);
			if (policy == ValidationPolicy.EVERYTHING) {
				// The SAX interface appears to not work when reporting the correct version/encoding.
				// if we can, we'll inspect the header/encoding ourselves
				if (stream.markSupported()) {
					stream.mark(1024);
					stream.reset();
				}
				// use a slower parser that keeps location data
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer nullTransformer = transformerFactory.newTransformer();
				DocumentBuilder docBuilder = factory.newDocumentBuilder();
				doc = docBuilder.newDocument();
				DOMResult domResult = new DOMResult(doc);
				SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setNamespaceAware(true);
				spf.setValidating(false);
				// xxe protection
				spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				SAXParser saxParser = spf.newSAXParser();
				XMLReader xmlReader = saxParser.getXMLReader();
				// xxe protection
				xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
				xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

				XmlLocationAnnotator locationAnnotator = new XmlLocationAnnotator(xmlReader, doc);
				InputSource inputSource = new InputSource(stream);
				SAXSource saxSource = new SAXSource(locationAnnotator, inputSource);
				nullTransformer.transform(saxSource, domResult);
			} else {
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(stream);
			}
		} catch (Exception e) {
			logError(0, 0, "(syntax)", IssueType.INVALID, e.getMessage(), IssueSeverity.FATAL);
			doc = null;
		}
		if (doc != null) {
			Element e = parse(doc);
			if (e != null) {
				res.add(new NamedElement(null, e));
			}
		}
		return res;
	}


	private int line(Node node) {
		XmlLocationData loc = node == null ? null : (XmlLocationData) node.getUserData(XmlLocationData.LOCATION_DATA_KEY);
		return loc == null ? 0 : loc.getStartLine();
	}

	private int col(Node node) {
		XmlLocationData loc = node == null ? null : (XmlLocationData) node.getUserData(XmlLocationData.LOCATION_DATA_KEY);
		return loc == null ? 0 : loc.getStartColumn();
	}

	public Element parse(Document doc) throws FHIRFormatError, DefinitionException, FHIRException, IOException {
		org.w3c.dom.Element element = doc.getDocumentElement();
		return parse(element);
	}

	// Fixed for https://github.com/ahdis/matchbox/issues/31
	@Override
	protected StructureDefinition getDefinition(int line, int col, String ns, String name) throws FHIRFormatError {
		if (ns == null) {
			logError(line, col, name, IssueType.STRUCTURE, context.formatMessage(I18nConstants.THIS__CANNOT_BE_PARSED_AS_A_FHIR_OBJECT_NO_NAMESPACE, name), IssueSeverity.FATAL);
			return null;
		}
		if (name == null) {
			logError(line, col, name, IssueType.STRUCTURE, context.formatMessage(I18nConstants.THIS_CANNOT_BE_PARSED_AS_A_FHIR_OBJECT_NO_NAME), IssueSeverity.FATAL);
			return null;
		}
		StructureDefinition sd = context.fetchTypeDefinition(ns + "|" + name);
		if (sd != null) {
			return sd;
		}
		logError(line, col, name, IssueType.STRUCTURE, context.formatMessage(I18nConstants.THIS_DOES_NOT_APPEAR_TO_BE_A_FHIR_RESOURCE_UNKNOWN_NAMESPACENAME_, ns, name), IssueSeverity.FATAL);
		return null;
	}

	public Element parse(org.w3c.dom.Element element) throws FHIRFormatError, DefinitionException, FHIRException, IOException {
		String ns = element.getNamespaceURI();
		String name = element.getLocalName();
		String path = "/" + pathPrefix(ns) + name;

		StructureDefinition sd = getDefinition(line(element), col(element), (ns == null ? "noNamespace" : ns), name);
		if (sd == null)
			return null;

		Element result = new Element(element.getLocalName(), new Property(context, sd.getSnapshot().getElement().get(0), sd));
		result.setPath(element.getLocalName());
		result.markLocation(line(element), col(element));
		result.setType(element.getLocalName());
		parseChildren(path, element, result);
		result.numberChildren();
		return result;
	}

	private String pathPrefix(String ns) {
		if (Utilities.noString(ns))
			return "";
		if (ns.equals(FormatUtilities.FHIR_NS))
			return "f:";
		if (ns.equals(FormatUtilities.XHTML_NS))
			return "h:";
		if (ns.equals("urn:hl7-org:v3"))
			return "v3:";
		if (ns.equals("urn:hl7-org:sdtc"))
			return "sdtc:";
		if (ns.equals("urn:ihe:pharm"))
			return "pharm:";
		return "?:";
	}


	private void parseChildren(String path, org.w3c.dom.Element node, Element element) throws FHIRFormatError, FHIRException, IOException, DefinitionException {
		// this parsing routine retains the original order in a the XML file, to support validation
		reapComments(node, element);
		List<Property> properties = element.getProperty().getChildProperties(element.getName(), XMLUtil.getXsiType(node));

		String text = XMLUtil.getDirectText(node).trim();
		int line = line(node);
		int col = col(node);
		if (!Utilities.noString(text)) {
			Property property = getTextProp(properties);
			if (property != null) {
				if ("ED.data[x]".equals(property.getDefinition().getId()) || (property.getDefinition() != null && property.getDefinition().getBase() != null && "ED.data[x]".equals(property.getDefinition().getBase().getPath()))) {
					if ("B64".equals(node.getAttribute("representation"))) {
						Element n = new Element("dataBase64Binary", property, "base64Binary", text).markLocation(line, col);
						n.setPath(element.getPath() + "." + property.getName());
						element.getChildren().add(n);
					} else {
						Element n = new Element("dataString", property, "string", text).markLocation(line, col);
						n.setPath(element.getPath() + "." + property.getName());
						element.getChildren().add(n);
					}
				} else {
					Element n = new Element(property.getName(), property, property.getType(), text).markLocation(line, col);
					n.setPath(element.getPath() + "." + property.getName());
					element.getChildren().add(n);
				}
			} else {
				Node n = node.getFirstChild();
				while (n != null) {
					if (n.getNodeType() == Node.TEXT_NODE && !Utilities.noString(n.getTextContent().trim())) {
						while (n.getNextSibling() != null && n.getNodeType() != Node.ELEMENT_NODE) {
							n = n.getNextSibling();
						}
						while (n.getPreviousSibling() != null && n.getNodeType() != Node.ELEMENT_NODE) {
							n = n.getPreviousSibling();
						}
						line = line(n);
						col = col(n);
						logError(line, col, path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.TEXT_SHOULD_NOT_BE_PRESENT, text), IssueSeverity.ERROR);
					}
					n = n.getNextSibling();
				}
			}
		}

		for (int i = 0; i < node.getAttributes().getLength(); i++) {
			Node attr = node.getAttributes().item(i);
			if (!(attr.getNodeName().equals("xmlns") || attr.getNodeName().startsWith("xmlns:"))) {
				Property property = getAttrProp(properties, attr.getLocalName(), attr.getNamespaceURI());
				if (property != null) {
					String av = attr.getNodeValue();
					if (ToolingExtensions.hasExtension(property.getDefinition(), "http://www.healthintersections.com.au/fhir/StructureDefinition/elementdefinition-dateformat"))
						av = convertForDateFormatFromExternal(ToolingExtensions.readStringExtension(property.getDefinition(), "http://www.healthintersections.com.au/fhir/StructureDefinition/elementdefinition-dateformat"), av);
					if (property.getName().equals("value") && element.isPrimitive())
						element.setValue(av);
					else {
						Element n = new Element(property.getName(), property, property.getType(), av).markLocation(line, col);
						n.setPath(element.getPath() + "." + property.getName());
						element.getChildren().add(n);
					}
				} else {
					boolean ok = false;
					if (!FormatUtilities.FHIR_NS.equals(node.getNamespaceURI()))
						ok = ok || (attr.getLocalName().equals("schemaLocation")); // xsi:schemalocation allowed for non FHIR content
					ok = ok || (hasTypeAttr(element) && attr.getLocalName().equals("type") && FormatUtilities.NS_XSI.equals(attr.getNamespaceURI())); // xsi:type allowed if element says so
					if (!ok)
						logError(line, col, path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.UNDEFINED_ATTRIBUTE__ON__FOR_TYPE__PROPERTIES__, attr.getNodeName(), node.getNodeName(), element.fhirType(), properties), IssueSeverity.ERROR);
				}
			}
		}

		String lastName = null;
		int repeatCount = 0;
		Node child = node.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Property property = getElementProp(properties, child.getLocalName(), child.getNamespaceURI());
				if (property != null) {
					if (property.getName().equals(lastName)) {
						repeatCount++;
					} else {
						lastName = property.getName();
						repeatCount = 0;
					}
					if (!property.isChoice() && "xhtml".equals(property.getType())) {
						XhtmlNode xhtml;
						if (property.getDefinition().hasRepresentation(PropertyRepresentation.CDATEXT))
							xhtml = new CDANarrativeFormat().convert((org.w3c.dom.Element) child);
						else
							xhtml = new XhtmlParser().setValidatorMode(true).parseHtmlNode((org.w3c.dom.Element) child);
						Element n = new Element(property.getName(), property, "xhtml", new XhtmlComposer(XhtmlComposer.XML, false).compose(xhtml)).setXhtml(xhtml).markLocation(line(child), col(child));
						n.setPath(element.getPath() + "." + property.getName());
						element.getChildren().add(n);
					} else {
						String npath = path + "/" + pathPrefix(child.getNamespaceURI()) + child.getLocalName();
						Element n = new Element(child.getLocalName(), property).markLocation(line(child), col(child));
						if (property.isList()) {
							n.setPath(element.getPath() + "." + property.getName() + "[" + repeatCount + "]");
						} else {
							n.setPath(element.getPath() + "." + property.getName());
						}

						boolean ok = true;
						if (property.isChoice()) {
							if (property.getDefinition().hasRepresentation(PropertyRepresentation.TYPEATTR)) {
								String xsiType = ((org.w3c.dom.Element) child).getAttributeNS(FormatUtilities.NS_XSI, "type");
								if (Utilities.noString(xsiType)) {
									if (ToolingExtensions.hasExtension(property.getDefinition(), "http://hl7.org/fhir/StructureDefinition/elementdefinition-defaulttype")) {
										xsiType = ToolingExtensions.readStringExtension(property.getDefinition(), "http://hl7.org/fhir/StructureDefinition/elementdefinition-defaulttype");
										n.setType(xsiType);
									} else {
										logError(line(child), col(child), path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.NO_TYPE_FOUND_ON_, child.getLocalName()), IssueSeverity.ERROR);
										ok = false;
									}
								} else {
									if (xsiType.contains(":"))
										xsiType = xsiType.substring(xsiType.indexOf(":") + 1);
									n.setType(xsiType);
									n.setExplicitType(xsiType);
								}
							} else
								n.setType(n.getType());
						}
						element.getChildren().add(n);
						if (ok) {
							if (property.isResource())
								parseResource(npath, (org.w3c.dom.Element) child, n, property);
							else
								parseChildren(npath, (org.w3c.dom.Element) child, n);
						}
					}
				} else
					logError(line(child), col(child), path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.UNDEFINED_ELEMENT_, child.getLocalName()), IssueSeverity.ERROR);
			} else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
				logError(line(child), col(child), path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.CDATA_IS_NOT_ALLOWED), IssueSeverity.ERROR);
			} else if (!Utilities.existsInList(child.getNodeType(), 3, 8)) {
				logError(line(child), col(child), path, IssueType.STRUCTURE, context.formatMessage(I18nConstants.NODE_TYPE__IS_NOT_ALLOWED, Integer.toString(child.getNodeType())), IssueSeverity.ERROR);
			}
			child = child.getNextSibling();
		}
	}

	private Property getElementProp(List<Property> properties, String nodeName, String namespace) {
		List<Property> propsSortedByLongestFirst = new ArrayList<Property>(properties);
		// sort properties according to their name longest first, so .requestOrganizationReference comes first before .request[x]
		// and therefore the longer property names get evaluated first
		Collections.sort(propsSortedByLongestFirst, new Comparator<Property>() {
			@Override
			public int compare(Property o1, Property o2) {
				return o2.getName().length() - o1.getName().length();
			}
		});
		// first scan, by namespace
		for (Property p : propsSortedByLongestFirst) {
			if (!p.getDefinition().hasRepresentation(PropertyRepresentation.XMLATTR) && !p.getDefinition().hasRepresentation(PropertyRepresentation.XMLTEXT)) {
				if (p.getXmlName().equals(nodeName) && p.getXmlNamespace().equals(namespace))
					return p;
			}
		}
		for (Property p : propsSortedByLongestFirst) {
			if (!p.getDefinition().hasRepresentation(PropertyRepresentation.XMLATTR) && !p.getDefinition().hasRepresentation(PropertyRepresentation.XMLTEXT)) {
				if (p.getXmlName().equals(nodeName))
					return p;
				if (p.getName().endsWith("[x]") && nodeName.length() > p.getName().length() - 3 && p.getName().substring(0, p.getName().length() - 3).equals(nodeName.substring(0, p.getName().length() - 3)))
					return p;
			}
		}
		return null;
	}

	private Property getAttrProp(List<Property> properties, String nodeName, String namespace) {
		for (Property p : properties) {
			if (p.getXmlName().equals(nodeName) && p.getDefinition().hasRepresentation(PropertyRepresentation.XMLATTR) && p.getXmlNamespace().equals(namespace)) {
				return p;
			}
		}
		if (namespace == null) {
			for (Property p : properties) {
				if (p.getXmlName().equals(nodeName) && p.getDefinition().hasRepresentation(PropertyRepresentation.XMLATTR)) {
					return p;
				}
			}
		}
		return null;
	}

	private Property getTextProp(List<Property> properties) {
		for (Property p : properties)
			if (p.getDefinition().hasRepresentation(PropertyRepresentation.XMLTEXT))
				return p;
		return null;
	}

	private String convertForDateFormatFromExternal(String fmt, String av) throws FHIRException {
		if ("v3".equals(fmt)) {
			try {
				DateTimeType d = DateTimeType.parseV3(av);
				return d.asStringValue();
			} catch (Exception e) {
				return av; // not at all clear what to do in this case.
			}
		} else
			throw new FHIRException(context.formatMessage(I18nConstants.UNKNOWN_DATA_FORMAT_, fmt));
	}

	private String convertForDateFormatToExternal(String fmt, String av) throws FHIRException {
		if ("v3".equals(fmt)) {
			DateTimeType d = new DateTimeType(av);
			return d.getAsV3();
		} else
			throw new FHIRException(context.formatMessage(I18nConstants.UNKNOWN_DATE_FORMAT_, fmt));
	}

	private void parseResource(String string, org.w3c.dom.Element container, Element parent, Property elementProperty) throws FHIRFormatError, DefinitionException, FHIRException, IOException {
		org.w3c.dom.Element res = XMLUtil.getFirstChild(container);
		String name = res.getLocalName();
		StructureDefinition sd = context.fetchResource(StructureDefinition.class, ProfileUtilities.sdNs(name, context.getOverrideVersionNs()));
		if (sd == null)
			throw new FHIRFormatError(context.formatMessage(I18nConstants.CONTAINED_RESOURCE_DOES_NOT_APPEAR_TO_BE_A_FHIR_RESOURCE_UNKNOWN_NAME_, res.getLocalName()));
		parent.updateProperty(new Property(context, sd.getSnapshot().getElement().get(0), sd), SpecialElement.fromProperty(parent.getProperty()), elementProperty);
		parent.setType(name);
		parseChildren(res.getLocalName(), res, parent);
	}

	private void reapComments(org.w3c.dom.Element element, Element context) {
		Node node = element.getPreviousSibling();
		while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
			if (node.getNodeType() == Node.COMMENT_NODE)
				context.getComments().add(0, node.getTextContent());
			node = node.getPreviousSibling();
		}
		node = element.getLastChild();
		while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
			node = node.getPreviousSibling();
		}
		while (node != null) {
			if (node.getNodeType() == Node.COMMENT_NODE)
				context.getComments().add(node.getTextContent());
			node = node.getNextSibling();
		}
	}

	private boolean isAttr(Property property) {
		for (Enumeration<PropertyRepresentation> r : property.getDefinition().getRepresentation()) {
			if (r.getValue() == PropertyRepresentation.XMLATTR) {
				return true;
			}
		}
		return false;
	}

	private boolean isCdaText(Property property) {
		for (Enumeration<PropertyRepresentation> r : property.getDefinition().getRepresentation()) {
			if (r.getValue() == PropertyRepresentation.CDATEXT) {
				return true;
			}
		}
		return false;
	}

	private boolean isTypeAttr(Property property) {
		for (Enumeration<PropertyRepresentation> r : property.getDefinition().getRepresentation()) {
			if (r.getValue() == PropertyRepresentation.TYPEATTR) {
				return true;
			}
		}
		return false;
	}

	private boolean isText(Property property) {
		for (Enumeration<PropertyRepresentation> r : property.getDefinition().getRepresentation()) {
			if (r.getValue() == PropertyRepresentation.XMLTEXT) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void compose(Element e, OutputStream stream, OutputStyle style, String base) throws IOException, FHIRException {
		XMLWriter xml = new XMLWriter(stream, "UTF-8");
		xml.setSortAttributes(false);
		xml.setPretty(style == OutputStyle.PRETTY);
		xml.start();
		String ns = e.getProperty().getXmlNamespace();
		if (ns != null && !"noNamespace".equals(ns)) {
			xml.setDefaultNamespace(ns);
		}
		if (hasTypeAttr(e))
			xml.namespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");
		addNamespaces(xml, e);
		composeElement(xml, e, e.getType(), true);
		xml.end();
	}

	private void addNamespaces(IXMLWriter xml, Element e) throws IOException {
		String ns = e.getProperty().getXmlNamespace();
		if (ns != null && xml.getDefaultNamespace() != null && !xml.getDefaultNamespace().equals(ns)) {
			if (!xml.namespaceDefined(ns)) {
				String prefix = pathPrefix(ns);
				if (prefix.endsWith(":")) {
					prefix = prefix.substring(0, prefix.length() - 1);
				}
				if ("?".equals(prefix)) {
					xml.namespace(ns);
				} else {
					xml.namespace(ns, prefix);
				}
			}
		}
		for (Element c : e.getChildren()) {
			addNamespaces(xml, c);
		}
	}

	private boolean hasTypeAttr(Element e) {
		if (isTypeAttr(e.getProperty()))
			return true;
		for (Element c : e.getChildren()) {
			if (hasTypeAttr(c))
				return true;
		}
		return false;
	}

	private void setXsiTypeIfIsTypeAttr(IXMLWriter xml, Element element) throws IOException, FHIRException {
		if (isTypeAttr(element.getProperty()) && !Utilities.noString(element.getType())) {
			String type = element.getType();
			if (Utilities.isAbsoluteUrl(type)) {
				type = type.substring(type.lastIndexOf("/") + 1);
			}
			xml.attribute("xsi:type", type);
		}
	}

	private void composeElement(IXMLWriter xml, Element element, String elementName, boolean root) throws IOException, FHIRException {
		if (showDecorations) {
			@SuppressWarnings("unchecked")
			List<ElementDecoration> decorations = (List<ElementDecoration>) element.getUserData("fhir.decorations");
			if (decorations != null)
				for (ElementDecoration d : decorations)
					xml.decorate(d);
		}
		for (String s : element.getComments()) {
			xml.comment(s, true);
		}
		if (isText(element.getProperty())) {
			if (linkResolver != null)
				xml.link(linkResolver.resolveProperty(element.getProperty()));
			xml.enter(element.getProperty().getXmlNamespace(), elementName);
			xml.text(element.getValue());
			xml.exit(element.getProperty().getXmlNamespace(), elementName);
		} else if (!element.hasChildren() && !element.hasValue()) {
			if (element.getExplicitType() != null)
				xml.attribute("xsi:type", element.getExplicitType());
			xml.element(elementName);
		} else if (element.isPrimitive() || (element.hasType() && isPrimitive(element.getType()))) {
			if (element.getType().equals("xhtml")) {
				String rawXhtml = element.getValue();
				if (isCdaText(element.getProperty())) {
					new CDANarrativeFormat().convert(xml, new XhtmlParser().parseFragment(rawXhtml));
				} else {
					xml.escapedText(rawXhtml);
					xml.anchor("end-xhtml");
				}
			} else if (isText(element.getProperty())) {
				if (linkResolver != null)
					xml.link(linkResolver.resolveProperty(element.getProperty()));
				xml.text(element.getValue());
			} else {
				setXsiTypeIfIsTypeAttr(xml, element);
				if (element.hasValue()) {
					if (linkResolver != null)
						xml.link(linkResolver.resolveType(element.getType()));
					xml.attribute("value", element.getValue());
				}
				if (linkResolver != null)
					xml.link(linkResolver.resolveProperty(element.getProperty()));
				if (element.hasChildren()) {
					xml.enter(element.getProperty().getXmlNamespace(), elementName);
					for (Element child : element.getChildren())
						composeElement(xml, child, child.getName(), false);
					xml.exit(element.getProperty().getXmlNamespace(), elementName);
				} else
					xml.element(elementName);
			}
		} else {
			setXsiTypeIfIsTypeAttr(xml, element);
			for (Element child : element.getChildren()) {
				if (isAttr(child.getProperty())) {
					if (linkResolver != null)
						xml.link(linkResolver.resolveType(child.getType()));
					String av = child.getValue();
					if (ToolingExtensions.hasExtension(child.getProperty().getDefinition(), "http://www.healthintersections.com.au/fhir/StructureDefinition/elementdefinition-dateformat"))
						av = convertForDateFormatToExternal(ToolingExtensions.readStringExtension(child.getProperty().getDefinition(), "http://www.healthintersections.com.au/fhir/StructureDefinition/elementdefinition-dateformat"), av);
					// PATCH: adusting it for pharm
					xml.attribute(child.getProperty().getXmlName(), av);
				}
			}
			if (linkResolver != null)
				xml.link(linkResolver.resolveProperty(element.getProperty()));
			xml.enter(element.getProperty().getXmlNamespace(), elementName);
			if (!root && element.getSpecial() != null) {
				if (linkResolver != null)
					xml.link(linkResolver.resolveProperty(element.getProperty()));
				xml.enter(element.getProperty().getXmlNamespace(), element.getType());
			}
			for (Element child : element.getChildren()) {
				if (isText(child.getProperty())) {
					if (linkResolver != null)
						xml.link(linkResolver.resolveProperty(element.getProperty()));
					xml.text(child.getValue());
				} else if (!isAttr(child.getProperty()))
					composeElement(xml, child, child.getName(), false);
			}
			if (!root && element.getSpecial() != null)
				xml.exit(element.getProperty().getXmlNamespace(), element.getType());
			xml.exit(element.getProperty().getXmlNamespace(), elementName);
		}
	}
}