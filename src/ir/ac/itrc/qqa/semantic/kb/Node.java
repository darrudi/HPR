/* Node.java
 * Created on May 24, 2010
 * @author: Ehsan Darrudi
 * 
 * Summary: 
 */

package ir.ac.itrc.qqa.semantic.kb;


import ir.ac.itrc.qqa.semantic.enums.CONTEXT;
import ir.ac.itrc.qqa.semantic.enums.LexicalType;
import ir.ac.itrc.qqa.semantic.enums.ConceptType;
import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.enums.POS;
import ir.ac.itrc.qqa.semantic.enums.ConditionalType;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;
import ir.ac.itrc.qqa.semantic.util.Common;
import ir.ac.itrc.qqa.semantic.util.MyError;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import sun.net.www.content.text.plain;
import sun.security.util.Length;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * 
 * Implements Node class for representing both Concepts and Relations.
 * Please bear in mind that relations are considered concepts in this implementation of HPR.
 * 
 * @author Ehsan Darrudi
 */

public class Node implements Comparable<Node>
{
	/** the node's name, must be unique */
	private String _name;
	
	/** node's unique identifier */
	private int _id; 

	/** a referent to the link-list that keeps track of relations that have this node as their referent */
	private InLinkElement _lastInLink = null;

	/** a referent to the link-list that keeps track of relations that have this node as their argument */
	// TODO: back to private 
	private OutLinkElement _lastOutLink = null;
	
	/** specifies if this node is DYNAMIC or STATIC. DYNAMIC node will be destroyed on the next knowledge base purge */
	private boolean _dynamic = false;

	/** used to enumerate instances of this node in the _kb */
	private volatile int _numberOfInstances = 0;

	/** specifies the source (source) of this concept (Wordnet? Farsnet? Estelahname? etc.) */
	private SourceType _source = SourceType.UNKNOWN;
	
	/** defines the lexicalType of this concept. */
	private LexicalType _lexicalType = LexicalType.ANY;
		
	/** used in Farsnet while loading Farsnet */
	private POS _pos = POS.ANY;
	
	/** specifies if this node is a statement or a simple concept (example, gloss or other types) */
	private ConceptType _conceptType = ConceptType.CONCEPT_OTHER;
	
	/** it is used only to name edge labels for JUNG graph presentation */
	private static Integer _edgeLabeler = 0;
	
	/** determines whether the node is a normal concept or it is descriptive such as Gloss, Arabic or Farsi Verse, Explanations, etc. */
	private boolean _descriptive = false;
	
	/** the number of relations this node has to other nodes */
	private int _inDegree = 0;
	/** the number of relations this node receives from other nodes */
	private int _outDegree = 0;
	
	/** max out degree seen globally */
	private static volatile Integer _maxInDegree = 0;
	/** max in degree seen globally */
	private static volatile Integer _maxOutDegree = 0;
	
	/** holds the tokenized version of concept name */
	private String _tokenized = "";
	
	/** holds the lemmatized version of concept name */
	private String _lemmatized = "";
	
	/** a random generator to create random concept IDs for relations */
	private static Random _randomGenerator = new Random();
	
	/** used for profiling: holds the number of times this node's name or tokenized and lemmetized versions has been read */
	private int _accessed = 0;
	
	// temporary values used only while compiling KBs (wordnet, farsnet, ...), i.e., they are never used at runtime -->
	
	/** used in WordNet while loading WordNet */
	public String WNHeadWord = null;
	/** used in WordNet while loading WordNet */
	public int WNTaggedCount = 0;
	/** Holds the sense number during Farsnet loading */
	public int senseNo = 0;
	/** it is used for KB refinement. We initialize it with a huge number; */
	public int distance = Integer.MAX_VALUE;
	/** used in WordNet while loading WordNet */
	public int WNRank = 0;
	
	// <--

	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * Creates a node with the specified name. 
	 * @param name node's name
	 */
	public Node(String name)
	{
		// apparently the node creation is done outside the usual KB. so we create a random (unimportant) id for the node
		this(name, true, _randomGenerator.nextInt());
	}
	/**
	 * Creates a node with the specified name. Trims the name to comply with naming standard.
	 * @param name suggested name
	 * @param forceTrimName determines whether the suggested name must be trimmed or used as it is
	 */
	public Node(String name, boolean forceTrimName)
	{
		this(name, forceTrimName, _randomGenerator.nextInt());
	}
	
	/**
	 * Creates a node with the specified name. Trims the name to comply with naming standards if trimName has been set 
	 * 
	 * @param name the name of the new concept to be created
	 * @param forceTrimName specifies whether to trim the name or not. Only in special cases (knowledgebase loads and imports) the trimming is skipped.
	 */
	public Node(String name, boolean forceTrimName, int id)
	{
		if (forceTrimName)
		{
			_name = trimName(name);
		}
		else
		{
			_name = extractPropertiesFromName(name);
		}
		
		_id = id;
	}
	
	/**
	 * Creates a node based on a template (which is a node too).
	 * As an example we may have only one ISA relation in the _kb (which is the template) but many ISA relation instances may exist.
	 * All instance names begin with an * to distinguish them from normal concepts.
	 * 
	 * @param template the template for creating our instance
	 */
	public Node(Node template)
	{
		// We're going to create a node from a template. Indeed we create an instance.
		// We mark instances with '*'.  
		// This constructor is called only by PlausibleStatement (a child of this class)
		
		Integer id = template.getNextInstanceId();
		
		
		//------------- added by hashemi ------------------
		extractPropertiesFromName(template._name);
		//------------- added by hashemi ------------------

		//TODO: in Node constructor check to see if the name starts with '*' and change it if so.
		_name = "*" + template._name + " (" + id.toString() + ")";
		setConceptType(ConceptType.STATEMENT);
		
		// id is unimportant for relations (not persistent) so we create a random number
		_id =  _randomGenerator.nextInt();
		
		setConceptType(ConceptType.STATEMENT);
	}
	
	/**
	 * Getter for <code>_name</code>
	 * @return
	 */
	public String getName()
	{
		_accessed++;		
		return _name;
	}
	
	/**
	 * Getter for <code>_id</code>
	 * @return
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Getter for <code>_dynamic</code>
	 * @return
	 */
	public boolean isDynamic()
	{
		return _dynamic;
	}
	/**
	 * Setter for <code>_dynamic</code>
	 */
	public void setDynamic()
	{
		_dynamic = true;
	}
	
	/**
	 * Setter for <code>_source</code>
	 * @param source the new soruce type of the node 
	 * @return
	 */
	public Node setSourceType(SourceType source)
	{
		_source = source;		
		return this;
	}
	/**
	 * Getter for <code>_source</code>
	 * @return
	 */
	public SourceType getSourceType()
	{
		return _source;
	}
	
	/**
	 * Setter for <code>_lexicalType</code>
	 * @param lexicalType
	 */
	public void setLexicalType(LexicalType lexicalType)
	{
		_lexicalType = lexicalType;
	}
	/**
	 * Getter for <code>_lexicalType</code>
	 * @return
	 */
	public LexicalType getLexicalType()
	{
		return _lexicalType;
	}
	

