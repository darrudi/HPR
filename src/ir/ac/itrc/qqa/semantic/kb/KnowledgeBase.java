package ir.ac.itrc.qqa.semantic.kb;

import ir.ac.itrc.qqa.semantic.enums.LexicalType;
import ir.ac.itrc.qqa.semantic.enums.KbOperationMode;
import ir.ac.itrc.qqa.semantic.enums.KnowledgebaseLoadMode;
import ir.ac.itrc.qqa.semantic.enums.ConceptType;
import ir.ac.itrc.qqa.semantic.enums.POS;
import ir.ac.itrc.qqa.semantic.enums.ConditionalType;
import ir.ac.itrc.qqa.semantic.enums.PreprocessorType;
import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.enums.StringMatch;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;
import ir.ac.itrc.qqa.semantic.util.MyError;
import ir.ac.itrc.qqa.semantic.util.Common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * This class implements the knowledge base core.
 *
 * @author Ehsan Darrudi
 */

public class KnowledgeBase
{	
	/** if true then the old node information is overridden by the new one on AddConcept */
	private boolean _overrideSourceTypeOnAddition = false;
	
	/** if set then only one way of a bi-directional relation is loaded */
	private boolean _ignoreBidirectionalRelationsOnImport = false;
	
	/** Defines the loading mode of _kb. When in abnormal conditions like "IMPORT" the kb does not trim concept's names when adding. It is used only when importing memory dumps into memory. */ 
	private KnowledgebaseLoadMode _loadingMode = KnowledgebaseLoadMode.NORMAL;
	
	/** holds the name of loaded KBs */
	private ArrayList<String> _loadedKbs = new ArrayList<String>(); 
	
	/** Keeps track of all nodes in the semantic net.We can look up a node in the semantic net using its name as the key in the NodesHash hash table */
	private Hashtable<String, Node> _nodes;
	
	/** Keeps track of all nodes in the semantic net by their unique IDs */
	private Hashtable<Integer, Node> _nodeIds;
	
	/** It is used to assign permanent unique IDs (i.e. do not change over different program runs) to concept names */
	private Hashtable<String, Integer> _nodePermanentIds;
	/** path to the file containing permanent ids in case we want to add a new node name to it */
	private String _permanentIdFilename = null;
	/** Serialization for _nodePermanentIds */
	private BufferedWriter _permanentIdFile = null;
	/** keeps track of the last id assigned to nodes **/
	private static int _lastIdAssigned = 0;
	
	/** it keeps track of the node's name variations after normalization; گل --> گِل و گُل  */
	private Multimap<String, Node> _nodesReversedTokenized;
	
	/** it keeps track of the node's name variations after lemmatization; گل --> گلها و گلهای  */
	private Multimap<String, Node> _nodesReversedLemmatized;
	
	/** Initial capacity of the KB used to initialize the hashtables */
	public static final int NODES_HASH_SIZE = 1000003;
	
	// The below hashtables are just added to compute statistical data. 
	private Hashtable<String, Node> _descriptorTypes;
	private Hashtable<String, Node> _arguments;
	private Hashtable<String, Node> _referents;

	/** All nodes added in STATIC mode (default) stand memory purges. On the other hand nodes added in DYNAMIC mode will be erased in next call to `PurgeDynamicKnowledge` function. */ 
	private KbOperationMode _operationMode = KbOperationMode.STATIC;
	
	// HPR core concepts and relations: read original HPR paper for a detailed description
	/** Hierarchical ISA relation */
	public static Node HPR_ISA;
	
	/** Inverse of ISA relation */
	public static Node HPR_HASTYPE;
	
	/** Hierarchical Part-of relation */
	public static Node HPR_PARTOF;
	
	/** Hierarchical HasPart (inverse of 'Part-of') relation */
	public static Node HPR_HASPART;
	
	/** Instances-Of relation */
	public static Node HPR_INSTANCE;
	
	/** Similarity relation */
	public static Node HPR_SIM;
	
	/** Dissimilarity relation */
	public static Node HPR_DIS;
	
	/** Dependency relation */
	public static Node HPR_DEP;
	
	/** POSITIVE Dependency relation */
	public static Node HPR_DEPP;
	
	/** NEGATIVE Dependency relation */
	public static Node HPR_DEPN;
	
	/** Implication relation */
	public static Node HPR_IMP;
	
	/** IS relation conceptType: added to support omitted properties such as "The sky is blue" where the property "color" has been omitted. It works hand in hand with ATTRIBUTE relations */
	public static Node HPR_IS;
	
	// Lexical relations to work with lexicons
	
	/** Lexical representation of concepts. It can be considered the inverse of SYN sometimes */
	public static Node HPR_LEX;
	
	/** Wordnet-conceptType Synonymy relation conceptType */
	public static Node HPR_SYN;
	
	/** Wordnet-conceptType glossary definition of the concept */
	public static Node HPR_GLOSS;
	
	/** Wordnet-conceptType example for a concept */
	public static Node HPR_EXAMPLE;
	
	/** Wordnet-conceptType Causes relation */
	public static Node HPR_CAUSES;

	/** Used to link a known to its attributes (e.g. human => height, age, skin color, etc/) */
	public static Node HPR_ATTRIBUTE;
	
	/** HPR special node for `high`, used in marked dependency relations */ 
	public static Node HPR_HIGH;
	/** HPR special node for `low`, used in marked dependency relations */
	public static Node HPR_LOW;
	
	/** A `yes` answer, used in yes/no questions */
	public static Node HPR_YES;
	/** A `no` answer, used in yes/no questions */
	public static Node HPR_NO;
	
	/** General context in HPR */
	public static Node HPR_CX;
	/** Context for time */
	public static Node HPR_CXTIME;
	/** Context for source */	
	public static Node HPR_CXDOMAIN;
	/** Context for location */
	public static Node HPR_CXLOCATION;
	
	/** Past time, used in conjunction with CXTIME */
	public static Node HPR_PAST;
	/** Present time, used in conjunction with CXTIME */
	public static Node HPR_TIME_PRESENT;
	/** Future time, used in conjunction with CXTIME */
	public static Node HPR_TIME_FUTURE;
	
	/** Comparison question conceptType */
	public static Node HPR_COMPARE;
	/** Similarity question conceptType */
	public static Node HPR_SIMILARITY;
	/** Difference question conceptType */
	public static Node HPR_DIFFERENCE;
	/** All-information question conceptType */
	public static Node HPR_KNOWLEDGE_DUMP;
	
	/** Inverse relation conceptType */
	public static Node HPR_INVERSE;
		
	/** Question-not-mapped answer */
	public static Node HPR_NOT_MAPPED;
	/** Answer-not-mapped answer */
	public static Node HPR_NOT_FOUND;
	
	/** General concept for denote anything */
	public static Node HPR_ANY;
	
	/** Related-to relation conceptType */
	public static Node HPR_RELATED;
	
	/** Reference relations */
	public static Node HPR_REF;
	
	/** verse concept */
	public static Node HPR_VERSE;
	/** chapter concept */
	public static Node HPR_CHAPTER;
	/** Farsi representation string of a concept */
	public static Node HPR_VERSE_FARSI;
	/** Arabic representation string of a concept */
	public static Node HPR_VERSE_ARABIC;
	/** Verse revelation cause (Shan-e Nozool) */
	public static Node HPR_VERSE_REVELATION_CAUSE;	
	/** the relation from a chapter to its verse count, used fot enumerating verses */
	public static Node HPR_CHAPTER_VERSE_COUNT;	
	/** to represent verse topics */
	public static Node HPR_VERSE_TOPIC;
	/** aliases */
	public static Node HPR_ALIAS;
	/** explanation */
	public static Node HPR_EXPLANATION;
	/** to hold concepts' properties */
	public static Node HPR_PROPERTY;
	/** verse's advantages 'فضیلت' */
	public static Node HPR_ADVANTAGE;
	/** chapter's topic */
	public static Node HPR_CHAPTER_TOPIC;
	
