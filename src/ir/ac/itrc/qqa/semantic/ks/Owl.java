/**
 * 
 */
package ir.ac.itrc.qqa.semantic.ks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;
import ir.ac.itrc.qqa.semantic.util.Common;

/**
 * This class provides an interface to load OWL ontologies into memory. 
 * In practice it has been used to load Quranic Ontology in OWL format into memory.
 * Caveat: full support of OWL is not guaranteed. These relation types have been totally ignored:
 * <ul>
 * 		<li>ObjectPropertyDomain, </li>
 * 		<li>FunctionalDataProperty, </li>
 * 		<li>DataPropertyDomain, </li>
 * 		<li>DataPropertyRange, </li>
 * 		<li>AnnotationPropertyRange,</li> 
 * 		<li>AnnotationPropertyDomain</li>
 * </ul> 
 * @author Ehsan Darrudi
 *
 */
public class Owl
{
	/** the source to the KnowledgeExplorer kb of the system. all relations and concepts are added to this kb */
	private KnowledgeBase _kb;

    /** the path to xml file */
	//private final String _ontologyPath = "res/ontologies/quran_owl/test.owl";
	private String _ontologyPath = "res/ontologies/quran_owl/1-10-36.owl";
	
	/** a temporary variable to load and check symmetric relations */
	private HashSet<String> _hashsetSymmetricObjectProperties = new HashSet<String>();
	
	/** Specifies whether the mapping to farsnet should be done or not while loading the ontology */ 
	public boolean mapToFarsnet = false;
	
	private static final boolean IGNORE_TEMPORARY_ROOTS = true;
	
	/**
	 * An auxiliary class used to handle source concept names such 'farhangq:name'
	 * @author Ehsan Darrudi
	 *
	 */
	private class SourcedConcept
	{
		public String name;
		SourceType source;
	}
	
	//=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * Main constructor
	 * @param kb the input KB. All new concepts and relations are loaded into this KB.
	 */
	public Owl(KnowledgeBase kb)
	{
		_kb = kb;
	}
	
	/**
	 * Main entry point for this class. 
	 * Loads all concepts and relations into memory
	 */
	public void load()
	{
		Document doc = getXmlDocument();
		
		loadSymmetricObjectProperty(doc);
		
		loadSubClassOf(doc);
		
		loadClassAssertion(doc);
		
		loadObjectPropertyAssertion(doc);

		loadDataPropertyAssertion(doc);
		
		loadAnnotationAssertions(doc);
		
		// TODO: ignored tags: ObjectPropertyDomain, FunctionalDataProperty, DataPropertyDomain, DataPropertyRange, AnnotationPropertyRange, AnnotationPropertyDomain, EquivalentClasses, SameIndividual
	}	
	
	/**
	 * Opens the source XLM file for reading
	 * @return The parsed XML document
	 */
	private Document getXmlDocument()
	{
		Common.log("Quran: loading OWL ontology '" + _ontologyPath + "' ... ");
    	
    	File fXmlFile = new File(_ontologyPath);

    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		
    	DocumentBuilder dBuilder = null;
    	
		try
		{
			dBuilder = dbFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		
    	Document doc = null;
    	
		try
		{
			doc = dBuilder.parse(fXmlFile);
		}
		catch (SAXException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		doc.getDocumentElement().normalize();
		
		return doc;
	}
	
	/**
	 * Loads symmetric relations into memory for later use
	 * Some relations in OWL are symmetric, i.e. if SourceType -- Relation --> Destination holds then Destination -- Relation --> SourceType holds too.
	 * @param doc The parsed XML document
	 */
	private void loadSymmetricObjectProperty(Document doc)
	{
		NodeList nList1 = doc.getElementsByTagName("SymmetricObjectProperty");
		
		int added = 0;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;
			
				String relation = cleanConceptName(Common.getTagAttribute("ObjectProperty", "IRI", eElement)).name;
				
				if (!_hashsetSymmetricObjectProperties.contains(relation))
					_hashsetSymmetricObjectProperties.add(relation);
				
				added++;
			}
		}
		
		Common.log("found " + added + " `SymmetricObjectProperty` (semmetric) relation names.");
	}
	