	/**
	 * Extracts all available information from the concept's name and then cleans the name.
	 * This include sense & synset information plus resource identifier.
	 * 
	 * @param name The concept name
	 * @return The cleaned name appropriate to be inserted in the KB
	 */
	private String extractPropertiesFromName(String name)
	{
		if (name.indexOf('#') != -1)
		{
			this._lexicalType = LexicalType.SENSE;
			this.setPos(Common.convertSingleCharStringToPos(name));
		}
		else if (name.indexOf('§') != -1)
		{
			this._lexicalType = LexicalType.SYNSET;
			// TODO: what about pos tags? how to make them persistent along with the node info?
		}
		
		if (name.length() > 2 && name.charAt(1) == '˸')
		{
			switch (name.charAt(0))
			{
				case 'f': this._source = SourceType.FARSNET; break;
				case 'w': this._source = SourceType.WORDNET; break;
				case 'e': this._source = SourceType.ESTELAHNAME; break;
				case 'q': this._source = SourceType.QURAN; break;
				case 'h': this._source = SourceType.FARHANG_QURAN; break;
				case 'n': this._source = SourceType.TAFSIR_NEMOONE; break;
				case 't': this._source = SourceType.TEBYAN; break;
				case 's': this._source = SourceType.TTS; break;
				default	: MyError.exit("Bad resource descriptor in concept name!");
			}
			
			name = name.substring(2);
		}
		
		if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1)
		{
			name = name.substring(1, name.length() - 1);
			this.setDescriptive(true);
		}
		