	/** to represent equivalency relations between inter-kb (inter-ontology) concepts */
	public static Node HPR_EQUAVALENCY;
	
	/** holds the template for converting a relation to natural language sentences */
	public static Node HPR_TEMPLATE;
	
	/** link to outside resources (currently, only wikipedia) */
	public static Node HPR_HREF;
	
	/** if true then no change can be make to the kb, to support concurrency it must be set to make data read-only */
	private boolean _locked = false;
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	/**
	 * Main constructor
	 */
	public KnowledgeBase()
	{
		_nodes = new Hashtable<String, Node>(NODES_HASH_SIZE);
		_nodeIds = new Hashtable<Integer, Node>(NODES_HASH_SIZE);
		
		_nodePermanentIds = new Hashtable<String, Integer>(NODES_HASH_SIZE);

		_descriptorTypes = new Hashtable<String, Node>(1000);		
		_arguments = new Hashtable<String, Node>(NODES_HASH_SIZE/3);
		_referents = new Hashtable<String, Node>(NODES_HASH_SIZE/3);
		
		_nodesReversedTokenized = ArrayListMultimap.create(NODES_HASH_SIZE, 2);
		_nodesReversedLemmatized = ArrayListMultimap.create(NODES_HASH_SIZE, 2);

		loadCorePlausibleRelationTypes();
	}

	
	/**
	 * Locks the kb. Afterwards no change can be make to the kb. 
	 * It is used to support concurrency when the kb is shared among threads. 
	 */
	public void lock()
	{
		_locked = true;
	}
	/**
	 * Unlocks the kb making it once more available for changes.
	 */
	public void unluck()
	{
		_locked = false;
	}
	/**
	 * Checks the locking state. If locked raises an error.
	 */
	private void checkLock()
	{
		if (_locked)
			MyError.exit("The knowledge base is locked. You cannot change it unless you call 'unlock()' method!");
	}
	
	
	/**
	 * Setter for <code>_ignoreBidirectionalRelationsOnImport</code>
	 */
	public void setIgnoreBidirectionalRelationsOnImport()
	{
		checkLock();
		
		_ignoreBidirectionalRelationsOnImport = true;
	}
	/**
	 * Getter for <code>_ignoreBidirectionalRelationsOnImport</code>
	 * @return
	 */
	public boolean getIgnoreBidirectionalRelationsOnImport()
	{
		return _ignoreBidirectionalRelationsOnImport;
	}
	
	
	/**
	 * Adds a KB name to the list of loaded KBs for statistical purposes only.
	 * @param kbName The KB name just loaded
	 */
	public void addLoadedKb(String kbName)
	{
		checkLock();
		
		_loadedKbs.add(kbName);
	}
	
	/**
	 * Provides the list od loaded KBs so far.
	 * @return The list of KBs loaded so far
	 */
	public ArrayList<String> getLoadedKbs()
	{
		return _loadedKbs;
	}

	/**
	 * Loads HPR's core relations into memory
	 */
	private void loadCorePlausibleRelationTypes()
	{
		// Plausible Relations:
		HPR_ISA 		= addConcept("ISA").setSourceType(SourceType.CORE);			// ISA
		HPR_HASTYPE		= addConcept("انواع").setSourceType(SourceType.CORE);			// ISA reversed
		HPR_PARTOF 		= addConcept("جزئی از").setSourceType(SourceType.CORE);		// PrtOf
		HPR_HASPART 	= addConcept("جزئی دارد").setSourceType(SourceType.CORE);		// PrtOf
		HPR_INSTANCE 	= addConcept("نمونه‌ای از").setSourceType(SourceType.CORE);		// Instances
		HPR_SIM	 		= addConcept("مشابه").setSourceType(SourceType.CORE);			// Similarity
		HPR_DIS			= addConcept("متضاد").setSourceType(SourceType.CORE);			// Dissimilarity
		HPR_DEP		 	= addConcept("وابستگی").setSourceType(SourceType.CORE);		// UNMARKED Dependency
		HPR_DEPP 		= addConcept("وابستگی مستقیم").setSourceType(SourceType.CORE);	// POSITIVE Dependency
		HPR_DEPN 		= addConcept("وابستگی معکوس").setSourceType(SourceType.CORE);	// NEGATIVE Dependency 
		HPR_IMP 		= addConcept("اگر").setSourceType(SourceType.CORE);			// Implications
		HPR_LEX 		= addConcept("نمود لغوی").setSourceType(SourceType.CORE);		// to connect Concepts to Lex nodes
		HPR_SYN		 	= addConcept("عضو هم‌نشیم").setSourceType(SourceType.CORE);	// Synonym
		HPR_GLOSS 		= addConcept("تعریف").setSourceType(SourceType.CORE);		// Definition
		HPR_EXAMPLE 	= addConcept("مثال").setSourceType(SourceType.CORE);			// Example sentences from WN
		HPR_CAUSES 		= addConcept("باعث می‌شود").setSourceType(SourceType.CORE);	// WN Causes
		HPR_IS 			= addConcept("هست").setSourceType(SourceType.CORE);			// Property
		HPR_ATTRIBUTE 	= addConcept("ویژگی").setSourceType(SourceType.CORE);		// WN Attribute		
		HPR_REF 		= addConcept("ارجاع").setSourceType(SourceType.CORE);
		HPR_EQUAVALENCY = addConcept("مترادف").setSourceType(SourceType.CORE);
		
		HPR_PROPERTY	= addConcept("خواص").setSourceType(SourceType.QURAN);
		HPR_ADVANTAGE	= addConcept("فضیلت").setSourceType(SourceType.QURAN);;
		HPR_CHAPTER_TOPIC = addConcept("توضیح سوره").setSourceType(SourceType.QURAN);;
		
		//(con|dis)junctives:
		//AddConcept("AND");	// Conjunction
		//AddConcept("OR");		// Disjunction
		
		// Simple Plausible Answers:
		HPR_YES 		= addConcept("YES").setSourceType(SourceType.CORE);
		HPR_NO 			= addConcept("NO").setSourceType(SourceType.CORE);
		
		// plausible quantitative concepts used in DDEP
		HPR_HIGH 		= addConcept("HIGH").setSourceType(SourceType.CORE);
		HPR_LOW 		= addConcept("LOW").setSourceType(SourceType.CORE);
		//AddRelation(HPR_YES, HPR_NO, HPR_DIS, new CertaintyParameters()); // ??? DIS?

		HPR_CX 			= addConcept("همبافت").setSourceType(SourceType.CORE);		// General Context
		HPR_CXTIME 		= addConcept("CX:TIME").setSourceType(SourceType.CORE);	// Time Context
		HPR_CXLOCATION 	= addConcept("CX:PLACE").setSourceType(SourceType.CORE);	// Place Context
		HPR_CXDOMAIN 	= addConcept("CX:DOMAIN").setSourceType(SourceType.CORE);	// Domain Context

		// Temporal
		HPR_PAST 		= addConcept("PAST").setSourceType(SourceType.CORE);
		HPR_TIME_PRESENT = addConcept("PRESENT").setSourceType(SourceType.CORE);
		HPR_TIME_FUTURE = addConcept("FUTURE").setSourceType(SourceType.CORE);

		// Non HPR multi argument inferences
		HPR_COMPARE 	= addConcept("COMPARE").setSourceType(SourceType.CORE);		// for Compare (compare, similarity, difference) questions
		HPR_SIMILARITY 	= addConcept("SIMILARITY").setSourceType(SourceType.CORE);	// for Similarities questions
		HPR_DIFFERENCE 	= addConcept("DIFFERENCE").setSourceType(SourceType.CORE);	// for Differences questions
		HPR_INVERSE 	= addConcept("INVERSE").setSourceType(SourceType.CORE);
		HPR_KNOWLEDGE_DUMP = addConcept("KNOWLEDGE_DUMP").setSourceType(SourceType.CORE);
		
		HPR_ANY 		= addConcept("ANY").setSourceType(SourceType.CORE);
		
		HPR_RELATED 	= addConcept("مرتبط با").setSourceType(SourceType.CORE);
		
		// Quran specific
		HPR_VERSE					= addConcept("آیه").setSourceType(SourceType.QURAN);
		HPR_CHAPTER					= addConcept("سوره").setSourceType(SourceType.QURAN);
		HPR_VERSE_FARSI 			= addConcept("ترجمه فارسی").setSourceType(SourceType.QURAN);
		HPR_VERSE_ARABIC 			= addConcept("متن عربی").setSourceType(SourceType.QURAN);
		HPR_VERSE_REVELATION_CAUSE 	= addConcept("شان نزول").setSourceType(SourceType.QURAN);
		HPR_CHAPTER_VERSE_COUNT		= addConcept("تعداد آیات").setSourceType(SourceType.QURAN);
		HPR_VERSE_TOPIC				= addConcept("موضوع").setSourceType(SourceType.QURAN);
		HPR_ALIAS					= addConcept("معروف به").setSourceType(SourceType.QURAN);
		HPR_EXPLANATION				= addConcept("توضیح").setSourceType(SourceType.QURAN);
		
		HPR_NOT_MAPPED 				= addConcept("QUESTION NOT MAPPED!").setSourceType(SourceType.CORE);
		HPR_NOT_FOUND 				= addConcept("ANSWER NOT FOUND!").setSourceType(SourceType.CORE);
		
		HPR_TEMPLATE				= addConcept("الگو").setSourceType(SourceType.CORE);
		HPR_HREF					= addConcept("لینک").setSourceType(SourceType.CORE);
	}
	
	
	public Node addConcept(String Name)
	{
		return addConcept(Name, true, SourceType.UNKNOWN);
	}	
	public Node addConcept(String Name, boolean trimName)
	{
		return addConcept(Name, trimName, SourceType.UNKNOWN);
	}
	public Node addConcept(String Name, SourceType source)
	{
		return addConcept(Name, true, source);
	}
	/**
	 * Adds a new node to the KB
	 * 
	 * @param name The new node's name
	 * @return A reference to the newly created node or reference to the node if already existed
	 */
	public Node addConcept(String name, boolean trimName, SourceType source)
	{
		checkLock();
		
		if (name == null || name.isEmpty())
		{
			MyError.exit("The concept name can't be empty!");
		}
		
		String searchName = name;
		
		// When importing KBs search name may be different from actual name, e.g. f:test vs. test
		searchName = Node.extractProspectiveName(name);		
		
		if (isInMemory(searchName)) 
		{
			Node already = (Node)_nodes.get(searchName.toLowerCase());
			
			if (_overrideSourceTypeOnAddition && source != SourceType.UNKNOWN)
				already.setSourceType(source);
			
			return already;
		}
		
		// Tashdid _ّ__ character is a special case. many concepts names in Farsi and Arabic can be written with or without it.
		// so we do not add a new concept when the Tashdid-less version already exists in the kb.
		// dictionary entries (having '#' in their names) are excempt from this extra check.
		if (searchName.contains("ّ") && !searchName.contains("#"))
		{
			String miniTrimmed = searchName.replace("ّ", "");
			
			if (isInMemory(miniTrimmed)) 
			{
				return (Node)_nodes.get(miniTrimmed.toLowerCase());
			}
		}
		
		// overriding trimming criteria
		if (_loadingMode == KnowledgebaseLoadMode.IMPORT)
		{
			trimName = false;
		}
		
		int id = getNextFreeNodeId(searchName);

		Node node = new Node(name, trimName, id);
		
		if (source != SourceType.UNKNOWN)
			node.setSourceType(source);
		
		if (_operationMode == KbOperationMode.DYNAMIC)
		{
			node.setDynamic();
		}
		
		_nodes.put(node.getName().toLowerCase(), node);
		_nodeIds.put(node.getId(), node);
				
		return node;
	}
	