	/**
	 * Loads `SubClassOf` relations in to memory.
	 * This SubClassOf is a specific conceptType of ISA relation and we treat it as ISA
	 * @param doc The parsed XML document
	 */
	private void loadSubClassOf(Document doc)
	{
		NodeList nList1 = doc.getElementsByTagName("SubClassOf");
		
		SourcedConcept spec = null;
		SourcedConcept gen = null;
		
		int added = 0;
		int bad = 0;
		int skippedTemporary = 0;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;
				NodeList children = eElement.getElementsByTagName("Class");
				
				Node iri1 = children.item(0).getAttributes().getNamedItem("IRI");
				Node iri2 = children.item(1).getAttributes().getNamedItem("IRI");
				
				if (iri1 == null || iri2 == null)
				{
					//iri1 = children.item(0).getAttributes().getNamedItem("abbreviatedIRI");
					//iri2 = children.item(1).getAttributes().getNamedItem("abbreviatedIRI");
					
					if (iri1 == null || iri2 == null)
					{					
						bad++;
						continue;
					}
				}
				
				spec 	= cleanConceptName(iri1.getNodeValue());
				gen 	= cleanConceptName(iri2.getNodeValue());
				
				
				if (IGNORE_TEMPORARY_ROOTS && (gen.name.equals("موقت") || gen.name.equals("شکیات")))
				{
					skippedTemporary++;
					continue;
				}
				
				ir.ac.itrc.qqa.semantic.kb.Node specNode 	= _kb.addConcept(spec.name, spec.source);
				ir.ac.itrc.qqa.semantic.kb.Node genNode 	= _kb.addConcept(gen.name, gen.source);
				
				_kb.addRelation(specNode, genNode, KnowledgeBase.HPR_ISA, SourceType.QURAN);
				
				added++;
			}
		}
		
		Common.log("loaded " + added + " `SubClassOf` (ISA) relations and skipped " + bad + " ill-formded ones and skipped " + skippedTemporary + " with temporary roots.");
	}
	
	/**
	 * Loads `ClassAssertion` relations.
	 * ClassAssertion is the same as Instance-Of in other knowledge bases. We treat it as ISA relations.
	 * @param doc The parsed XML document
	 */
	private void loadClassAssertion(Document doc)
	{
		NodeList nList1 = doc.getElementsByTagName("ClassAssertion");
		
		int added = 0;
		int skippedTemporary = 0;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;
							
				SourcedConcept destination	= cleanConceptName(Common.getTagAttribute("Class", "IRI", eElement));
				SourcedConcept source 		= cleanConceptName(Common.getTagAttribute("NamedIndividual", "IRI", eElement));
				
				if (IGNORE_TEMPORARY_ROOTS && (destination.name.equals("موقت") || destination.name.equals("شکیات")))
				{
					skippedTemporary++;
					continue;
				}
				
				ir.ac.itrc.qqa.semantic.kb.Node sourceNode 		= _kb.addConcept(source.name, source.source);
				ir.ac.itrc.qqa.semantic.kb.Node destinationNode = _kb.addConcept(destination.name, destination.source);
				
				// TODO: what about the SourceType of ISA relation?
				_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_ISA, SourceType.QURAN);
				
				added++;
			}
		}
		
		Common.log("loaded " + added + " `ClassAssertion` (ISA) relations, skipped " + skippedTemporary + " with temporary roots.");
	}
	
	/**
	 * Loads `ObjectPropertyAssertion` relations in to memory.
	 * These are custom relations between concepts.
	 * @param doc The parsed XML document
	 */
	private void loadObjectPropertyAssertion(Document doc)
    {
		HashSet<String> profiler = new HashSet<String>();
		
		NodeList nList1 = doc.getElementsByTagName("ObjectPropertyAssertion");
		
		int added = 0;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;				
				
				SourcedConcept relation 	= cleanConceptName(Common.getTagAttribute("ObjectProperty", "IRI", eElement));
				
				NodeList children = eElement.getElementsByTagName("NamedIndividual");
				
				SourcedConcept source 		= cleanConceptName(children.item(0).getAttributes().getNamedItem("IRI").getNodeValue());
				SourcedConcept destination 	= cleanConceptName(children.item(1).getAttributes().getNamedItem("IRI").getNodeValue());
				
				ir.ac.itrc.qqa.semantic.kb.Node sourceNode 		= _kb.addConcept(source.name, source.source);
				ir.ac.itrc.qqa.semantic.kb.Node destinationNode = _kb.addConcept(destination.name, destination.source);
				
				// we treat `name-of` specially: we put all name-of destinations into a synset
				if (relation.name.equals("نام دیگر"))
				{
					_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_SIM, SourceType.QURAN);
					
					_kb.addRelationReciprocal(destinationNode, destinationNode, KnowledgeBase.HPR_ALIAS, SourceType.QURAN); // adding the synset node as a member too
				}
				else if (relation.name.equals("جزء"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					_kb.addRelation(destinationNode, sourceNode, KnowledgeBase.HPR_PARTOF, SourceType.QURAN);
				}
				else if (relation.name.equals("ارجاع آیه"))
				{
					//_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_REF, SourceType.QURAN);
					_kb.addRelation(destinationNode, sourceNode, KnowledgeBase.HPR_VERSE_TOPIC, SourceType.QURAN);
				}
				else
				{
					ir.ac.itrc.qqa.semantic.kb.Node relationNode = _kb.addConcept(relation.name, relation.source);
					
					_kb.addRelation(sourceNode, destinationNode, relationNode, SourceType.QURAN);
					
					profiler.add(relationNode.getName());
					
					if (_hashsetSymmetricObjectProperties.contains(relationNode))
					{					
						_kb.addRelation(destinationNode, sourceNode, relationNode, SourceType.QURAN);
						
						added++;
					}
				}
					
				added++;
			}
		}
		
		Common.log("loaded " + added + " `ObjectPropertyAssertion` relations.");
		
		// profiler
		
		Common.print("Notice: following relations not mapped to specific HRP nodes:");
		Common.printInline("\t");
		
		for (String item: profiler)
		{
			Common.printInline("'" + item + "' -  ");
		}
		
		Common.print();
    }
	
	/**
	 * Loads `DataPropertyAssertion` relations into memory.
	 * These are custom relations between concepts.
	 * @param doc The parsed XML document
	 */
	private void loadDataPropertyAssertion(Document doc)
    {
		HashSet<String> profiler = new HashSet<String>();
		
		NodeList nList1 = doc.getElementsByTagName("DataPropertyAssertion");
		
		int addedRelations = 0;
		int addedReferences = 0;
		int addedNestedAnnotations = 0;
		int mapped = 0;
		int notMapped = 0;
		int skippedRelations = 0;
		
		ArrayList<ir.ac.itrc.qqa.semantic.kb.Node> references = new ArrayList<ir.ac.itrc.qqa.semantic.kb.Node>();
		PlausibleStatement ps;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;
				
				SourcedConcept relation 	= cleanConceptName(Common.getTagAttribute("DataProperty", "IRI", eElement));
				SourcedConcept source 		= cleanConceptName(Common.getTagAttribute("NamedIndividual", "IRI", eElement));
				SourcedConcept destination 	= cleanConceptName(Common.getTagValue("Literal", eElement, 0));

				if (relation == null || source == null || destination == null)
				{
					reportIncompleteRelation(source, relation, destination);
					continue;
				}
				
				ps = null;
				references.clear();
				destination.name = cleanChapterVerseReferenceFromConceptName(destination.name, references);
				
				ir.ac.itrc.qqa.semantic.kb.Node sourceNode 		= _kb.addConcept(source.name, source.source);
				ir.ac.itrc.qqa.semantic.kb.Node destinationNode;
				ir.ac.itrc.qqa.semantic.kb.Node relationNode;
				
				if (source.name.equals("آیه 214 سوره شعرا") && destination.name.equals("آیه انذار"))
				{
					int kkk = 0;
					kkk++;
				}
				
				if (relation.name.equals("تعریف"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.setDescriptive(true);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_GLOSS, SourceType.QURAN);
				}
				else if (relation.name.equals("نام مستعار") || relation.name.equals("آیه خاص"))
				{
					if (source.name.equals(destination.name))
						continue;
					
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					_kb.addRelation(destinationNode, sourceNode, KnowledgeBase.HPR_SIM, SourceType.QURAN);
					
					// has-alias is reciprocal
					ps = _kb.addRelationReciprocal(sourceNode, destinationNode, KnowledgeBase.HPR_ALIAS, SourceType.QURAN);
				}
				else if (mapToFarsnet && relation.name.equals("شناسه فارس نت")) 
				{
					destinationNode = _kb.findConcept(destination.name);
					
					if (destinationNode != null && !destination.equals("*"))
					{
						//TODO: ps must be SIM or the equavalency relation?!
						
						_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_SIM, SourceType.QURAN);
						
						ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_EQUAVALENCY, SourceType.QURAN);
						
						mapped++;
					}
					else
					{
						if (!destination.name.equals("*"))
							Common.log("\tnot mapped to farsnet: " + destination.name);
						
						notMapped++;
					}
				}
				else if (mapToFarsnet && relation.name.equals("dReference-Chapter-Verse")) //-------------------------------------------
				{
					parseChapterVerse(destination.name, sourceNode);
				}
				else if (relation.name.equals("revelation-cause"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.setDescriptive(true);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_VERSE_REVELATION_CAUSE);
				}
				else if (relation.name.equals("توضیح"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.setDescriptive(true);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_EXPLANATION);					
				}
				else if (relation.name.equals("ترجمه فارسی"))
				{
					/* this relation in the ontology is buggy. ignoring it.
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.isDescriptive = true;
					
					_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_VERSE_FARSI);
					*/
					
					skippedRelations++;
				}
				else if (relation.name.equals("متن عربی"))
				{
					/* this relation in the ontology is buggy. ignoring it.
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.isDescriptive = true;
					
					_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_VERSE_ARABIC);
					*/
					
					skippedRelations++;
				}
				else if (relation.name.equals("تعداد آیه"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_CHAPTER_VERSE_COUNT);
				}
				else if (relation.name.equals("موضوع"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_VERSE_TOPIC);
				}
				else if (relation.name.equals("خواص سوره") || relation.name.equals("خواص آیه"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_PROPERTY);
				}
				else if (relation.name.equals("سوره فضیلت"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_ADVANTAGE);
				}
				else if (relation.name.equals("توضیح سوره"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_CHAPTER_TOPIC);
				}
				else //------------------------------------------------------------------------------------------
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					relationNode 	= _kb.addConcept(relation.name, relation.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, relationNode, relation.source);
					
					profiler.add(relationNode.getName());
				}
				
				if (ps != null && references.size() > 0)
				{
					for (ir.ac.itrc.qqa.semantic.kb.Node reference: references)
					{
						_kb.addRelation(ps, reference, KnowledgeBase.HPR_REF, SourceType.QURAN);
						
						addedReferences++;
					}
				}
				
				if (ps != null) // treating the nested Annotation, if any -------------------------------------
				{
					addedNestedAnnotations += extractNestedAnnotations(eElement, ps);
				}
				
				addedRelations++;
			}
		}
		
		addedRelations -= skippedRelations;
		
		Common.log("loaded " + addedRelations + " `DataPropertyAssertion` relations + " + addedReferences + " inline references and " + addedNestedAnnotations + " standalone references.");
		
		if (notMapped > 0)
			Common.log(" Mapped " + mapped + ", failed to map " + notMapped + " concepts.");
		else
			Common.log();
		
		// profiler
		
		Common.print("Notice: following relations not mapped to specific HRP nodes:");
		Common.printInline("\t");
		
		for (String item: profiler)
		{
			Common.printInline("'" + item + "' -  ");
		}
		
		Common.print();
    }
	
	private void reportIncompleteRelation(SourcedConcept source, SourcedConcept relation, SourcedConcept destination)
	{
		String msg = "";
		
		msg += (msg == null)? "NULL" : source.name;
		msg += " -- ";
		msg += (relation == null)? "NULL" : relation.name;
		msg += " -> ";
		msg += (destination == null)? "NULL" : destination.name;
		
		Common.log("\tincomplete relation: " + msg);
	}
	
	private int extractNestedAnnotations(Element eElement, PlausibleStatement ps)
	{
		int added = 0;
		
		NodeList nList = eElement.getElementsByTagName("Annotation");
		
		for (int i = 0; i < nList.getLength(); i++)
		{
			Element annotation = (Element)nList.item(i);
			
			SourcedConcept relation 	= cleanConceptName(Common.getTagAttribute("AnnotationProperty", "IRI", annotation));
			SourcedConcept destination 	= cleanConceptName(Common.getTagValue("IRI", annotation, 0));
			
			if (destination == null)
				destination = cleanConceptName(Common.getTagValue("Literal", annotation, 0));
			
			ir.ac.itrc.qqa.semantic.kb.Node destinationNode = _kb.addConcept(destination.name);
			
			if (relation.name.equals("reference"))
			{
				_kb.addRelation(ps, destinationNode, KnowledgeBase.HPR_REF);
				
				added++;
			}
			else if (relation.name.equals("instanceOf")) // to handle weird subtopic relations
			{
				if (ps.argument.getName().startsWith("آیه "))
				{				
					_kb.addRelation(destinationNode, ps.referent, _kb.addConcept("has-subTopic"));
				
					added++;
				}
			}
		}		
		
		return added;
	}
	
	private void parseChapterVerse(String destination, ir.ac.itrc.qqa.semantic.kb.Node sourceNode)
	{
		String[] parts = destination.split("/");
		
		if ((parts.length != 2))
		{
			Common.log("\tquran chapter/verser not parsed: " + destination);
			return;
		}
		
		String chapter = parts[0].trim();					
		String verse = parts[1].trim();
		
		ArrayList<String> verseArray = new ArrayList<String>();
		
		if (verse.contains(":"))
		{
			String[] verseRanges = verse.split(":");
			
			if (verseRanges.length != 2)
			{
				Common.log("\tquran verse range not parsed: " + destination);
				return;
			}
			
			int start = Integer.parseInt(verseRanges[0]);
			int end = Integer.parseInt(verseRanges[1]);
			
			for (Integer i = start; i <= end; i++)
			{
				verseArray.addAll(Arrays.asList(i.toString()));
			}
		}
		else 
		{
			verseArray.add(verse);
		}
		
		//---
		
		for (String verseNo: verseArray)
		{
			String verseName = "آیه " + verseNo + " سوره " + chapter;
			
			ir.ac.itrc.qqa.semantic.kb.Node verseNode = _kb.findConcept(verseName);
			
			if (verseNode == null)
			{
				Common.log("\tnot mapped to quran chapter: " + chapter);
			}
			else
			{
				_kb.addRelation(sourceNode, verseNode, KnowledgeBase.HPR_REF, SourceType.QURAN);
			}
		}
	}
	
	/**
	 * Loads `AnnotationAssertions` relations into memory. 
	 * These are custom relations between concepts and literal (we tread literals as concepts)
	 * @param doc The parsed XML document
	 */
	private void loadAnnotationAssertions(Document doc)
    {
		NodeList nList1 = doc.getElementsByTagName("AnnotationAssertion");
		
		int addedRelations = 0;
		int addedReferences = 0;
		int emptyNodes = 0;
		
		SourcedConcept source = new SourcedConcept(); 
		SourcedConcept destination = new SourcedConcept();
		SourcedConcept relation = new SourcedConcept();
		
		ir.ac.itrc.qqa.semantic.kb.Node destinationNode;
		ir.ac.itrc.qqa.semantic.kb.Node relationNode;
		ir.ac.itrc.qqa.semantic.kb.Node sourceNode;
		
		ArrayList<ir.ac.itrc.qqa.semantic.kb.Node> references = new ArrayList<ir.ac.itrc.qqa.semantic.kb.Node>();
		PlausibleStatement ps;
		
		int iLength1 = nList1.getLength();
		
		for (int i = 0; i < iLength1; i++)
		{
			Node node = nList1.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) 
			{
				Element eElement = (Element)node;
				
				source.name = Common.getTagValue("IRI", eElement, 0);				
				
				destination.name = Common.getTagValue("Literal", eElement, 0);
				if (destination.name == null)
					destination.name = Common.getTagValue("IRI", eElement, 1);
				
				relation.name = Common.getTagAttribute("AnnotationProperty", "IRI", eElement);

				if (source.name == null || destination.name == null || relation.name == null)
				{
					emptyNodes++;
					continue;
				}
				
				source 		= cleanConceptName(source.name);
				destination = cleanConceptName(destination.name);
				relation 	= cleanConceptName(relation.name);
				
				if (source.name.isEmpty() || destination.name.isEmpty() || relation.name.isEmpty())
				{
					emptyNodes++;
					continue;
				}
				
				
				ps = null;
				references.clear();
				destination.name = cleanChapterVerseReferenceFromConceptName(destination.name, references);
				
								
				sourceNode = _kb.addConcept(source.name, source.source);
				
				if (relation.name.equals("تعریف"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.setDescriptive(true);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_GLOSS, SourceType.QURAN);
				}
				else if (relation.name.equals("مرتبط با"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_RELATED, SourceType.QURAN);
				}
				else if (mapToFarsnet && relation.name.equals("aReference-Chapter-Verse")) //-------------------------------------------
				{
					parseChapterVerse(destination.name, sourceNode);
				}
				else if (relation.name.equals("ارجاع آیه"))
				{
					destinationNode = _kb.findConcept(destination.name);
					
					//ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_REF, relation.source);
					ps = _kb.addRelation(destinationNode, sourceNode, KnowledgeBase.HPR_VERSE_TOPIC, relation.source);
				}
				else if (mapToFarsnet && relation.name.equals("شناسه فارس نت"))
				{
					destinationNode = _kb.findConcept(destination.name);
					
					if (destinationNode == null)
					{
						Common.log("\tnot mapped to farsnet: " + destination.name);
						continue;
					}
					
					_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_SIM, relation.source);					
					
					ps = _kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_EQUAVALENCY, relation.source);
					
				}
				else if (relation.name.equals("نام مستعار"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					
					_kb.addRelation(destinationNode, sourceNode, KnowledgeBase.HPR_SIM, SourceType.QURAN);
					
					ps = _kb.addRelationReciprocal(sourceNode, destinationNode, KnowledgeBase.HPR_ALIAS, SourceType.QURAN);
				}
				else if (relation.name.equals("توضیح"))
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					destinationNode.setDescriptive(true);
					
					_kb.addRelation(sourceNode, destinationNode, KnowledgeBase.HPR_EXPLANATION);					
				}
				else //-------------------------------------------
				{
					destinationNode = _kb.addConcept(destination.name, destination.source);
					relationNode 	= _kb.addConcept(relation.name, relation.source);
					
					ps = _kb.addRelation(sourceNode, destinationNode, relationNode, relation.source);
				}
				
				if (ps != null && references.size() > 0)
				{
					for (ir.ac.itrc.qqa.semantic.kb.Node reference: references)
					{
						_kb.addRelation(ps, reference, KnowledgeBase.HPR_REF, SourceType.QURAN);
						
						addedReferences++;
					}
				}
				
				addedRelations++;
			}
		}
		
		Common.log("loaded " + addedRelations + " `AnnotationProperty` relations plus " + addedReferences + " references. Found and skipped " + emptyNodes + " empty concept names!");
    }
	
	/**
	 * Tries to extract chapter-verse numbers from a concept name based on regular expressions. it also trims the concept name
	 * @param name the name that may contain chapter-verse patterns
	 * @param references the chapter-verse references found in the concept name
	 * @return the trimmed concept name
	 */
	private String cleanChapterVerseReferenceFromConceptName(String name, ArrayList<ir.ac.itrc.qqa.semantic.kb.Node> references)
	{
		String[] parts = name.split("\\+");
		
		if (parts.length == 1)
			return name;
		
		references.clear();
		
		name = parts[0];
		
		if (!mapToFarsnet)
			return name;
		
		Pattern pattern = Pattern.compile("[آا]یه\\s*([\\d:]+)([\\s]?سوره[\\s]?)?(.*)");
		Matcher matcher;
		
		for (int i = 1; i < parts.length; i++)
		{
			matcher = pattern.matcher(parts[i]);
			
			if (matcher.find())
			{
				String verse = matcher.group(1).trim();
				String chapter = matcher.group(3).trim();
				
				ArrayList<String> verseArray = new ArrayList<String>();
				
				if (verse.contains(":"))
				{
					String[] verseRanges = verse.split(":");
					
					if (verseRanges.length != 2)
					{
						Common.log("\tquran composite verse range not parsed: " + parts[i]);
					}
					
					int start = Integer.parseInt(verseRanges[0]);
					int end = Integer.parseInt(verseRanges[1]);
					
					for (Integer j = start; j <= end; j++)
					{
						verseArray.addAll(Arrays.asList(j.toString()));
					}
				}
				else 
				{
					verseArray.add(verse);
				}
				
				for (String verseParsed: verseArray)
				{
					String ref = "آیه " + verseParsed + " سوره " + chapter;
					
					ir.ac.itrc.qqa.semantic.kb.Node node = _kb.findConcept(ref);
					
					if (node == null)
					{
						Common.log("\tquran composite chapter-verse not found: " + parts[i]);
						continue;
					}
					
					references.add(node);					
				}
			}
			else
			{
				Common.log("\tquran composite chapter-verse not parsed: " + parts[i]);
			}
		}
		
		return name;
	}
	
	/**
	 * Cleans concept names by removing unwanted characters.
	 * 
	 * @param name The concept name
	 * @return The cleaned concept name ready to be inserted into the memory
	 */
	private SourcedConcept cleanConceptName(String name)
	{
		SourcedConcept out = new SourcedConcept();
		
		if (name == null || name.isEmpty())
		{
			return null;
		}			
		
		if (name.startsWith("farhangq:"))
		{
			name = name.substring("farhangq:".length());
			out.source = SourceType.FARHANG_QURAN;
		}
		else if (name.startsWith("quran:"))
		{
			name = name.substring("quran:".length());
			out.source = SourceType.QURAN;
		}
		else if (name.startsWith("#"))
		{
			name = name.substring("#".length());
			out.source = SourceType.QURAN;
		}
		else
		{
			out.source = SourceType.QURAN;
		}
		
		if (name.toLowerCase().equals("concept"))
		{
			out.name = "مفهوم";
			return out;
		}
		else if (name.equals("معجزه(مفهوم)") || name.equals("معجزه (مفهوم)"))
		{
			name = "معجزه";
		}
		
		name = name.replaceAll("_", " ");
		
		// TODO: apply real normalization here!
		name = Common.normalizeNotTokenized(name);
		
		if (name.endsWith("-رابطه"))
		{
			name = name.substring(0, name.length() - "-رابطه".length());
		}
		
		if (name.endsWith("-نمونه"))
		{
			name = name.substring(0, name.length() - "-نمونه".length());
		}
		
		if (name.endsWith("-مفهوم"))
		{
			name = name.substring(0, name.length() - "-مفهوم".length());
		}
		
		if (name.endsWith("(مفهوم)"))
		{
			name = name.substring(0, name.length() - "(مفهوم)".length());
		}
		
		out.name = name.trim();
		
		return out;
	}
}