		return name;
	}
	
	/**
	 * Determines what would be the concept name after trimming. it rips the name off the source info and normalizes it
	 * @param name tentative string
	 * @return would-be name
	 */
	public static String extractProspectiveName(String name)
	{
		if (name.length() > 2 && name.charAt(1) == '˸')
		{
			switch (name.charAt(0))
			{
				case 'f': break;
				case 'w': break;
				case 'e': break;
				case 'q': break;
				case 'h': break;
				case 'n': break;
				case 't': break;
				case 's': break;
				default	: MyError.exit("Bad resource descriptor in concept name!");
			}
			
			name = name.substring(2);
		}
		
		name = Common.normalizeNotTokenized(name);
		
		return name;
	}
	
	/**
	 * Determines the concept source type based on its name 
	 * @param name the concept name
	 * @return the concept's would-be source type
	 */
	public static SourceType extractSource(String name)
	{
		if (name.length() > 2 && name.charAt(1) == '˸')
		{
			switch (name.charAt(0))
			{
				case 'f': return SourceType.FARSNET;
				case 'w': return SourceType.WORDNET;
				case 'e': return SourceType.ESTELAHNAME;
				case 'q': return SourceType.QURAN;
				case 'n': return SourceType.TAFSIR_NEMOONE;
				case 'h': return SourceType.FARHANG_QURAN;
				case 't': return SourceType.TEBYAN;
				case 's': return SourceType.TTS;											
				default	: MyError.exit("Bad resource descriptor in concept name!");
			}
		}
		
		return SourceType.UNKNOWN;
	}
	
	/**
	 * Trims the node name by removing conflicting characters ()
	 * Please note that the name cannot contain '#', '˸', or '§' characters because they are used internally and have special meaning.
	 * Also double quotation marks are removed from double quoted words.
	 * 
	 * @param name
	 * @return
	 */
	public static String trimName(String name)
	{
		// #: sense
		// §: synset
		// ˸: source
		// next free = ₪
		
		name = name.replaceAll("#", "_").replaceAll("§", "_").replaceAll("˸", "_").replaceAll("\t", " ").replaceAll("  ", " ").trim();
		
		name = Common.normalizeNotTokenized(name);
		
		if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1)
		{
			name = name.substring(1, name.length() - 1);
		}
		
		return name;
	}

	/**
	 * get the next instance id which will be created having this node as its conceptType
	 * @return an instance number
	 */
	private synchronized int getNextInstanceId()
	{
		return ++_numberOfInstances;
	}
	
	/**
	 * it is called to notify the node that an instance of this node has been deleted. 
	 * @return the number of remained instances of this conceptType
	 */
	public synchronized int removeInstance()
	{
		return --_numberOfInstances;
	}

	/**
	 * adds a new incoming link to this node 
	 * @param SourceType the source node
	 * @param pr the relation
	 */
	public void addInLink(Node Source, PlausibleStatement pr)
	{
		InLinkElement InLink = new InLinkElement(pr);

		InLink.sourceNode = Source;

		if (_lastInLink == null)
		{
			_lastInLink = InLink;
		}
		else
		{
			InLink.previousInLinkElement = _lastInLink;
			_lastInLink = InLink;
		}
		
		_inDegree++;
		
		synchronized(_maxInDegree)
		{		
			if (_inDegree > _maxInDegree)
			{
				_maxInDegree = _inDegree;
			}
		}
	}
	
	/**
	 * removes an incoming link to this node
	 * @param pr the relation
	 */
	public void removeInLink(PlausibleStatement pr)
	{
		InLinkElement InLink = _lastInLink;
		InLinkElement LastChecked = null;

		while (InLink != null)
		{
			if (InLink.relation == pr)
			{
				if (LastChecked == null)
				{
					_lastInLink = InLink.previousInLinkElement;
				}
				else
				{
					LastChecked.previousInLinkElement = InLink.previousInLinkElement;
				}
				
				InLink.relation = null;
				InLink.sourceNode = null;
				
				_inDegree--;
				
				return;
			}

			LastChecked = InLink;
			InLink = InLink.previousInLinkElement;
		}
	}

	/**
	 * adds an outgoing link from this node
	 * @param Destination the target node
	 * @param pr the relation
	 */
	public void addOutLink(Node Destination, PlausibleStatement pr)
	{
		OutLinkElement OutLink = new OutLinkElement(pr);

		OutLink.destinationNode = Destination;

		if (_lastOutLink == null)
		{
			_lastOutLink = OutLink;
		}
		else
		{
			OutLink.previousOutLinkElement = _lastOutLink;
			_lastOutLink = OutLink;
		}
		
		_outDegree++;
		
		synchronized (_maxOutDegree)
		{		
			if (_outDegree > _maxOutDegree)
				_maxOutDegree = _outDegree;
		}
	}
	
	/**
	 * removes an outgoing relation from this node
	 * @param pr the relation
	 */
	public void removeOutLink(PlausibleStatement pr)
	{
		OutLinkElement OutLink = _lastOutLink;
		OutLinkElement LastChecked = null;

		while (OutLink != null)
		{
			if (OutLink.relation == pr)
			{
				if (LastChecked == null)
				{
					_lastOutLink = OutLink.previousOutLinkElement;
				}
				else
				{
					LastChecked.previousOutLinkElement = OutLink.previousOutLinkElement;
				}
				
				OutLink.relation = null;
				OutLink.destinationNode = null;
				
				_outDegree--;
				
				return;
			}

			LastChecked = OutLink;
			OutLink = OutLink.previousOutLinkElement;
		}
	}

	/**
	 * Composes a text representation of the concept and its relations. also shows lexical info (senses) related to the concept 
	 * @return a text representation of the concept and its relations
	 */
	public String getNodeData()
	{
		if (!isLex())
		{
			return getNodeDumpBrief();
		}

		String Output = "";

		ArrayList<PlausibleAnswer> Lexs = findTargetNodes(KnowledgeBase.HPR_LEX);

		Output = "Node: " + _name + " (Lexical)\r\n";
		Output += "Related Concepts: " + Lexs.size() + "\r\n";
		
		Integer Counter = 0;
		
		for(PlausibleAnswer PA: Lexs)
		{
			Counter++;
			Output += "\r---------- (" + Counter.toString() + ") ----------\r\n";
			Output += PA.answer.getNodeDump();	
		}

		return Output;
	}
	
	/**
	 * Composes a text representation of the concept and its relations. 
	 * @return a text representation of the concept and its relations
	 */
	public String getNodeDump()
	{	
		String Output = "";
		String Message = "";

		Output = "Node: " + this._name;
		
		if (this._source != SourceType.UNKNOWN)
		{
			Output += this._source.toString().toLowerCase() + "\r\n";
		}

		Message += Output + "\r\n";

		if (this._name.charAt(0) == '*') // Statement nodes start with a '*' 
		{
			PlausibleStatement PR = (PlausibleStatement)this;

			String Type		= PR.conditionalType.toString();
			String Source	= PR.argument._name;
			String Target	= PR.referent._name;
			Message += "conceptType: " + Type + "\r\n";
			Message += "SourceType: " + Source + "\r\n";
			Message += "Target: " + Target + "\r\n";
		}

		Message += "---------- رابطه های این مفهوم به دیگر مفاهیم ----------\r\n";

		Integer Num = 1;
		ArrayList<PlausibleAnswer> CXs;
		
		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			Output = "   " + Num.toString() + ". ";
			Output += _name;
			Output += " -- ";
			Output += OutLink.relation.relationType._name;
			Output += " --> ";
			Output += OutLink.destinationNode._name;

			CXs = OutLink.relation.findTargetNodes(KnowledgeBase.HPR_CX);

			Output += Common.composeCX(CXs);

			Output += "\t";
			Output += OutLink.relation.parameters.toString();

			if (OutLink.relation.conditionalType != ConditionalType.NOT_CONDITIONAL)
			{
				Output += " (" + OutLink.relation.conditionalType.toString() + ")";
			}

			Message += Output + "\r\n";

			OutLink = OutLink.previousOutLinkElement;

			Num++;
		}

		Message += "---------- رابطه های دیگر مفاهیم به این مفهوم ---------- \r\n";

		Num = 1;

		InLinkElement InLink = _lastInLink;

		while (InLink != null)
		{
			Output = "   " + Num.toString() + ". ";
			Output += InLink.sourceNode._name;
			Output += " -- ";
			Output += InLink.relation.relationType.toString();
			Output += " --> ";
			Output += _name;

			Message += Output + "\r\n";

			InLink = InLink.previousInLinkElement;

			Num++;
		}

		return Message;
	}
	
	/**
	 * Composes a brief text representation of the concept and its relations.
	 * @return
	 */
	public String getNodeDumpBrief()
	{	
		String Output = "";
		String Message = "";

		Output = this._name;
		Output += "\r\n" + this._id;

		Message += Output + "\r\n\r\n";

		if (this._name.charAt(0) == '*') // Statement nodes start with a '*' 
		{
			PlausibleStatement pr = (PlausibleStatement)this;

			Message += "conceptType: " + pr.conditionalType.toString() + "\r\n";
			Message += "SourceType: " + pr.argument._name + "\r\n";
			Message += "Target: " + pr.referent._name + "\r\n";
		}

		Message += "---------- رابطه های این مفهوم به دیگر مفاهیم ---------- \r\n\r\n";

		Integer Num = 1;
		ArrayList<PlausibleAnswer> CXs;
		
		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			Output = Num.toString() + ". ";			
			Output += OutLink.relation.relationType._name;
			Output += "\t --> ";
			Output += OutLink.destinationNode._name;

			CXs = OutLink.relation.findTargetNodes(KnowledgeBase.HPR_CX);

			Output += Common.composeCX(CXs);

			if (OutLink.relation.conditionalType != ConditionalType.NOT_CONDITIONAL)
			{
				Output += " (" + OutLink.relation.conditionalType.toString() + ")";
			}

			Message += Output + "\r\n";

			OutLink = OutLink.previousOutLinkElement;

			Num++;
		}

		Message += "\r\n---------- رابطه های دیگر مفاهیم به این مفهوم ---------- \r\n\r\n";

		Num = 1;

		InLinkElement InLink = _lastInLink;

		while (InLink != null)
		{
			Output = Num.toString() + ". ";
			Output += InLink.relation.relationType.toString();
			Output += "\t<-- ";
			Output += InLink.sourceNode._name;

			Message += Output + "\r\n";

			InLink = InLink.previousInLinkElement;

			Num++;
		}

		return Message;
	}
	
	/**
	 * Finds the relation template associated with a relation and fills it to create a human readable sentence
	 * @param argument source of the relation
	 * @param referent destination of the relation
	 * @param relationType relation type
	 * @return
	 */
	public String fillRelationTemplate(Node argument, Node referent, Node relationType)
	{
		ArrayList<PlausibleAnswer> templates = relationType.findTargetNodes(KnowledgeBase.HPR_TEMPLATE);
		
		Node template = null;
		
		if (templates.size() > 0)
		{
			template = templates.get(0).answer;
		}
		
		if (template == null)
		{
			return "";
		}
		
		String out = template._name.toLowerCase().replace("s", "« " + argument._name + "» ").replace("o", "« " + referent._name + "» ");
		
		return out;
	}
	
	public String getNodeKnowledgeHumanReadable()
	{
		return getNodeKnowledgeHumanReadable(Integer.MAX_VALUE);
	}
	/**
	 * Composes the knowledge regarding a concept as human readable sentences
	 * @param limit the maximum number of relations to be considered
	 * @return human readable text representation of the knowledge regarding the concept
	 */
	public String getNodeKnowledgeHumanReadable(int limit)
	{	
		StringBuilder buffer = new StringBuilder();

		buffer.append("دانش موجود در مورد مفهوم " + this._name + ":\r\n\r\n");

		String temp = "";
		ArrayList<PlausibleAnswer> cx;
		int num = 1;
		
		OutLinkElement outLink = _lastOutLink;
		
		while (outLink != null && num <= limit / 2)
		{
			temp = fillRelationTemplate(outLink.relation.argument, outLink.relation.referent, outLink.relation.relationType);
			
			if (temp.isEmpty()) // ignoring template-less relations 
			{
				outLink = outLink.previousOutLinkElement;
				continue;
			}
			
			buffer.append(num + ". " + temp);
			
			cx = outLink.relation.findTargetNodes(KnowledgeBase.HPR_CX);

			buffer.append(Common.composeCXHumanReadable(cx));

			if (outLink.relation.conditionalType != ConditionalType.NOT_CONDITIONAL)
			{
				buffer.append(" (" + outLink.relation.conditionalType.toString() + ")");
			}

			buffer.append("\r\n");

			outLink = outLink.previousOutLinkElement;

			num++;
		}

		InLinkElement inLink = _lastInLink;

		while (inLink != null && num <= limit / 2)
		{
			temp = fillRelationTemplate(inLink.relation.argument, inLink.relation.referent, inLink.relation.relationType);
			
			if (temp.isEmpty())
			{
				inLink = inLink.previousInLinkElement;
				continue;
			}
			
			buffer.append(num + ". " + temp);
			
			buffer.append("\r\n");

			inLink = inLink.previousInLinkElement;

			num++;
		}

		return buffer.toString();
	}
	
	
	public int exportNode(BufferedWriter stream, int row) throws IOException	
	{
		return exportNode(stream, row, false);
	}
	/**
	 * Writes node knowledge to a stream
	 * @param stream where the relations are written
	 * @param row the row number to start with
	 * @param isRelation whether it is a relation or normal concept
	 * @return
	 * @throws IOException
	 */
	public int exportNode(BufferedWriter stream, Integer row, boolean isRelation) throws IOException 
	{
		if (_lastOutLink == null)
			return row;
		
		String Output = "";

		OutLinkElement OutLink = _lastOutLink;

		String ConceptName = prepareConceptNameForExport(this);
		
		if (isRelation == true)
		{
			ConceptName = "*(" + row.toString()+")";
		}

		PlausibleStatement Antecedent;
		PlausibleStatement Consequence;

		ArrayList<PlausibleStatement> IMPStatements;

		while (OutLink != null)
		{
			if (OutLink.relation.conditionalType == ConditionalType.NOT_CONDITIONAL)
			{
				row++;
				
				Output = row.toString();
				Output += "\t";
				Output += ConceptName;
				Output += "\t";
				Output += prepareConceptNameForExport(OutLink.relation);
				Output += "\t";
				Output += prepareConceptNameForExport(OutLink.destinationNode);
				Output += "\t";
				Output += OutLink.relation.parameters.toString();
				Output += "\r\n";
				
				stream.write(Output);

				row = OutLink.relation.exportNode(stream, row, true);
			}
			else if (OutLink.relation.IsAntecedentStatement())
			{
				IMPStatements = OutLink.relation.findOutRelations(KnowledgeBase.HPR_IMP);

				for (PlausibleStatement IMPStatement: IMPStatements)
				{
					row++;

					Antecedent = (PlausibleStatement)IMPStatement.argument;
					Consequence = (PlausibleStatement)IMPStatement.referent;

					Output = row.toString();
					Output += "\t";
					Output += Antecedent.relationType._name + "(" + Antecedent.argument._name + ")={" + Antecedent.referent._name + "}";
					Output += "\tIMP\t";
					Output += Consequence.relationType._name + "(" + Consequence.argument._name + ")={" + Consequence.referent._name + "}";
					Output += "\t";
					Output += IMPStatement.parameters.toString();
					Output += "\r\n";

					stream.write(Output);
				}
			}

			OutLink = OutLink.previousOutLinkElement;
		}
		
		return row;
	}
	
	/**
	 * Prepares the concept name for export KB into files. some special characters such as CR/LF and tabs are replaced.
	 * @param node
	 * @return
	 */
	private String prepareConceptNameForExport(Node node)
	{
		String conceptName;
		
		if (node.getClass() == PlausibleStatement.class)
			conceptName = ((PlausibleStatement)node).relationType._name;
		else
			conceptName = node._name;
		
		// the character ¶ is used to denote carriage return/line feed (CR/LF)
		conceptName = conceptName.replaceAll("\r?\n", "¶");
		conceptName = conceptName.replaceAll("\r", "¶");
		
		// the character º is used to denote tabs
		//conceptName = conceptName.replaceAll("\t", "º");
		
		if (node.isDescriptive())
		{
			conceptName = "\"" + conceptName + "\"";
		}
		
		String prefix = "";
		
		switch (node._source)
		{
			case FARSNET		: prefix = "f˸"; break;
			case WORDNET		: prefix = "w˸"; break;
			case ESTELAHNAME	: prefix = "e˸"; break;
			case QURAN			: prefix = "q˸"; break;
			case TAFSIR_NEMOONE	: prefix = "n˸"; break;
			case FARHANG_QURAN	: prefix = "h˸"; break;
			case TEBYAN			: prefix = "t˸"; break;
			case UNKNOWN		: prefix = "";   break;
		}
		
		return prefix + conceptName;
	}
	
	/**
	 * Does the reverse of 'prepareConceptNameForExport' by restoring special characters such as CR/LF and tabs. 
	 * @param conceptName
	 * @return
	 */
	public static String prepareConceptNameForImport(String conceptName)
	{
		// the character ¶ is used to denote carriage return/line feed (CR/LF)
		conceptName = conceptName.replaceAll("¶", "\r\n");
		// the character º is used to denote tabs
		//conceptName = conceptName.replaceAll("º", "\t");
		
		return conceptName;
	}

	/**
	 * finds relations from this node to a target node
	 * @param relation relation conceptType to be checked
	 * @param destinationNode target node
	 * @return a relation
	 */
	public PlausibleStatement findRelationToTarget(Node relation, Node destinationNode)
	{
		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			if (OutLink.destinationNode == destinationNode)
			{
				if (OutLink.relation.relationType == relation)
				{
					return OutLink.relation;
				}
			}

			OutLink = OutLink.previousOutLinkElement;
		}
		
		return null;
	}

	
	/**
	 * finds relations from this node to a target node
	 * @param relation relation conceptType to be checked
	 * @param destinationNode target node
	 * @return a relation
	 */
	public PlausibleStatement findRelationToTarget2(Node relation, Node destinationNode)
	{
		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			if (OutLink.destinationNode == destinationNode)
			{
				if (OutLink.relation == relation)
				{
					return OutLink.relation;
				}
			}

			OutLink = OutLink.previousOutLinkElement;
		}
		
		return null;
	}

	
	/**
	 * Finds the relation this node receives from <code>sourceNode</code>
	 * @param relation relation
	 * @param sourceNode the souce of the relation
	 * @return the found relation
	 */
	public PlausibleStatement findRelationFromSource(Node relation, Node sourceNode)
	{
		InLinkElement inLink = _lastInLink;

		while (inLink != null)
		{
			if (inLink.sourceNode == sourceNode)
			{
				if (inLink.relation.relationType == relation)
				{
					return inLink.relation;
				}
			}

			inLink = inLink.previousInLinkElement;
		}
		
		return null;
	}

	/**
	 * finds out relations from this node
	 * @param relationType relation conceptType
	 * @return a list of relations
	 */
	public ArrayList<PlausibleStatement> findOutRelations(Node RelationType)
	{
		ArrayList<PlausibleStatement> AL = new ArrayList<PlausibleStatement>();
		
		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			// TODO: should I check for ConditionalType.NOT_CONDITIONAL as in GetAllStatements
			if (RelationType == KnowledgeBase.HPR_ANY || OutLink.relation.relationType == RelationType)
			{
				AL.add(OutLink.relation);
			}

			OutLink = OutLink.previousOutLinkElement;
		}
		
		return AL;
	}
	
	/**
	 * finds in relations to this node 
	 * @param relationType relation conceptType
	 * @return a list of relations
	 */
	public ArrayList<PlausibleStatement> findInRelations(Node RelationType)
	{
		ArrayList<PlausibleStatement> AL = new ArrayList<PlausibleStatement>();
		
		InLinkElement InLink = _lastInLink;

		while (InLink != null)
		{
			if (RelationType == KnowledgeBase.HPR_ANY || InLink.relation.relationType == RelationType)
			{
				AL.add(InLink.relation);
			}

			InLink = InLink.previousInLinkElement;
		}
		
		return AL;
	}


	public ArrayList<PlausibleAnswer> findTargetNodes(Node relationType)
	{
		return findTargetNodes(relationType, ConditionalType.NOT_CONDITIONAL, null, null);
	}
	public ArrayList<PlausibleAnswer> findTargetNodes(Node relationType, ConditionalType statType)
	{
		return findTargetNodes(relationType, statType, null, null);
	}
	public ArrayList<PlausibleAnswer> findTargetNodes(Node relationType, Node transparentRelation)
	{
		return findTargetNodes(relationType, ConditionalType.NOT_CONDITIONAL, transparentRelation, null);
	}
	public ArrayList<PlausibleAnswer> findTargetNodes(Node relationType, Node transparentRelation, ArrayList<Node> cxs)
	{
		return findTargetNodes(relationType, ConditionalType.NOT_CONDITIONAL, transparentRelation, cxs);
	}
	/**
	 * Finds nodes which this node has a relation with
	 * @param relationType relation type
	 * @param statType whether we are looking for a conditional statement of a normal one
	 * @param transparentRelation which relation should be considered transparent (i.e. relations of this kind are ignored when traversing the node)
	 * @param cxs contexts seen
	 * @return founds nodes
	 */
	public ArrayList<PlausibleAnswer> findTargetNodes(Node relationType, ConditionalType statType, Node transparentRelation, ArrayList<Node> cxs)
	{
		ArrayList<PlausibleAnswer> outs = new ArrayList<PlausibleAnswer>();
		PlausibleAnswer answer;

		OutLinkElement outLink = _lastOutLink;
		
		while (outLink != null)
		{
			if ((outLink.relation.relationType == relationType || relationType == KnowledgeBase.HPR_ANY) && outLink.relation.conditionalType == statType)
			{
				answer = new PlausibleAnswer();
				
				answer.answer 		= outLink.destinationNode;
				answer.statement 	= outLink.relation;
				answer.source		= outLink.relation.getSourceType();				
				answer.copyParameters(outLink.relation.parameters);
				
				if (!Common.isEmpty(cxs))
				{
					for (Node CX: cxs)
					{
						answer.contexs = outLink.relation.findTargetNodes(CX);
					}
				}
				
				outs.add(answer);
				
				//outLink.relation.relationType._accessed++;
			}

			outLink = outLink.previousOutLinkElement;
		}
		
		//TODO: marked as unnecessary, May 24 2013
		/*
		if (TransparentRelation != null)
		{
			ArrayList<?> SourceSynsets = findTargetNodes(TransparentRelation);
			if (SourceSynsets.size() != 0)
			{
				PlausibleAnswer SourceSynsetAnswer = (PlausibleAnswer)SourceSynsets.get(0);
				Node SourceSynset = SourceSynsetAnswer.answer;

				ArrayList<PlausibleAnswer> TragetSynsets = SourceSynset.findTargetNodes(RelationType);
				ArrayList<PlausibleAnswer> TargetNodes;

				for (PlausibleAnswer PA: TragetSynsets)
				{
					TargetNodes = PA.answer.findTargetNodes(TransparentRelation);

					if (RelationType != TransparentRelation)
					{
						for (PlausibleAnswer TargetNode: TargetNodes)
						{
							TargetNode.CopyCertaintyParameters(PA.parameters);
						}
					}

					answers.addAll(TargetNodes);
				}
			}
		}
		*/

		return outs;
	}
	
	
	public ArrayList<PlausibleAnswer> findSourceNodes(Node relationType)
	{
		return findSourceNodes(relationType, ConditionalType.NOT_CONDITIONAL, null, null);
	}
	public ArrayList<PlausibleAnswer> findSourceNodes(Node relationType, ConditionalType statType)
	{
		return findSourceNodes(relationType, statType, null, null);
	}
	public ArrayList<PlausibleAnswer> findSourceNodes(Node relationType, Node transparentRelation)
	{
		return findSourceNodes(relationType, ConditionalType.NOT_CONDITIONAL, transparentRelation, null);
	}
	public ArrayList<PlausibleAnswer> findSourceNodes(Node relationType, Node transparentRelation, ArrayList<Node> cxs)
	{
		return findSourceNodes(relationType, ConditionalType.NOT_CONDITIONAL, transparentRelation, cxs);
	}
	/**
	 * Finds nodes which have a relation to this node
	 * @param relationType relation type
	 * @param statType whether we are looking for a conditional statement of a normal one
	 * @param transparentRelation which relation should be considered transparent (i.e. relations of this kind are ignored when traversing the node)
	 * @param cxs contexts seen
	 * @return founds nodes
	 */
	public ArrayList<PlausibleAnswer> findSourceNodes(Node relationType, ConditionalType statType, Node transparentRelation, ArrayList<Node> cxs)
	{
		ArrayList<PlausibleAnswer> outs = new ArrayList<PlausibleAnswer>();
		PlausibleAnswer answer;

		InLinkElement inLink = _lastInLink;
		
		while (inLink != null)
		{
			if ((inLink.relation.relationType == relationType || relationType == KnowledgeBase.HPR_ANY) && inLink.relation.conditionalType == statType)
			{
				answer = new PlausibleAnswer();
				
				answer.answer 		= inLink.sourceNode;
				answer.statement 	= inLink.relation;
				answer.source 		= inLink.relation.getSourceType();
				answer.copyParameters(inLink.relation.parameters);
				
				if (!Common.isEmpty(cxs))
				{
					for (Node CX: cxs)
					{
						answer.contexs = inLink.relation.findTargetNodes(CX);
					}
				}

				outs.add(answer);
				
				//inLink.relation.relationType._accessed++;
			}

			inLink = inLink.previousInLinkElement;
		}
		
		//TODO: marked as unnecessary, May 24 2013
		/*
		if (TransparentRelation != null)
		{
			ArrayList<?> SourceSynsets = findTargetNodes(TransparentRelation);
			if (SourceSynsets.size() != 0)
			{
				PlausibleAnswer SourceSynsetAnswer = (PlausibleAnswer)SourceSynsets.get(0);
				Node SourceSynset = SourceSynsetAnswer.answer;

				ArrayList<PlausibleAnswer> TragetSynsets = SourceSynset.findSourceNodes(RelationType);
				ArrayList<PlausibleAnswer> TargetNodes;

				for (PlausibleAnswer PA: TragetSynsets)
				{
					TargetNodes = PA.answer.findTargetNodes(TransparentRelation);
					
					if (RelationType != TransparentRelation)
					{
						for (PlausibleAnswer TargetNode: TargetNodes)
						{
							TargetNode.CopyCertaintyParameters(PA.parameters);
						}
					}

					AL.addAll(TargetNodes);
				}
			}
		}
		*/

		return outs;		
	}
	
	public ArrayList<PlausibleStatement> getAllStatements()
	{
		return getAllStatements(null);
	}
	/**
	 * Finds all relations going out this node. it's similar to <code>FindOutRelations</code> but retrieves all relations
	 * @param transparentRelation which relation should be considered transparent (i.e. relations of this kind are ignored when traversing the node)
	 * @return found nodes
	 */
	public ArrayList<PlausibleStatement> getAllStatements(Node transparentRelation)
	{
		ArrayList<PlausibleStatement> OutRelations = new ArrayList<PlausibleStatement>();

		OutLinkElement OutLink = _lastOutLink;

		while (OutLink != null)
		{
			if (OutLink.relation.conditionalType == ConditionalType.NOT_CONDITIONAL && OutLink.relation.relationType != transparentRelation)
			{
				OutRelations.add(OutLink.relation);
			}

			OutLink = OutLink.previousOutLinkElement;				
		}

		return OutRelations;
	}

	/** 
	 * Changes the concept name
	 * @param name new name
	 * @param trimName should trim the name according to the naming regulations?
	 */
	public void changeName(String name, boolean trimName)
	{
		if (trimName)
		{
			name = trimName(name);
		}
		else
		{
			name = extractPropertiesFromName(name);
		}
		
		this._name = name;		
	}
	
	/**
	 * Getter for <code>_numberOfInstances</code>
	 * @return
	 */
	public int getInstancesNum()
	{
		return _numberOfInstances;
	}

	/**
	 * Determines whether it is a lexical node.
	 * @return true if it is a lexical node, false otherwise
	 */
	public boolean isLex()
	{
		OutLinkElement OutLink = _lastOutLink;
		
		while (OutLink != null)
		{
			if (OutLink.relation.relationType._name == "LEX")
			{
				return true;
			}

			OutLink = OutLink.previousOutLinkElement;
		}
		
		return false;
	}
	
	/**
	 * Computes the depth of the node in the ISA hierarchy recursively.
	 *  
	 * @return the depth
	 */
	public int computeDepthInISAHierarchy()
	{
		// Extracting those nodes with an ISA (or INS) relation to Child
		ArrayList<PlausibleAnswer> ParentNodes = findTargetNodes(KnowledgeBase.HPR_ISA);
		ParentNodes.addAll(findTargetNodes(KnowledgeBase.HPR_INSTANCE));
		
		int Depth;
		int MinDepth = Integer.MAX_VALUE; // A big depth
		Node ParentNode;
		
		for (PlausibleAnswer panswer: ParentNodes)
		{
			ParentNode = ((PlausibleAnswer)panswer).answer;
		
			Depth = ParentNode.computeDepthInISAHierarchy();
			 
			if (Depth < MinDepth)
			{
				MinDepth = Depth;
			}			

			break;
		}

		if (MinDepth == Integer.MAX_VALUE)
		{
			MinDepth = 0;
		}
	
		return MinDepth + 1;
	}


	/**
	 * Finds an antonym of this node
	 * @return the antonym
	 */
	public Node getAntonym()
	{
		Node DIS = KnowledgeBase.HPR_DIS;
		Node SIM = KnowledgeBase.HPR_SIM;
		
		ArrayList<?> Antonyms = findTargetNodes(DIS, SIM);

		if (Antonyms == null || Antonyms.size() == 0)
		{
			return null;
		}

		return ((PlausibleAnswer)Antonyms.get(0)).answer;
	}
	
	/**
	 * Finds all parents of a node in a limited search radius.
	 * 
	 * @param maxSearchDepth the maximum depth for search 
	 * @return all parents in range
	 */
	public ArrayList<Node> findAllParents(int maxSearchDepth)
	{
		if (maxSearchDepth <= 0)
		{
			// backtrack:
			return null;
		}

		maxSearchDepth--;
		
		ArrayList<Node> answers = new ArrayList<Node>();

		ArrayList<PlausibleAnswer> parents;
		ArrayList<Node> nextLevelParents;

		Node Parent;

		parents = findTargetNodes(KnowledgeBase.HPR_ISA);

		if (parents != null)
		{
			for (PlausibleAnswer parentAnswer: parents)
			{
				Parent = parentAnswer.answer;

				answers.add(Parent);
				
				nextLevelParents = Parent.findAllParents(maxSearchDepth);
				if (nextLevelParents != null)
				{
					answers.addAll(nextLevelParents);
				}
			}
		}

		return answers;
	}
	
	@Override
	public String toString()
	{
		return this._name;
	}

	@Override
	public int compareTo(Node node)
	{
		return this._name.compareTo(node._name);
	}
	
	/**
	 * Shallow copies this node to another one.
	 * Notice: the name is not copied.
	 * 
	 * @param copy the destination node to receive the copy
	 */
	public void copyShallowTo(Node copy)
	{
		copy._dynamic 			= this._dynamic;
		copy._numberOfInstances = this._numberOfInstances;
		copy.WNHeadWord 		= new String(this.WNHeadWord);
		copy._source 			= this._source;
		copy.WNTaggedCount 		= this.WNTaggedCount;
		copy.WNRank 			= this.WNRank;
		copy.setPos(this.getPos());
		copy.senseNo 			= this.senseNo;
		//copy.ava 				= new String(this.ava);
		copy.distance 			= this.distance;
		//copy.ancestor 		= this.ancestor;		
		copy._lastInLink 		= this._lastInLink;
		copy._lastOutLink 		= this._lastOutLink;
		//clone.lastInLink 		= this.lastInLink.clone();
		//clone.lastOutLink 	= this.lastOutLink.clone();
	}
	
	/**
	 * Unbinds a node from all its relations. Used when deleting a node.
	 * @return The number of relations which were unbound. 
	 */
	public int unbindRelations()
	{
		int relationsUnbound = 0;
		
		OutLinkElement outLink = _lastOutLink;

		while (outLink != null)
		{
			outLink.relation.unbindRelations();
			
			relationsUnbound++;

			outLink = outLink.previousOutLinkElement;
		}
		
		InLinkElement inLink = _lastInLink;

		while (inLink != null)
		{
			inLink.relation.unbindRelations();
			
			relationsUnbound++;

			inLink = inLink.previousInLinkElement;
		}
		
		return relationsUnbound;
	}
	
	/**
	 * removes a relation from a node and moves it to another one
	 * @param newOwner the new owner of the relation
	 */
	public void moveRelationsTo(Node newOwner)
	{
		OutLinkElement outLink = _lastOutLink;

		while (outLink != null)
		{
			outLink.relation.changeArgument(newOwner);
			
			outLink = outLink.previousOutLinkElement;
		}
		
		InLinkElement inLink = _lastInLink;

		while (inLink != null)
		{
			inLink.relation.changeReferent(newOwner);
			
			inLink = inLink.previousInLinkElement;
		}
	}
	
	/**
	 * Creates an id for next JUNG relation
	 * @return
	 */
	private synchronized String getNextEdgeLabelId()
	{
		return (++_edgeLabeler).toString();
	}
	
	
	/**
	 * This method creates a graph representation of a concept used to visualize it via JUNG library
	 * @return
	 */
	public DirectedSparseGraph<Node, String> getJungGraph()
	{
		DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();
		
		graph.addVertex(this);
		
		OutLinkElement outLink = _lastOutLink;

		while (outLink != null)
		{
			graph.addVertex(outLink.relation);
			
			graph.addEdge(getNextEdgeLabelId(), this, outLink.relation, EdgeType.DIRECTED);
			
			graph.addVertex(outLink.relation.referent);
			
			graph.addEdge(getNextEdgeLabelId(), outLink.relation, outLink.relation.referent, EdgeType.DIRECTED);
			
			outLink = outLink.previousOutLinkElement;
		}
		
		InLinkElement inLink = _lastInLink;

		while (inLink != null)
		{
			graph.addVertex(inLink.relation);
			
			graph.addEdge(getNextEdgeLabelId(), inLink.relation, this, EdgeType.DIRECTED);
			
			graph.addVertex(inLink.relation.argument);
			
			graph.addEdge(getNextEdgeLabelId(), inLink.relation.argument, inLink.relation, EdgeType.DIRECTED);
			
			inLink = inLink.previousInLinkElement;
		}
				
		return graph;
	}
	
	/**
	 * Extracts the node's relations for webservice graphical output
	 * @param offset start from a relation number
	 * @param maxOutputSize the maximum number of relation to produce
	 * @return relations
	 */

	
	
	/**
	 * Verifies if the concept has any references.
	 * @return
	 */
	public boolean hasReference()
	{
		if (this.findTargetNodes(KnowledgeBase.HPR_REF).size() > 0)
			return true;
		
		return false;
	}
	
	/**
	 * Checks whether the node has a certain parent 
	 * @param expectedParent parent
	 * @return true if the node is a descendant of the parent
	 */
	public boolean hasParent(Node expectedParent)
	{
		if (expectedParent == KnowledgeBase.HPR_ANY)
			return true;
		
		HashSet<Node> seens = new HashSet<Node>();		
		int result = _hasParent(expectedParent, seens);
		
		if (result == 1)
			return true;
		
		return false;
	}
	private int _hasParent(Node expectedParent, HashSet<Node> seens)
	{
		if (seens.contains(this))
			return 0; 
		else
			seens.add(this);
			
		ArrayList<PlausibleAnswer> parents = findTargetNodes(KnowledgeBase.HPR_ISA);
		
		for (PlausibleAnswer parent: parents)
		{
			if (parent.answer == expectedParent)
				return 1;
			
			int parentCheck = parent.answer._hasParent(expectedParent, seens);
			
			if (parentCheck == 1)
				return 1;
		}
		
		return -1;
	}
	
	/**
	 * Checks whether the node has a direct certain parent 
	 * @param expectedParent parent
	 * @return true if the node is a direct descendant of the parent
	 */
	public boolean hasDirectParent(Node expectedParent)
	{
		ArrayList<PlausibleAnswer> parents = findTargetNodes(KnowledgeBase.HPR_ISA);
		
		for (PlausibleAnswer parent: parents)
		{
			if (parent.answer == expectedParent)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Extract all descendants of the node
	 * @return all descendants of the node
	 */
	public HashSet<Node> extractDescendants()
	{
		HashSet<Node> seens = new HashSet<Node>();
		_extractDescendants(seens);
		
		return seens;
	}
	private void _extractDescendants(HashSet<Node> seens)
	{
		if (seens.contains(this))
			return;
		
		seens.add(this);
		
		ArrayList<PlausibleAnswer> children = this.findSourceNodes(KnowledgeBase.HPR_ISA);
		
		for (PlausibleAnswer child: children)
		{
			child.answer._extractDescendants(seens);
		}
	}
	
	
	/**
	 * Extract all descendants of the node which are leaves (has no descendants by themselves)
	 * @return leaf descendants
	 */
	public HashSet<Node> extractLeafDescendants()
	{
		HashSet<Node> seens = new HashSet<Node>();
		
		return _extractLeafDescendants(seens);
	}
	private HashSet<Node> _extractLeafDescendants(HashSet<Node> seens)
	{
		HashSet<Node> out = new HashSet<Node>();
		
		if (seens.contains(this))
			return out;
		
		seens.add(this);
		
		ArrayList<PlausibleAnswer> children = this.findSourceNodes(KnowledgeBase.HPR_ISA);
		
		if (children.size() == 0)
		{
			if (StringUtils.countMatches(this._name, "(") <= 1) // محمد(ص)ـ  is ok but محمد(ص)(در بیابان)ـ is not			
				out.add(this);
		
			return out;
		}
		
		boolean isPseudoLeaf = true;
		
		for (PlausibleAnswer child: children)
		{
			//TODO: to remedy اصحاب سبت در قیامت  -- ISA --> اصحاب سبت
			if (	!child.answer._name.matches("^" + Pattern.quote(this._name) + "\\s*\\(در .*") && 
					!child.answer._name.matches("^" + Pattern.quote(this._name) + "\\s*\\(از .*") && 
					!child.answer._name.matches("^" + Pattern.quote(this._name) + "\\s*\\(به .*")) 
				isPseudoLeaf = false;
		}
		
		if (isPseudoLeaf)
		{
			out.add(this);
			
			// ignoring the children
			return out;
		}
		
		for (PlausibleAnswer child: children)
		{
			out.addAll(child.answer._extractLeafDescendants(seens));			
		}
		
		return out;
	}
	
	/**
	 * Computes a weight for the node based on its in and out degrees
	 * @return
	 */
	public int getWeight()
	{
		return ((_inDegree/_maxInDegree) + (_outDegree/_maxOutDegree)) / 2;
	}
	
	/**
	 * A relaxed version of <code>equal</code> method. 
	 * If the tokenized or lemmatized version of two nodes are equal the it return true.
	 * @param counterpart the other concept to match
	 * @return whether two nodes can be considered equal (relaxed)
	 */
	public boolean equalsRelaxed(Node counterpart)
	{
		if (this._name.equals(counterpart._name))
			return true;
		
		if (!this.getTokenized().isEmpty() && !counterpart.getTokenized().isEmpty() && this.getTokenized().equals(counterpart.getTokenized()))
			return true;
		
		if (!this.getLemmatized().isEmpty() && !counterpart.getLemmatized().isEmpty() && this.getLemmatized().equals(counterpart.getLemmatized()))
			return true;
		
		return false;
	}
	
	/**
	 * Verifies if the node's name contains another node's name as a word (ie at words boundaries and NOT as simple substring)
	 * @param counterpart the other node
	 * @return true/false
	 */
	public boolean containsAsWord(Node counterpart)
	{
		//TODO: what about running Common.getLexicalRelaxations()?
		if (Common.matchAtBoundries(this._name, counterpart._name))
			return true;
		
		if (!this.getTokenized().isEmpty() && !counterpart.getTokenized().isEmpty() && Common.matchAtBoundries(this.getTokenized(), counterpart.getTokenized()))
			return true;
		
		if (!this.getLemmatized().isEmpty() && !counterpart.getLemmatized().isEmpty() && Common.matchAtBoundries(this.getLemmatized(), counterpart.getLemmatized()))
			return true;
		
		return false;
	}
	
	/**
	 * Verifies if the node's tokenized version contains another node's tokenized version as a word (ie at words boundaries and NOT as simple substring)
	 * @param counterpart the other node
	 * @return true/false
	 */
	public boolean containsAsWordTokenized(Node counterpart)
	{
		if (this._tokenized.isEmpty())
			return false;
		
		if (counterpart._tokenized.isEmpty())
			return false;
		
		//TODO: what about running Common.getLexicalRelaxations()?
		if (Common.matchAtBoundries(this._tokenized, counterpart._tokenized))
			return true;
		
		//if (Common.matchAtBoundries(Common.removeParenthesis(this._tokenized), Common.removeParenthesis(counterpart._tokenized)))
			//return true;
		
		return false;
	}
	
	/**
	 * Verifies if the node's name contains a string as a word (ie at words boundaries and NOT as simple substring)
	 * @param counterpart the string to be searched for
	 * @return true/false
	 */
	public boolean containsAsWord(String counterpart)
	{
		if (Common.matchAtBoundries(this._name, counterpart))
			return true;
		
		if (!this.getTokenized().isEmpty() && !counterpart.isEmpty() && Common.matchAtBoundries(this.getTokenized(), counterpart))
			return true;
		
		if (!this.getLemmatized().isEmpty() && !counterpart.isEmpty() && Common.matchAtBoundries(this.getLemmatized(), counterpart))
			return true;
		
		return false;
	}
	
	/**
	 * Verifies if the nodes is fully disconnected to others
	 * @return
	 */
	public boolean isOrphan()
	{
		if (_inDegree == 0 && _outDegree == 0)
			return false;
	
		return true;
	}
	
	/**
	 * Getter for <code>_inDegree</code>
	 * @return
	 */
	public int getInDegree()
	{
		return _inDegree;
	}
	
	/**
	 * Getter for <code>_outDegree</code>
	 * @return
	 */
	public int getOutDegree()
	{
		return _outDegree;
	}

	/**
	 * Getter for <code>_pos</code>
	 * @return
	 */
	public POS getPos() 
	{
		return _pos;
	}
	/**
	 * Setter for <code>_pos</code>
	 * @param pos
	 */
	public void setPos(POS pos) 
	{
		_pos = pos;
	}

	/**
	 * Getter for <code>_conceptType</code>
	 * @return
	 */
	public ConceptType getConceptType() 
	{
		return _conceptType;
	}

	/**
	 * Setter for <code>_conceptType</code>
	 * @param conceptType
	 */
	public void setConceptType(ConceptType conceptType) 
	{
		_conceptType = conceptType;
	}

	/**
	 * Getter for <code>_descriptive</code>
	 * @return
	 */
	public boolean isDescriptive()
	{
		return _descriptive;
	}

	/**
	 * Setter for <code>_descriptive</code>
	 * @param descriptive
	 */
	public void setDescriptive(boolean descriptive)
	{
		_descriptive = descriptive;
	}

	/**
	 * Getter for <code>_tokenized</code>
	 * @return
	 */
	public String getTokenized()
	{
		_accessed++;
		
		//if (!_tokenized.isEmpty())
			return _tokenized;
			
		//return Common.preprocess(_name, PreprocessorType.TOKENIZATION);
	}

	/**
	 * Setter for <code>_tokenized</code>
	 * @param tokenized
	 */
	public void setTokenized(String tokenized)
	{
		_tokenized = tokenized;
	}

	/**
	 * Getter for <code>_lemmatized</code>
	 * @return
	 */
	public String getLemmatized()
	{
		_accessed++;
		
		//if (!_lemmatized.isEmpty())
			return _lemmatized;
			
		//return Common.preprocess(_name, PreprocessorType.LEMMATIZATION);
	}

	/**
	 * Setter for <code>_lemmatized</code>
	 * @param lemmatized
	 */
	public void setLemmatized(String lemmatized)
	{
		_lemmatized = lemmatized;
	}
	
	/**
	 * Makes a shallow copy of the node
	 * @param newName new name of the copy
	 * @return copies node
	 */
	public Node makeCopy(String newName)
	{
		Node newNode = new Node(newName);
		
		newNode.setConceptType(this.getConceptType());
		newNode.setLemmatized(this.getLemmatized());
		newNode.setLexicalType(this.getLexicalType());
		newNode.setPos(this.getPos());
		newNode.setSourceType(this.getSourceType());
		newNode.setTokenized(this.getTokenized());
		
		return newNode;
	}
	
	/**
	 * Getter for <code>_accessed</code>
	 * @return
	 */
	public int getAccessed()
	{
		return _accessed;
	}
	

	/**
	 * this method returns the Synset node which originalNode has SYN relation with, 
	 * if not found searches for Synset node which originalNode has SIM relation with,
	 * if not found return null.
	 * 
	 * @param originalNode
	 * @return
	 */
	public Node getSynSet(){
				
		ArrayList<PlausibleAnswer> answers = findTargetNodes(KnowledgeBase.HPR_SYN);
				
		for (PlausibleAnswer answer: answers)
			if(answer.answer != null)
				return answer.answer;
		
		answers = findTargetNodes(KnowledgeBase.HPR_SIM);
		
		for (PlausibleAnswer answer: answers){
			Node found = answer.answer;
			if(found != null)
				if(found.getLexicalType() == LexicalType.SYNSET)
					return found;
		}
		MyError.error(this + " node has no SynSet nor SIMs to any SynSet, it is a bug in kb!");
		return null;
	}
	/**
	 * if _name of this node exists in CONTEXT enum, it is a context node.
	 * @return
	 */
	public boolean isContextNode(){
		try{
			String cxName = "";
			int index = _name.indexOf("CX:");
			if(index != -1)
				if((index + 3) < _name.length())
					cxName = _name.substring(index + 3);
			CONTEXT.valueOf(cxName);
			return true;
		}
		catch(Exception e){
			return false;
		}			
	}
	
	/**
	 * this methods return all relation of this node with relationType of CONTEXT.
	 * 
	 * @param originalNode
	 * @return
	 */
	public ArrayList<PlausibleStatement> loadCXs(){
		
		ArrayList<PlausibleStatement> outs = findOutRelations(KnowledgeBase.HPR_ANY);
		
		ArrayList<PlausibleStatement> cxs = new ArrayList<PlausibleStatement>();
		
		for(PlausibleStatement out:outs)			
			if(out.relationType != null && out.relationType.isContextNode())
				cxs.add(out);		
		
		return cxs;
	}
}