	/**
	 * Finds a node in the KB by it's name
	 * 
	 * @param name The node's name to be searched
	 * @return A reference to the found node or null 
	 */
	public Node findConcept(String name)
	{
		if (name == null)
		{
			return null;
		}
		
		Node node = (Node)_nodes.get(name.toLowerCase());
		
		if (node != null)
		{
			return node;
		}
		
		name = Common.normalizeNotTokenized(name);
		
		return (Node)_nodes.get(name.toLowerCase());
	}
	
	/**
	 * Finds a concept by its unique id
	 * @param id node's id
	 * @return found concept or null
	 */
	public Node findConceptById(int id)
	{
		return (Node)_nodeIds.get(id);
	}
	

	public PlausibleStatement addRelationReciprocal(Node argument, Node referent, Node relation, SourceType source)
	{
		return addRelation(argument, referent, relation, new CertaintyParameters(), true, ConditionalType.NOT_CONDITIONAL, source);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation)
	{
		CertaintyParameters cp = new CertaintyParameters();
		
		return addRelation(argument, referent, relation, cp, false, ConditionalType.NOT_CONDITIONAL, SourceType.UNKNOWN);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, SourceType source)
	{
		CertaintyParameters cp = new CertaintyParameters();
		
		return addRelation(argument, referent, relation, cp, false, ConditionalType.NOT_CONDITIONAL, source);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, CertaintyParameters parameters)
	{
		return addRelation(argument, referent, relation, parameters, false, ConditionalType.NOT_CONDITIONAL, SourceType.UNKNOWN);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, CertaintyParameters parameters, SourceType source)
	{
		return addRelation(argument, referent, relation, parameters, false, ConditionalType.NOT_CONDITIONAL, source);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, CertaintyParameters parameters, boolean isBidirectional)
	{
		return addRelation(argument, referent, relation, parameters, isBidirectional, ConditionalType.NOT_CONDITIONAL, SourceType.UNKNOWN);
	}
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, CertaintyParameters parameters, boolean isBidirectional, SourceType source)
	{
		return addRelation(argument, referent, relation, parameters, isBidirectional, ConditionalType.NOT_CONDITIONAL, source);
	}
	/**
	 * 
	 * Adds a new relation to the KB
	 * 
	 * @param SourceType source node
	 * @param referent target node
	 * @param relation relation conceptType
	 * @param Parameters certainty parameter for this relation
	 * @param IsBidirectional is this relation two way?
	 * @param StatType statement conceptType
	 * @return the newly created statement
	 */
	public PlausibleStatement addRelation(Node argument, Node referent, Node relation, CertaintyParameters Parameters, boolean IsBidirectional, ConditionalType StatType, SourceType source)
	{
		checkLock();
		
		PlausibleStatement ps;

		if (argument == null || referent == null || relation == null)
		{
			// You shouldn't be here! 
			MyError.exit("SourceType, Destination or Relation conceptType is null!");
		}
		if (argument.isLex() || referent.isLex() || relation.isLex())
		{
			MyError.exit("SourceType, Destination or Relation conceptType can't be a Lexical node!");
		}

		// Checks if the relation already exists in the KB. 
		// If it is the case, it returns without re-adding the relation.
		ps = argument.findRelationToTarget(relation, referent);
		if (ps != null)
		{
			ps.updateStatementProperties(Parameters, StatType, source);
			
			if (IsBidirectional || relation == HPR_INVERSE || relation == HPR_SIM || relation == HPR_DIS)
			{
				PlausibleStatement inversePs = referent.findRelationToTarget(relation, argument);
				
				if (inversePs != null)
					inversePs.updateStatementProperties(Parameters, StatType, source);
			}
			
			return ps;
		}
				
		ps = new PlausibleStatement(relation, Parameters, argument, referent, StatType);
		
		_nodeIds.put(ps.getId(), ps);

		argument.addOutLink(referent, ps);
		referent.addInLink(argument, ps);
		
		// flagging gloss and example nodes
		if (relation == KnowledgeBase.HPR_GLOSS && referent.getConceptType() == ConceptType.CONCEPT_OTHER)
			referent.setConceptType(ConceptType.CONCEPT_GLOSS);
		else if (relation == KnowledgeBase.HPR_EXAMPLE && referent.getConceptType() == ConceptType.CONCEPT_OTHER)
			referent.setConceptType(ConceptType.CONCEPT_EXAMPLE);

		// Adding Des, Arg and Ref to appropriate tracking hash tables:
		if (!_descriptorTypes.containsKey(relation.getName().toLowerCase()))
		{
			_descriptorTypes.put(relation.getName().toLowerCase(), relation);
		}
		if (!_arguments.containsKey(argument.getName().toLowerCase()))
		{
			_arguments.put(argument.getName().toLowerCase(), argument);
		}
		if (!_referents.containsKey(referent.getName().toLowerCase()))
		{
			_referents.put(referent.getName().toLowerCase(), referent);
		}
		
		if (!isInMemory(ps.getName()))
		{
			if (_operationMode == KbOperationMode.DYNAMIC)
			{
				ps.setDynamic();
			}
			
			_nodes.put(ps.getName().toLowerCase(), ps);
		}		

		// Is it bidirectional?
		if (IsBidirectional || relation == HPR_INVERSE || relation == HPR_SIM || relation == HPR_DIS)
		{
			addRelation(referent, argument, relation, Parameters, false, StatType, source);
		}
		
		ps.setSourceType(source);

		return ps;
	}
	
	/**
	 * Add contextual info to an existing relation (statement).
	 * It is actually redefinition of <code>AddRelation</code>.
	 * 
	 * @param PS plausible relation
	 * @param Target target
	 * @param CXRelation context conceptType
	 * @param CP certainty parameters
	 */
	public void addContext(PlausibleStatement PS, Node Target, Node CXRelation, CertaintyParameters CP)
	{
		addContext(PS, Target, CXRelation, CP, false);
	}
	public void addContext(PlausibleStatement PS, Node Target, Node CXRelation, CertaintyParameters CP, boolean IsTwoWay)
	{
		addRelation(PS, Target, CXRelation, CP);
	}

	/**
	 * Adds a plausible implication (IMP) to the KB.
	 * @param Triple1 a triple set containing the first relation
	 * @param Triple2 a triple set containing the second relation
	 * @param IMPParameters certainty parameters for IMP relation
	 * @return the newly created IMP relation
	 */
	public PlausibleStatement addImplication(String[] Triple1, String[] Triple2, CertaintyParameters IMPParameters)
	{
		CertaintyParameters CP = new CertaintyParameters();

		Node Antecedent = addConceptRelationConcept(Triple1[0], Triple1[1], Triple1[2], CP, false, ConditionalType.ANTECEDENT);
		Node Consequent = addConceptRelationConcept(Triple2[0], Triple2[1], Triple2[2], CP, false, ConditionalType.CONSEQUENT);

		return addRelation(Antecedent, Consequent, HPR_IMP, IMPParameters);
	}

	/**
	 * Checks if a concept exists in the KB.
	 * @param ConceptName name to be searched for
	 * @return true if the node was found; false otherwise
	 */
	private boolean isInMemory(String ConceptName)
	{
		if (_nodes.containsKey(ConceptName.toLowerCase()))
		{
			return true;
		}

		return false;
	}
	
	/**
	 * This methods is called when the name of a node is changed to adjusts dependent hashes accordingly.
	 * 
	 * @param node new node with updated name
	 * @param OldName old name of the node
	 * @param trimName Specifies whether the node's name must be trimmed before adding the name 
	 */
	public void changeConceptName(Node node, String newName, boolean trimName)
	{
		checkLock();
		
		if (findConcept(newName) != null)
		{
			MyError.exit("Chaning the concept name from `" + node.getName() + "` to `" + newName + "` failed! Duplicate name!");
		}
		
		String OldName = node.getName();
		
		// overriding trimming criteria
		if (_loadingMode == KnowledgebaseLoadMode.IMPORT)
		{
			trimName = false;
		}
		
		node.changeName(newName, trimName);
		
		if (isInMemory(OldName))
		{
			_nodes.remove(OldName.toLowerCase());
			_nodes.put(node.getName().toLowerCase(), node);
		}
		else
		{
			MyError.exit("The requested node doesn't exist in the KB.");
		}
		
		if (_descriptorTypes.containsKey(OldName.toLowerCase()))
		{
			_descriptorTypes.remove(OldName.toLowerCase());
			_descriptorTypes.put(node.getName().toLowerCase(), node);
		}
		if (_arguments.containsKey(OldName.toLowerCase()))
		{
			_arguments.remove(OldName.toLowerCase());
			_arguments.put(node.getName().toLowerCase(), node);
		}
		if (_referents.containsKey(OldName.toLowerCase()))
		{
			_referents.remove(OldName.toLowerCase());
			_referents.put(node.getName().toLowerCase(), node);
		}
	}
	
	/**
	 * Provides all nodes in the KB as an ArrayList
	 * @return all nodes in an ArrayList
	 */
	public ArrayList<Node> getNodes()
	{
		ArrayList<Node> Nodes = new ArrayList<Node>(_nodes.size());
	
		Collection<Node> allNodes = _nodes.values();
		for (Node node: allNodes)
		{
			Nodes.add(node);
		}

		return Nodes;
	}
	
	/**
	 * Provides an iterator for the nodes in the KB
	 * @return An iterator for nodes
	 */
	public Set<Entry<String, Node>> getNodesSetIterator()
	{
		return _nodes.entrySet();
	}

	/**
	 * Provides the number of nodes in the KB
	 * @return The number of nodes in the KB
	 */
	public int getNodesNum()
	{
		return _nodes.size();
	}
	 
	/**
	 * Finds the common parent of two nodes
	 * @param Concept1 node 1
	 * @param Concept2 node 2
	 * @param MaxSearchDepth the maximum depth that will be tried
	 * @return The common parents of two nodes
	 */
	public Node findCommonParent(Node Concept1, Node Concept2, int MaxSearchDepth)
	{
		ArrayList<Node> Concept1Parents = Concept1.findAllParents(MaxSearchDepth);

		Hashtable<String, Node> Concept1ParentsHash = new Hashtable<String, Node>(Concept1Parents.size());

		for (Node Parent: Concept1Parents)
		{
			if (!Concept1ParentsHash.containsKey(Parent.getName().toLowerCase()))
			{
				Concept1ParentsHash.put(Parent.getName().toLowerCase(), Parent);
			}
		}

		ArrayList<Node> Concept2Parents = new ArrayList<Node>();
		Concept2Parents.add(Concept2);

		ArrayList<Node> TempParents = new ArrayList<Node>();
		ArrayList<Node> Parents = new ArrayList<Node>();

		while (Concept2Parents != null && Concept2Parents.size() != 0)
		{
			for (Node Parent: Concept2Parents)
			{
				if (Concept1ParentsHash.containsKey(Parent.getName().toLowerCase()))
				{
					return Parent;
				}
			}

			// We didn't find the a common parent. Now extracting parents of current parents.

			Parents.clear();

			for (Node Parent: Concept2Parents)
			{
				TempParents = PlausibleAnswer.ConvertPlausibleAnswersToNodes(Parent.findTargetNodes(HPR_ISA));

				if (TempParents != null)
				{
					Parents.addAll(TempParents);
				}
			}
			
			Concept2Parents.clear();
			Concept2Parents.addAll(Parents);
		}

		return null;
	}

	private void printToBuffer(StringBuilder buffer)
	{
		buffer.append("\r\n");
	}
	/**
	 * A utility function which simulates print() but into a <code>StringBuilder</code> object
	 * @param buffer buffer
	 * @param msg text to be appended to the buffer
	 */
	private void printToBuffer(StringBuilder buffer, String msg)
	{
		buffer.append(msg + "\r\n");
	}
	
	public String getConceptDump(String conceptName, int iMaxRelations)
	{
		return getConceptDump(findConcept(conceptName), iMaxRelations);
	}	
	/**
	 * gets as a text string all the information related to concept name.
	 * @param conceptName 
	 * @return a text representation of info related to the lemma
	 */
	public String getConceptDump(Node node, int iMaxRelations)
	{
		if (node == null)
			return "";
		
		StringBuilder buffer = new StringBuilder();
		
		String conceptName = node.getName();
		Integer counter;

		//int Depth = node.computeDepthInISAHierarchy();
		printToBuffer(buffer, node.getNodeData());
	
		printToBuffer(buffer, "---------- روابط ----------");
		printToBuffer(buffer);
		
		//TODO: instead of composing the relation name (prone to error) provide an iterator here
		String statName = "*" + conceptName + " (";

		counter = 1;
		
		PlausibleStatement statement = (PlausibleStatement)findConcept(statName + counter.toString() + ")");

		while (statement != null && counter < iMaxRelations)
		{	
			String out = counter + ". " + statement.argument.getName() + "\t" + Common.removeParenthesis(statement.getName()) + "\t" + statement.referent.getName();
			
			if (statement.conditionalType != ConditionalType.NOT_CONDITIONAL)
				out += "\t(" + statement.conditionalType.toString() + ")";
	
			printToBuffer(buffer, out);
			
			counter++;
			statement = (PlausibleStatement)findConcept(statName + counter.toString() + ")");
		}
		
		if (counter == iMaxRelations)
			printToBuffer(buffer, "فقط " + iMaxRelations + " رابطه اول از کل " + node.getInstancesNum() + " روابط  نشان داده شده اند.");
		

		printToBuffer(buffer);
		printToBuffer(buffer, "---------- دانش لغوی ----------");
		printToBuffer(buffer);

		String[] POS = {"n", "v", "a", "s", "r"};
		String[] POSVerbose = {"Noun", "Verb", "Adjective", "Adjective Satellite", "Adverb"};
	
		String SenseName;
		Node SynsetNode;
		Node GlossNode;
		Node ExampleNode;
		String SynsetNodeName;
		String GlossNodeName;
		String ExampleNodeName;

		ArrayList<?> SYNs;
		ArrayList<?> Glosses;
		ArrayList<?> Examples;

		Node LexNode;

		for (int i = 0; i < POS.length; i++)
		{
			printToBuffer(buffer, POSVerbose[i] + ":");

			SenseName = conceptName + "#" + POS[i];
			counter = 1;
			
			LexNode = (Node)findConcept(SenseName + counter.toString());

			while (LexNode != null)
			{	
				SYNs = LexNode.findTargetNodes(HPR_SYN);
				if (SYNs.size() == 0)
				{
					SynsetNodeName = "Unavailable!";
					GlossNodeName = "Unavailable!";
					ExampleNodeName = "";
				}
				else
				{
					SynsetNode = ((PlausibleAnswer)SYNs.get(0)).answer;
					SynsetNodeName = SynsetNode.getName();

					Glosses = SynsetNode.findTargetNodes(HPR_GLOSS);
					if (Glosses.size() == 0)
					{
						GlossNodeName = "Unavailable!";
					}
					else
					{
						GlossNode = ((PlausibleAnswer)Glosses.get(0)).answer;
						GlossNodeName = GlossNode.getName();
					}
					
					Examples = SynsetNode.findTargetNodes(HPR_EXAMPLE);
					if (Examples.size() == 0)
					{
						ExampleNodeName = "";
					}
					else
					{
						ExampleNode = ((PlausibleAnswer)Examples.get(0)).answer;
						ExampleNodeName = ExampleNode.getName();
					}
				}

				printToBuffer(buffer, LexNode.getName() +"\t(" + LexNode.WNTaggedCount + ")\tSYN\t" + SynsetNodeName + "\t" + GlossNodeName + " : " + ExampleNodeName);
				
				counter++;
				LexNode = (Node)findConcept(SenseName + counter.toString());
			}

			printToBuffer(buffer);
		}

		return buffer.toString();
	}
	
	/**
	 * Exports the KB to the default filename
	 * @return Number of relations written to the default file
	 */
	public int exportKb()
	{
		String fileName = "hpr-kb-dump.txt";
		
		return exportKb(fileName);
	}
	
	/**
	 * Exports all relations in the KB to a file
	 * 
	 * @param fileName The destination file for export
	 * @return the number of relations written
	 */
	public int exportKb(String fileName)
	{	
		BufferedWriter stream = null;
		
		try
		{
			stream 	= new BufferedWriter(new FileWriter(fileName));
	
			// writing header
			writeExportHeader(stream);
		}
		catch(Exception e)
		{
			MyError.exit("Error opening `" + fileName + "` for writing!\r\n" + e.getMessage());
		}
		
		// sorting _kb nodes based on their names
		Vector<String> tempNodeNames = new Vector<String>(_nodes.keySet());
	    Collections.sort(tempNodeNames);
		
		int exported = 0;
		int processed = 0;
		int counter = 0;
		int skippedRelations = 0;

		Node node;
		String key;

		String name;
		Enumeration<String> enumerationNodeNames = tempNodeNames.elements();
		
		while (enumerationNodeNames.hasMoreElements()) 
		{		
			counter++;
			
			key = (String)enumerationNodeNames.nextElement();
			node = _nodes.get(key);
			
			if (node.getName().charAt(0) == '*')
			{
				// just stand-alone relations not antecedents of IF-THEN relations
				skippedRelations++;
				continue;
			}
			
			try
			{
				exported = node.exportNode(stream, exported);
			}
			catch (Exception e)
			{
				MyError.exit("Error writing to `" + fileName + "`!" + "\r\n" + e.getMessage());
			}			
		}
		
		try
		{
			stream.close();
		}
		catch (Exception e)
		{
			MyError.exit("Error closing `" + fileName + "`!");
		}
		
		return exported;
	}
	
	/**
	 * Writes header lines while exporting the KB.
	 * Headers lines provide help and copyright notice.
	 * 
	 * @param stream The header lines are written to this stream.
	 * @throws IOException 
	 */
	private void writeExportHeader(BufferedWriter stream) throws IOException
	{
		stream.write("# Human Plausible Reasoning knowledge base export file\r\n");		
		stream.write("# Knowledge base is composed of triples separated with a TAB, then certainty parameters follow.\r\n");
		stream.write("# Relation for relations start with a star (*). They come immediately after the original relation.\r\n");

		stream.write("# Concept names (constituents of triples) are unique. The may start with a letter + unicode character \\02F8 '˸'; please notice though very similar to a normal colon sign but it is not!)\r\n");		
		stream.write("# The letter determines the source of the concept (if any).\r\n");
		stream.write("# For Quranic question answering system (ITRC) these sources are supported:\r\n");
		stream.write("# \tf˸ Farsnet\r\n");
		stream.write("# \tw˸ Wordnet\r\n");
		stream.write("# \tq˸ Quranic Conceptual Graph (created at Iran Telecom Research Center)\r\n");
		stream.write("# \te˸ Islamic Thesaurus or `Estelahname Oloom-e Quorani` (www.islamicdoc.ir)\r\n");
		//stream.write("# \th˸ Quranic Farhang or `Farhang-e Quran` (www.maarefquran.com)\r\n");
		//stream.write("# \tt˸ Tebyan Categorized Quranic Encyclopedia (tebyan.ir)`\r\n");
		//stream.write("# \tn˸ Nemoone Commentaries or `Tafsir-e Nemoone`\r\n");
		
		stream.write("# All comment lines in this file start with a nubmber sign '#'\r\n");		
		stream.write("# ------------------------------------------------------------------------------------------\r\n");
	}
	
	/**
	 * Produces some statistical info about KB.
	 * 
	 * @return a string containing the stats
	 */
	public String getStatistics()
	{
		Integer relations = 0;
		
		Set<Entry<String, Node>> set = _nodes.entrySet();
		Iterator<Entry<String, Node>> iterator = set.iterator();
		
		Entry<String, Node> entry;
		String name;
		
		while (iterator.hasNext()) 
		{                        
			entry = (Entry<String, Node>)iterator.next();
			
			name = entry.getKey();
			
			if (name.startsWith("*"))
				relations++;
		}
		
		StringBuilder buffer = new StringBuilder();

		printToBuffer(buffer, "تعداد کل مفاهیم: " + _nodes.size());
		printToBuffer(buffer, "نوع رابطه ها: " + _descriptorTypes.size());
		printToBuffer(buffer, "تعداد رابطه ها: " + relations);
		printToBuffer(buffer, "تعداد مفاهیم مبداء یکتا: " + _arguments.size());
		printToBuffer(buffer, "تعداد مفاهیم مقصد یکتا: " + _referents.size());
		
		return buffer.toString();
	}

	/**
	 * Sets the KB's operation mode (default = STATIC)
	 * When the KB is set to DYNAMIC mode all new nodes and relations are marked as `dynamic`. These info will be destroyed at the next memory purge (see <code>PurgeDynamicKnowledge</code>).
	 * Static nodes however are permanent and stand memory purges. 
	 *  
	 * @param newOperationMode the operation mode to be set.
	 */
	public void setOperationMode(KbOperationMode newOperationMode)
	{
		_operationMode = newOperationMode;
	}
	
	/**
	 * Removes all DYNAMIC data from KB. See <code>SetOperationMode</code> for help. 
	 * @return The number of nodes removed
	 */
	public int purgeDynamicKnowledge()
	{		
		checkLock();
		
		int deleted = 0;
		
		Node current;
		String nodeName;
		PlausibleStatement ps;
		
		Enumeration<Node> nodesEnum = _nodes.elements();
		while (nodesEnum.hasMoreElements())
		{
			current = nodesEnum.nextElement();
			nodeName = current.getName().toLowerCase();
			
			if (current.isDynamic())
			{
				_nodes.remove(nodeName);
				
				if (nodeName.startsWith("*"))
				{					
					ps = (PlausibleStatement)current;
					
					if (ps.relationType.removeInstance() == 0)
					{
						// the node we're going to remove is a relation and it is the last instance of its relation conceptType. 
						// so we remove the relation conceptType from the DescriptorTypeHash too:
						_descriptorTypes.remove(ps.relationType.getName().toLowerCase().toString());
					}
					
					ps.unbindRelations();
				}
				
				deleted++;
			}
		}
			
		// clearing hashes	
		_descriptorTypes.clear();
		_arguments.clear();
		_referents.clear();
		
		// reconstructing hashes
		nodesEnum = _nodes.elements();
		while (nodesEnum.hasMoreElements())
		{
			current = nodesEnum.nextElement();
			nodeName = current.getName().toLowerCase();
				
			if (nodeName.startsWith("*")) // it is a relation
			{					
				ps = (PlausibleStatement)current;

				if (!_descriptorTypes.containsKey(ps.relationType.getName().toString()));
				{
					_descriptorTypes.put(ps.relationType.getName().toString(), current);
				}
				
				if (!_arguments.containsKey(ps.argument.getName().toString()))
				{
					_arguments.put(ps.argument.getName().toString(), ps.argument);
				}
				
				if (!_referents.containsKey(ps.referent.getName().toString()))
				{
					_referents.put(ps.referent.getName().toString(), ps.referent);
				}
			}
		}
		
		return deleted;
	}
	
	/**
	 * Deletes a concept and all its connections (relations) to other nodes from the KB
	 * @param node The node to be removed
	 * @return The number of relations removed along with the node.
	 */
	public int deleteConcept(Node node)
	{
		checkLock();
		
		MyError.assertNotNull(node);
		
		int deletedRelations = node.unbindRelations();
		
		if (node.getName().startsWith("*"))
		{
			if (node.removeInstance() == 0)
			{
				PlausibleStatement ps = (PlausibleStatement)node;
				
				_descriptorTypes.remove(ps.relationType.getName().toLowerCase());
			}
		}
		
		_arguments.remove(node.getName().toLowerCase());
		_referents.remove(node.getName().toLowerCase());		
		_nodes.remove(node.getName().toLowerCase());
		
		return deletedRelations;
	}
	
	/**
	 * Loads a KB without its preprocessed concept names
	 * @param filename path to the kb
	 * @return number of relations loaded
	 */
	public int importSimpleKb(String filename)
	{
		checkLock();
		
		return importKb(filename);
	}
	
	public int importKb(InputStream stream, String filename)
	{
		checkLock();
		
		BufferedReader myStream = null;
		
		try
		{
			myStream = new BufferedReader(new InputStreamReader(stream, "utf-8"));
		}
		catch(Exception e)
		{
			MyError.exit("Error opening `" + filename + "` for reading!");
		}
		
		return importKb(myStream, filename, null);
	}
	public int importKb(String filename)
	{
		return importKb(filename, null);
	}
	public int importKb(String filename, HashSet<Node> excudedRelations)
	{
		BufferedReader stream = null;
		
		try
		{
			stream = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"));
		}
		catch(Exception e)
		{
			MyError.exit("Error opening `" + filename + "` for reading!");
		}
		
		return  importKb(stream, filename, excudedRelations);
	}
	/**
	 * Imports a KB into memory.
	 * 
	 * @param stream The stream to read data from.
	 * @param filename The KB's filename
	 * @param excludRelations these relations are excluded on import
	 * @return The number of relation read from the stream
	 */
	public int importKb(BufferedReader stream, String filename, HashSet<Node> excludRelations)
	{
		checkLock();
		
		Common.log("loading knowledgebase dump '" + filename + "' ... ");
		
		int StatementNumber = 0;

		String Line = "";
		String Delimiter = "\t";
		String Delimiters2 = "\\*|\\(|\\)";
		String[] Split;		
		String source;
		String target;
		String relation;
		String Parameters = "";
		
		CertaintyParameters CP;
		PlausibleStatement ps;

		CertaintyParameters EqualSIMCP = new CertaintyParameters();
		EqualSIMCP.similarity = 0.99F;
		
		int relations_loaded = 0;
		int relationsReflexiveIgnored = 0;
		int relationsExcludedIgnored = 0;
		int relationsBidirectionalIgnored = 0;

		int ReferenceStatement;

		Hashtable<Integer, PlausibleStatement> statements = new Hashtable<Integer, PlausibleStatement>();
		HashSet<String> addedStatements = new HashSet<String>();
		
		_loadingMode = KnowledgebaseLoadMode.IMPORT;
		
		while (Line != null)
		{
			try
			{
				Line = stream.readLine();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			if (Line == null)
				break;
			
			if (Line.startsWith("#")) // comment lines
				continue;

			Split = Line.split(Delimiter, 2); 
				
			StatementNumber = Integer.parseInt(Split[0]);
			Split = Split[1].split(Delimiter);

			if (Split.length != 3 && Split.length != 4)
			{
				MyError.exit("Bad Line in the KB! (KnowledgeBase.LoadQuickKB)");
			}

			Parameters = "";

			source 		= Node.prepareConceptNameForImport(Split[0]);
			relation 	= Node.prepareConceptNameForImport(Split[1]);
			target 		= Node.prepareConceptNameForImport(Split[2]);
			
			String prospectiveName = Node.extractProspectiveName(relation);
			
			if (excludRelations != null && excludRelations.contains(findConcept(prospectiveName)))
			{
				relationsExcludedIgnored++;
				continue;
			}
			
			if (Split.length == 4)
			{
				Parameters = Split[3];
			}

			CP = new CertaintyParameters(Parameters);

			if (relation == "IMP" && source.indexOf("=") != -1 && target.indexOf("=") != -1)
			{
				ps = addImplicationFromSplit(Split, CP);
				
				relations_loaded++;
			}
			else
			{
				if (source.startsWith("*"))
				{
					// it is a relation about another relation, e.g. CX

					Split = source.split(Delimiters2);
					
					source = Split[2];
					ReferenceStatement = Integer.parseInt(source);

					ps = (PlausibleStatement)statements.get(ReferenceStatement);

					//MyError.assertNotNull(ps);
					if (ps == null) // most probably is a CX for an ignored reflexive relation
						continue;

					ps = addRelation(ps, addConcept(target), addConcept(relation), CP);
					
					relations_loaded++;
				}
				else
				{					
					if (source.equals(target) && !relation.equals(KnowledgeBase.HPR_VERSE_ARABIC.getName())) 
					{
						// a reflexive relation
						
						//Common.log("\tignored reflexive: " + source + " -- " + relation + " --> " + target);
						
						relationsReflexiveIgnored++;
						continue;
					}
					
					if (_ignoreBidirectionalRelationsOnImport && addedStatements.contains(target + "--" + relation + "->" + source))
					{
						relationsBidirectionalIgnored++;
						continue;
					}
					
					ps = addConceptRelationConcept(source, relation, target, CP);					
					
					relations_loaded++;
					
					addedStatements.add(source + "--" + relation + "->" + target);

					// TODO: Special relations (reflective, symmetric, reverse) come here
				}
			}
			
			statements.put(StatementNumber, ps);
		}
		
		statements.clear();

		try
		{
			stream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		addLoadedKb("Memory Dump: " + filename);
		
		_loadingMode = KnowledgebaseLoadMode.NORMAL;
		
		Common.log("loaded relations: " + relations_loaded + 
					", reflexive ignored: " + relationsReflexiveIgnored + 
					", excluded ignored: " + relationsExcludedIgnored + 
					", bi-directional ignored: " + relationsBidirectionalIgnored);
		
		return relations_loaded;
	}
	

	/**
	 * Adds and implication (IMP) relation from a already split string array
	 * @param split The array containing constituents
	 * @param cp The certainty parameters for the implication
	 * @return The created implication (statement)
	 */
	private PlausibleStatement addImplicationFromSplit(String[] split, CertaintyParameters cp)
	{
		String[] Antecedent = splitStatement(split[0]);
		String[] Consequent = splitStatement(split[2]);

		return addImplication(Antecedent, Consequent, cp);
	}
	
	/**
	 * Breaks down a text representation of a statement to its constituents 
	 * @param snippet The statement in string format
	 * @return An array containing the statement's constituents
	 */
	private String[] splitStatement(String snippet)
	{
		String Delimiter1 = "(";
		String Delimiter2 = ")";

		String[] Split;
		String[] SplittedStatemnet = new String[3];

		Split = snippet.split(Delimiter1, 2);
		SplittedStatemnet[1] = Split[0];

		Split = Split[1].split(Delimiter2, 2);
		SplittedStatemnet[0] = Split[0];

		SplittedStatemnet[2] = Split[1].replace("={", "").replace("}", "");

		return SplittedStatemnet;
	}
	
	PlausibleStatement addConceptRelationConcept(String SourceName, String RelationName, String DestinationName, CertaintyParameters Parameters)
	{
		return addConceptRelationConcept(SourceName, RelationName, DestinationName, Parameters, false, ConditionalType.NOT_CONDITIONAL);
	}
	PlausibleStatement addConceptRelationConcept(String SourceName, String RelationName, String DestinationName, CertaintyParameters Parameters, boolean Bidirectional)
	{
		return addConceptRelationConcept(SourceName, RelationName, DestinationName, Parameters, Bidirectional, ConditionalType.NOT_CONDITIONAL);
	}
	/**
	 * Adds a relation to the KB.
	 * @param SourceName The source node
	 * @param RelationName The relation node
	 * @param DestinationName The target node
	 * @param Parameters the certainty parameters
	 * @param Bidirectional the uni- or bi-directionality of the relation
	 * @param StatType The statement conceptType
	 * @return
	 */
	PlausibleStatement addConceptRelationConcept(String SourceName, String RelationName, String DestinationName, CertaintyParameters Parameters, boolean Bidirectional, ConditionalType StatType)
	{	
		Node Source 		= addConcept(SourceName);
		Node Destination 	= addConcept(DestinationName);
		Node RelationType 	= addConcept(RelationName);
		
		SourceType relationSource = Node.extractSource(RelationName);

		return addRelation(Source, Destination, RelationType, Parameters, Bidirectional, StatType, relationSource);
	}
	
	/**
	 * Tries to find the next available sense number of a word in the KB
	 * @param headWord The word to be searched
	 * @param pos The POS tag of the word
	 * @return The sense number available (free to use)
	 */
	public int getNextSenseNumber(String headWord, POS pos)
	{
		String tentativeName;
		int i = 0;
		
		Node tentativeConcept = null;
		
		do
		{
			i++;

			tentativeName = Node.trimName(headWord) + "#" + Common.convertPosToSingleCharString(pos) + i;

			tentativeConcept = findConcept(tentativeName);
		
		} while (tentativeConcept != null);
		
		return i;
	}
	
	/**
	 * Produces a shallow copy of a concept and name it.
	 * 
	 * @param newName The new name to be assigned to the new node
	 * @param model The model to copy from
	 * @param trimName Should we trim the name of the new node?
	 * @return The new copy of the node with a brand new name
	 */
	public Node copyConceptShallow(String newName, Node model, boolean trimName)
	{
		checkLock();
		
		Node newNode = addConcept(newName, trimName);
		
		model.copyShallowTo(newNode);
		
		return newNode;
	}
	
	/**
	 * Finds all concepts that their name matches a string (word).
	 *  
	 * @param snippet The word to search for
	 * @param pos The POS tag of the nodes to be considered in the search
	 * @param matchType The matching algorithm. See <code>StringMatch</code> for a enumeration of matching algorithms supported.
	 * @param lexicalType Specifies the node categories to be searched.
	 * @return An array list containing found concepts
	 */
	public Vector<Node> getConceptFromSubstr(String snippet, POS pos, StringMatch matchType, LexicalType category)
	{		
		snippet = snippet.toLowerCase();
		
		Hashtable<String, Node> answers = new Hashtable<String, Node>();
		
		Set<Entry<String, Node>> set = _nodes.entrySet();
		Iterator<Entry<String, Node>> iterator = set.iterator();
		
		Entry<String, Node> entry;
		String name;
		Node node;
		String word;
		
		while (iterator.hasNext()) 
		{                        
			entry = (Entry<String, Node>)iterator.next();
			
			name = entry.getKey();
			node = entry.getValue();
			
			if (node.getConceptType() == ConceptType.STATEMENT || node.getConceptType() == ConceptType.CONCEPT_GLOSS || node.getConceptType() == ConceptType.CONCEPT_EXAMPLE)
			{
				continue;
			}
			
			if (node.getLexicalType() == LexicalType.SYNSET && matchType != StringMatch.EXACT && matchType != StringMatch.SUBNUMBER)
			{
				continue;
			}
			
			if (category != LexicalType.ANY && node.getLexicalType() != category)
			{
				continue;
			}			
			
			if (pos != POS.ANY && pos != node.getPos())
			{
				continue;
			}
			
			if (category == LexicalType.SENSE && !name.contains("#"))
			{
				continue;
			}
			
			
			if (name.length() < snippet.length())
			{
				continue;
			}

			word = name;			
			if (word.contains("#"))
				word = word.substring(0, word.indexOf('#'));

			
			if (	(matchType == StringMatch.SUBSTRING && name.contains(snippet)) || 
					(matchType == StringMatch.PREFIX && name.startsWith(snippet)) ||
					(matchType == StringMatch.SUBNUMBER && name.contains(snippet)) ||
					(matchType == StringMatch.EXACT && (word.equals(snippet) || name.equals(snippet))) ||
					matchType == StringMatch.WHOLE_WORD && (name.matches("\\b" + snippet + "\\b")))
			{
				if (node.getLexicalType() == LexicalType.SENSE)
				{				
					ArrayList<PlausibleAnswer> ans = node.findTargetNodes(KnowledgeBase.HPR_SYN);
					
					PlausibleAnswer pa = ans.get(0);
					
					if (answers.get(pa.answer.getName()) == null)				
						answers.put(pa.answer.getName(), node);
				}
				else
				{
					if (answers.get(node.getName()) == null)				
						answers.put(node.getName(), node);
				}
			}
		}
		
		// sorting
		Vector<Node> tempNodeNames = new Vector<Node>(answers.values());
	    Collections.sort(tempNodeNames);
		
		return tempNodeNames;
	}
	
	/**
	 * Tries to find concepts having a substring in a relaxed way (ie preprocessed versions are also checked).
	 * @param snippet substring
	 * @param pos the required part-of-speech 
	 * @param matchType matching criteria
	 * @param category required concept type
	 * @param relaxationType the rexation method
	 * @return
	 */
	public Vector<Node> getConceptFromSubstrRelaxed(String snippet, POS pos, StringMatch matchType, LexicalType category, PreprocessorType relaxationType)
	{		
		snippet = snippet.toLowerCase();
		
		Hashtable<String, Node> answers = new Hashtable<String, Node>();
		
		Multimap<String, Node> nodesHash = null;
		
		switch (relaxationType)
		{
			case TOKENIZATION: nodesHash = _nodesReversedTokenized; break;
			case LEMMATIZATION: nodesHash = _nodesReversedLemmatized; break;
			default: MyError.exit("Unsupported relaxation method!");
		}		
		
		for (Map.Entry<String,Collection<Node>> entry : nodesHash.asMap().entrySet())
		{                        
			String name = entry.getKey();
			
			if ((matchType == StringMatch.SUBSTRING && name.contains(snippet)) || 
				(matchType == StringMatch.PREFIX && name.startsWith(snippet)) ||
				(matchType == StringMatch.EXACT && name.equals(snippet)) ||
				matchType == StringMatch.WHOLE_WORD && (name.matches(".*\\b" + snippet + "\\b*.")))
			{
				Collection<Node> nodes = entry.getValue();
				
				for (Node node: nodes)
				{
					if (category != LexicalType.ANY && node.getLexicalType() != category)
					{
						continue;
					}
					
					if (pos != POS.ANY && pos != node.getPos())
					{
						continue;
					}
					
					if (category == LexicalType.SENSE && !name.contains("#"))
					{
						continue;
					}
					
					answers.put(node.getName(), node);
				}
			}			
		}
		
		// sorting
		Vector<Node> tempNodeNames = new Vector<Node>(answers.values());
	    Collections.sort(tempNodeNames);
		
		return tempNodeNames;
	}
	
	/**
	 * Provides the descriptor (relation) types as a string
	 * @return The descriptor (relation) types as a string
	 */
	public String getDescriptorsAsText()
	{
		StringBuilder buffer = new StringBuilder();
				
		int Counter = 0;
		
		Collection<Node> descs = _descriptorTypes.values();
		for (Node desc: descs)
		{
			Counter++;

			printToBuffer(buffer, Counter + ".\t" + desc.getName() + "\t" + desc.getInstancesNum());
		}
		
		return buffer.toString();
	}
	
	
	/**
	 * Returns descriptor type existing in the kb.
	 * @return descriptor types
	 */
	public Collection<Node> getDescriptors()
	{
		return _descriptorTypes.values();
	}

	/**
	 * Loads a comma separated file as source-elation-target into memory
	 * @param path path to the file
	 * @return number of relations loaded
	 */
	public int loadCsv(String path)
	{
		checkLock();
		
		int loaded = 0;
		
		BufferedReader stream = null;
		
		try
		{
			stream = new BufferedReader(new InputStreamReader(new	FileInputStream(path), "CP1256"));
		}
		catch(Exception e)
		{
			MyError.exit("Error opening `" + path + "` for reading!");
		}
		
		String line = "";
		
		while (line != null)
		{
			try
			{
				line = stream.readLine();
			}
			catch (Exception e)
			{
				MyError.exit("Error reading from input file!");
			}
			
			if (line == null)
				break;
			
			String[] parts = line.split(",");
			
			if (parts.length < 3)
			{
				MyError.exit("Number of columns is less than 3!");
			}
			
			addConceptRelationConcept(parts[0], parts[1], parts[2], new CertaintyParameters());
			
			loaded++;			
		}
		
		try 
		{
			stream.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return loaded;
	}
	
	/**
	 * Tries to enumerate the instances of a relation type, e.g. for ISA relation type all instances of ISA instances (*ISA(1), *ISA(2), ... ) are returned. 
	 * @param relation relation type
	 * @return instances of the relation type
	 */
	public ArrayList<PlausibleStatement> findRelationInstances(Node relation)
	{
		ArrayList<PlausibleStatement> out = new ArrayList<PlausibleStatement>();
		
		for (int i = 1; i <= relation.getInstancesNum(); i++)
		{
			String label = "*" + relation.getName() + " (" + i + ")";
			
			Node instance = findConcept(label);
			
			out.add((PlausibleStatement)instance);
		}
		
		return out;
	}
	

	
	/**
	 * Calculates the next free ID to be assigned to the new concept
	 * @param name concept name
	 * @return new ID generated ot reused from the permanent id file
	 */
	private synchronized int getNextFreeNodeId(String name)
	{
		return ++_lastIdAssigned;
	}
	
	
	@Override
	protected void finalize() throws Throwable 
	{
		if (_permanentIdFile != null)
		{
			try
			{
				_permanentIdFile.close();
			}
			catch(IOException e)
			{
				// do nothing
			}
		}
		
		super.finalize();
	}
	
	/**
	 * Finds a string in the reversed tokenized hash table
	 * @param tokenized search name
	 * @return concepts having the 'search name' as their tokenized property  
	 */
	public Collection<Node> getReversedTokenized(String tokenized)
	{
		return _nodesReversedTokenized.get(tokenized);
	}
	
	
	/**
	 * Finds a string in the reversed lemmatized hash table
	 * @param lemmatized search name
	 * @return concepts having the 'search name' as their lemmatized property  
	 */
	public Collection<Node> getReversedLemmatized(String lemmatized)
	{
		return _nodesReversedLemmatized.get(lemmatized);
	}
	
	/**
	 * Enables source overriding, i.e, when a concept or relation is added to the kb and it already exist then its source is updated (overridden).   
	 */
	public void enableOverrideSourceTypeOnAddition()
	{
		checkLock();
		_overrideSourceTypeOnAddition = true;
	}
	
	/**
	 * Disables source overriding, i.e, when a concept or relation is added to the kb and it already exist then its source is NOT updated (overridden).    
	 */
	public void disableOverrideSourceTypeOnAddition()
	{
		checkLock();
		_overrideSourceTypeOnAddition = false;
	}
}