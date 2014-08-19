package ir.ac.itrc.qqa.semantic.reasoning;

import ir.ac.itrc.qqa.semantic.enums.*;
import ir.ac.itrc.qqa.semantic.kb.*;
import ir.ac.itrc.qqa.semantic.util.MyError;
import ir.ac.itrc.qqa.semantic.util.Common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * 
 * This class constitutes the core of the HPR Reasoning Engine.
 * <p>
 * It takes a knowledge base (KB) and tries to infer answers by calling the entry function called RECALL.
 * </p>
 * <p>
 * The function RECALL tries to retrieve the answer right from the memory (_kb). If it fails it resorts
 * to drawing inferences. 
 * It support 2 kind of inferences: basic , i.e., those documented in the original HPR theory and those suggested by me.
 * The original ones are:
 * </p>
 * <ul>
 *     <li>AGEN    (argument Generalization)</li>
 *     <li>ASPEC   (argument Specification)</li>
 *     <li>ASIM    (argument Similarity)</li>
 *     <li>ADIS    (argument Dissimilarity)</li>
 *     <li>RGEN    (referent Generalization)</li>
 *     <li>RSPEC   (referent Specification)</li>
 *     <li>RSIM    (referent Similarity)</li>
 *     <li>RDIS    (referent Dissimilarity)</li>
 *     <li>DEP    (Derivation from Dependency)</li>
 *     		<ul>
 *          	<li>DDEPP   (Derivation from POSITIVE Dependency)</li>
 *            	<li>DDEPN   (Derivation from NEGATIVE Dependency)</li>
 *          </ul>
 *     <li>TDEP    Transitivity Dependency</li>
 *     <li>DEPA    Dependency-based Analogy (not in the original paper by Collins & Michalski, but in a follow-up paper by one of their students)</li>
 *     <li>DIMP    Derivation from implication</li>
 * </ul> 
 * <p>
 * It then tries non-original ones, i.e. those suggested by me:
 * </p>
 * <ul>
 *     <li>DGEN    (DESCRIPTOR Generalization)</li>
 *     <li>DSPEC   (DESCRIPTOR Specification)</li>
 *     <li>DSIM    (DESCRIPTOR Similarity)</li>
 *     <li>DDIS    (DESCRIPTOR Dissimilarity)</li>
 *     <li>DSYN    (argument Synonymy)</li>
 *     <li>ASYN    (argument Synonymy)</li>
 *     <li>RSYN    (referent Synonymy)</li>   
 *     <li>Abduction (disabled)</li>
 *     <li>RCausality  referent Causality (disabled)</li>
 *     <li>ACausality  argument Causality (disabled</li>
 *     <li>Attribute   Attribute-of</li>
 *     <li>DESCRIPTOR INVERSE Transform</li> *     
 *     <li>Compare (disabled) </li>
 *     <li>Ambiguation</li>
 *     <li>Disambiguation</li>
 * </ul>
 * 
 * @author Ehsan Darrudi
 */

public class SemanticReasoner
{
	/** _kb used as the base for reasoning */
	private KnowledgeBase _kb;
	
	/** Determine maximum depths for reasoning */
	private int _maxReasoningDepth;
		
	/** keeps track of the current depth of reasoning. */
	private int _reasoningDepth;
	
	/** counts the number of backtracks during reasoning */
	public int totalBackTracks;
	
	/** Reasoning Constant */
	final float ABDUCTION_DEGRADATION_FACTOR = 0.3F;
	
	/** Reasoning Constant */
	final float GEN_SPEC_DEGRADATION_FACTOR	= 0.3F;
	
	/** Reasoning Constant */
	final float MIN_DEPENDENCY_INTENSITY = 0.1F;
	
	/** Retains the number of calls made during the reasoning */
	public int totalCalls;
	
	/** Specifies the maximum number of answers to be returned */ 
	private int _maxAnswersNumber = 3;
	
	/** hashtable that keeps track of visited nodes during the reasoning */
	private History _pathHistory;

	/** Used internally for conditional answers */
	private String _conditionText = "";
	
	/** Controls whether the engine should produce and log internal reasoning lines */
	private boolean _logReasoningLinesToFile = false;

	/** is used to store internal reasoning lines (only in DEBUG mode) */
	private BufferedWriter _internalReasoningLinesLogFile = null;
		
	/** is used to log HPR Engine activities */
	private BufferedWriter _logFile = null;
	
	/** Controls the level of online printing of internal reasoning lines on the standard output */
	private int _maxOnlineStdoutPrintLevel = 5;
	
	/** reasoning time in milliseconds */
	public long reasoningTime = 0;
	
	/** Keeps track of disambiguated words in regard to WordNet senses */
	private Hashtable<String, ArrayList<PlausibleAnswer>> _cacheDisambiguations = new Hashtable<String, ArrayList<PlausibleAnswer>>();
	
	private Hashtable<String, ArrayList<PlausibleStatement>> _cacheStatements = new Hashtable<String, ArrayList<PlausibleStatement>>();
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	/**
	 * constructor
	 * 
	 * @param KnowledgeBaseIn input _kb
	 * @param em execution mode; in DEBUG mode all internal reasoning lines are kept (it is slow for this reason)
	 */
	public SemanticReasoner(KnowledgeBase KnowledgeBaseIn, ExecutionMode em)
	{
		_kb = KnowledgeBaseIn;
		
		if (em == ExecutionMode.DEBUG)
			_logReasoningLinesToFile = true;
		else 
			_logReasoningLinesToFile = false;
		
		// set the initial reasoning depth. user can change it later.
		setMaxReasoningDepth(7);
	}
	
	/**
	 * sets the maximum reasoning depth which would be reached 
	 * @param MaxReasoningDepthIn
	 */
	public void setMaxReasoningDepth(int MaxReasoningDepthIn)
	{
		_maxReasoningDepth = MaxReasoningDepthIn;
	}
	
	/**
	 * Setter for _maxAnswersNumber
	 * @param maxAnswersNumber
	 */
	public void setMaximumAnswers(int maxAnswersNumber)
	{
		_maxAnswersNumber = maxAnswersNumber;
	}
	
	/**
	 * Standard entry point for the reasoning engine. 
	 * gets a plausible question and launches RECALL.
	 * 
	 * @param pq the plausible question
	 * @return the plausible answers (if any)
	 */
	public ArrayList<PlausibleAnswer> answerQuestion(PlausibleQuestion pq)
	{			
		_reasoningDepth = 0;
		totalCalls = 0;
		totalBackTracks = 0;
		reasoningTime = 0;
		_conditionText = "";
		_cacheDisambiguations.clear();
		_cacheStatements.clear();
		
		//String filename = "log/hpr/" + pq.toString().replaceAll("[/\\\n\r\t\0\f`\\?\\*<>\\|\":]", "_");
		String filename = "log/hpr/result-" + (new Long(System.currentTimeMillis())) + ".log";
		
		if (_logReasoningLinesToFile)
		{
			try
			{
				_internalReasoningLinesLogFile = new BufferedWriter(new FileWriter(filename));
			}
			catch(Exception e)
			{
				_logReasoningLinesToFile = false;
			}			
		}
		
		if (!isValidPlausibleQuestion(pq))
		{
			ArrayList<PlausibleAnswer> outs = new ArrayList<PlausibleAnswer>();
			outs.add(new PlausibleAnswer(new Node("اجزاء پرسش به پایگاه دانش نگاشت نشد لذا فرآیند استدلال اجرا نشد.")));
			
			return outs;
		}

		_pathHistory = new History();
		
		Long startTime = System.currentTimeMillis();

		if (pq.IsMultiArgument)
		{
			return MultiArgumentInference(pq);
		}
		else if (pq.descriptor == KnowledgeBase.HPR_KNOWLEDGE_DUMP && pq.argument != null)
		{
			return createArtificialAnswer(_kb.getConceptDump(pq.argument.getName(), 10), true);
		}

		// reasoning -----------------------------------
		
		ArrayList<PlausibleAnswer> Answers = recall(pq);
		
		//----------------------------------------------
		
		reasoningTime = System.currentTimeMillis() - startTime;
		
		if (!Common.isEmpty(Answers))
		{
			logToFile(pq.question);
			logToFile(pq.toString());
			
			for (PlausibleAnswer Answer: Answers)
			{
				logToFile(Answer.toString() + " " + Answer.parameters.toString());
				
				// Adjusting NEGATIVE Yes-No Answers
				if (Answer.isNegative)
				{
					if (Answer.answer == KnowledgeBase.HPR_YES)
					{
						Answer.answer = KnowledgeBase.HPR_NO;
						Answer.isNegative = false;
					}
					else if (Answer.answer == KnowledgeBase.HPR_NO)
					{
						Answer.answer = KnowledgeBase.HPR_YES;
						Answer.isNegative = false;					
					}
				}
			}
			
			logToFile("");
		}
		
		if (_logReasoningLinesToFile)
		{
			try
			{
				_internalReasoningLinesLogFile.close();
			}
			catch(Exception e)
			{
				_logReasoningLinesToFile = false;
			}
		}
		
		if (Answers == null)
		{
			Answers = new ArrayList<PlausibleAnswer>();
		}

		return Answers;
	}
	
	/**
	 * the RECALL function which is the internal entry point to the reasoning engine.		
	 * @param pq input plausible question
	 * @return a list if found answers
	 */
	private ArrayList<PlausibleAnswer> recall(PlausibleQuestion pq)
	{
		return recall(pq, null);
	}
	
	/**
	 * another version of RECALL which accepts a node as input and ignores all extracted answers of that conceptType 
	 * @param pq plausible question
	 * @param unwantedAnswer unwanted answer conceptType
	 * @return a list if found answers
	 */
	private ArrayList<PlausibleAnswer> recall(PlausibleQuestion pq, Node unwantedAnswer)
	{		
		String Function = "RECALL";

		Node descriptor = pq.descriptor;
		Node argument = pq.argument;
		Node referent = pq.referent;
		
		if (!InferencePrologue(pq, Function))
		{
			return null;
		}
		
		PlausibleAnswer answer = null;
		String statement = null;
		String reference = null;
		ArrayList<PlausibleAnswer> answers = new ArrayList<PlausibleAnswer>();

		ArrayList<PlausibleAnswer> Arguments;
		ArrayList<PlausibleAnswer> referents;
	
		// First we'll try to find the answer in the _kb directly (i.e. no reasoning) ...
		
		if (referent == null) //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		{
			// QUESTION TYPE ONE: Des(Arg)={?}

			referents = argument.findTargetNodes(descriptor);

			for (PlausibleAnswer pa: referents)
			{
				answer = pa;

				if (answer.answer == unwantedAnswer)
				{
					continue;
				}

				statement = composeStatement(pq, answer);				
				reference = composeReference(answer.statement);

				_pathHistory.pushReasoningLine(statement, answer.parameters.toString(), reference);
				answer.AddJustification(_pathHistory.getReasoningLines());
				_pathHistory.popReasoningLine(1);
				
				log("*" + composeReasoningLine(statement + "\t" + answer.parameters, Function));

				answers.add(answer);
			}
		}
		else if (argument == null) //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		{
			// QUESTION TYPE 2: Des(?)={Ref}

			Arguments = referent.findSourceNodes(descriptor);

			for (PlausibleAnswer pa: Arguments)
			{
				answer = pa;

				if (answer.answer == unwantedAnswer)
				{
					continue;
				}
				
				statement = composeStatement(pq, answer);				
				reference = composeReference(answer.statement);
				
				_pathHistory.pushReasoningLine(statement, answer.parameters.toString(), reference);
				answer.AddJustification(_pathHistory.getReasoningLines());
				_pathHistory.popReasoningLine(1);
				
				log("*" + composeReasoningLine(statement + "\t" + answer.parameters, Function));

				answers.add(answer);
			}
		}
		else if (pq.cxTime != KnowledgeBase.HPR_ANY || pq.cxLocation != KnowledgeBase.HPR_ANY) //~~~~~~~~~~~~~
		{
			// QUESTION TYPE 4: asking for CXTIME or CXLOCATION
			//
			//  				4a) Des(Arg)={Anything}:
			//											- 4a1) CX:Time = {?} 
			//											- 4a2) CX:Location = {?}
			//											- 4a3) CX:TIME = {time}?
			//											- 4a4) CX:Location = {location}?
			//					4b) Des(Anything)={Ref}:
			//											- 4b1) CX:Time = {?} 
			//											- 4b2) CX:Location = {?}
			//											- 4b3) CX:Time = {time}?
			//											- 4b4) CX:Location = {location}?
			//					4c) Des(Arg)={Ref}:
			//											- 4c1) CX:Time = {?} 
			//											- 4c2) CX:Location = {?}
			//											- 4c3) CX:TIME = {time}?
			//											- 4c4) CX:Location = {location}?
						
			answers = RecallCXs(pq, Function);
		}
		else //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		{
			// QUESTION TYPE 3: Des(Arg)={Ref}?

			// Please note that there is no difference in calling FindTargetNodes or FindSourceNodes
			// because we deal with the _kb directly here. If we wanted to do inference then it would matter.
			referents = argument.findTargetNodes(descriptor);
			
			// TODO: do the same for arguments:
			//Arguments = referent.FindSourceNodes(DESCRIPTOR);
			
			for (PlausibleAnswer pa: referents)
			{
				answer = pa;

				// It if meaningless to say: if (answer == BadAnswerNode) then ...
				// because answers here are Yes or nothing.

				if (answer.answer == referent)
				{
					PlausibleAnswer Yes = new PlausibleAnswer();

					statement = composeStatement(pq, answer);					
					reference = composeReference(answer.statement);
					
					_pathHistory.pushReasoningLine(statement, answer.parameters.toString(), reference);
					
					Yes.answer = KnowledgeBase.HPR_YES;
					Yes.copyParameters(answer.parameters);
					Yes.AddJustification(_pathHistory.getReasoningLines());
					
					_pathHistory.popReasoningLine(1);
					log("*" + composeReasoningLine(statement + "\t" + Yes.parameters, Function));

					answers.add(Yes);
				}
			}
		}
		
		
		if (_reasoningDepth >= _maxReasoningDepth)
		{
			// It's exceeded the Max. Reasoning Depth
			
			totalBackTracks++;
			
			log(composeReasoningLine("BACKTRACK (useless inference call)", Function));
			
			InferenceEpilogue(pq, Function);
			
			return answers;
		}
		
		if (answers.size() >= _maxAnswersNumber)
		{
			// we found enough answers! no need to continue.
			
			log(composeReasoningLine("BACKTRACK (found enough answers)", Function));
			
			InferenceEpilogue(pq, Function);
			
			return answers;
		}	
				
		
		// We couldn't find the answer directly. Now we have to reason to find it:

		if (referent == null)
		{
			PlausibleQuestion NewPQ = pq.clone();
			NewPQ.referent = null;

			//Do(AGEN(NewPQ), Answers);
			Do(ASPEC(NewPQ), answers);
			Do(ASIM(NewPQ), answers);
			Do(ADIS(NewPQ), answers);
			Do(ASYN(NewPQ), answers);
			
			Do(DGEN(NewPQ), answers);
			Do(DSPEC(NewPQ), answers);
			Do(DSIM(NewPQ), answers);
			Do(DDIS(NewPQ), answers);
			Do(DSYN(NewPQ), answers);
			
			Do(DDEP(NewPQ), answers);
			Do(DIMP(NewPQ), answers);
			Do(DEPA(NewPQ), answers);
			
			//Do(Abduction(NewPQ), Answers);
			//Do(RCausality(NewPQ), Answers);
			//Do(Attribute(NewPQ), Answers);
			Do(DescriptorInverseTransform(NewPQ), answers);

			Do(Ambiguation(NewPQ), answers);
			Do(Disambiguation(NewPQ), answers);
		}
		else if (argument == null)
		{
			PlausibleQuestion NewPQ = pq.clone();
			NewPQ.argument = null;
			
			//Do(RGEN(NewPQ), Answers);
			Do(RSPEC(NewPQ), answers);
			Do(RSIM(NewPQ), answers);
			Do(RDIS(NewPQ), answers);
			Do(RSYN(NewPQ), answers);
			
			Do(DGEN(NewPQ), answers);
			Do(DSPEC(NewPQ), answers);
			Do(DSIM(NewPQ), answers);
			Do(DDIS(NewPQ), answers);
			Do(DSYN(NewPQ), answers);
			
			Do(DDEP(NewPQ), answers);
			Do(DIMP(NewPQ), answers);
			
			//Do(Abduction(NewPQ), Answers);
			//Do(ACausality(NewPQ), Answers);
			//Do(Attribute(NewPQ), Answers);
			Do(DescriptorInverseTransform(NewPQ), answers);
			
			Do(Ambiguation(NewPQ), answers);
			Do(Disambiguation(NewPQ), answers);
		}
		else
		{
			//TODO: AGEN and DGEN were disabled!!!
			//Do(AGEN(pq), Answers);
			//Do(RGEN(pq), Answers);
			Do(DGEN(pq), answers);

			Do(ASPEC(pq), answers);
			Do(RSPEC(pq), answers);
			Do(DSPEC(pq), answers);

			Do(ASIM(pq), answers);
			Do(RSIM(pq), answers);
			Do(DSIM(pq), answers);

			Do(ADIS(pq), answers);
			Do(RDIS(pq), answers);
			Do(DDIS(pq), answers);
			
			Do(ASYN(pq), answers);
			Do(DSYN(pq), answers);
			Do(RSYN(pq), answers);
			
			Do(DDEP(pq), answers);
			Do(DIMP(pq), answers);
			Do(DEPA(pq), answers);

			//Do(Abduction(pq), Answers);
			//Do(RCausality(pq), Answers);
			//Do(Attribute(pq), Answers);
			Do(DescriptorInverseTransform(pq), answers);
			
			Do(Ambiguation(pq), answers);
			Do(Disambiguation(pq), answers);			
		}

		answers = combineEvidences(answers, Function, pq);
				
		InferenceEpilogue(pq, Function);
		
		log(composeReasoningLine("RETURN", Function));

		return answers;
	}
	
	/**
	 * a variation of RECALL specialized in finding contextual answers (i.e. Time and Location) answers 
	 * @param pq input plausible question
	 * @param Function the inference which called this one (it is always "RECALL")
	 * @return a list of found answers
	 */
	private ArrayList<PlausibleAnswer> RecallCXs(PlausibleQuestion pq, String Function)
	{
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();
		
		ArrayList<PlausibleAnswer> cxAnswers;
		ArrayList<PlausibleStatement> PSs = new ArrayList<PlausibleStatement>();

		Node cx;
		Node pqCX;
		
		String Question;
		
		// QUESTION TYPE 4: asking for CXTIME or CXLOCATION
		//
		//  				4a) Des(Arg)={Anything}:
		//											- 4a1) CX:Time = {?} 
		//											- 4a2) CX:Location = {?}
		//											- 4a3) CX:TIME = {time}?
		//											- 4a4) CX:Location = {location}?
		//					4b) Des(Anything)={Ref}:
		//											- 4b1) CX:Time = {?} 
		//											- 4b2) CX:Location = {?}
		//											- 4b3) CX:Time = {time}?
		//											- 4b4) CX:Location = {location}?
		//					4c) Des(Arg)={Ref}:
		//											- 4c1) CX:Time = {?} 
		//											- 4c2) CX:Location = {?}
		//											- 4c3) CX:TIME = {time}?
		//											- 4c4) CX:Location = {location}?
					
		if (pq.referent == KnowledgeBase.HPR_ANY) // 4a
		{			
			PSs = pq.argument.findOutRelations(pq.descriptor);
		}
		else if (pq.argument == KnowledgeBase.HPR_ANY) // 4b
		{
			PSs = pq.referent.findInRelations(pq.descriptor);
		}
		else // 4c
		{
			PlausibleStatement ps = pq.argument.findRelationToTarget(pq.descriptor, pq.referent);
			
			if (ps != null)
			{
				PSs.add(ps);
			}
		}
		
		if (pq.cxTime != KnowledgeBase.HPR_ANY) // 4x1, 4x3
		{
			cx = KnowledgeBase.HPR_CXTIME;
			pqCX = pq.cxTime;
		}
		else // 4x2, 4x4
		{
			cx = KnowledgeBase.HPR_CXLOCATION;
			pqCX = pq.cxLocation;
		}
						
		for (PlausibleStatement ps: PSs)
		{
			cxAnswers = ps.findTargetNodes(cx);
			
			for (PlausibleAnswer pa: cxAnswers)
			{
				if (pqCX == null) // 4x1, 4x2
				{
					pa.parameters.certainty = CXComputeCertainty(pa.parameters.certainty, ps.parameters.certainty);
					
					Question = composeStatement(pq, pa);					
					String reference = composeReference(pa.statement);

					_pathHistory.pushReasoningLine(Question, pa.parameters.toString(), reference);
					pa.AddJustification(_pathHistory.getReasoningLines());
					_pathHistory.popReasoningLine(1);
					
					log("*" + composeReasoningLine(Question + "\t" + pa.parameters, Function));

					Answers.add(pa);
					
				}
				else if (pqCX == pa.answer)// 4x3, 4x4
				{
					PlausibleAnswer Yes = new PlausibleAnswer();
					
					Question = composeStatement(pq, pa);					
					String reference = composeReference(pa.statement);
					
					_pathHistory.pushReasoningLine(Question, pa.parameters.toString(), reference);
					
					Yes.answer = KnowledgeBase.HPR_YES;
					Yes.copyParameters(pa.parameters);
					Yes.AddJustification(_pathHistory.getReasoningLines());
					
					_pathHistory.popReasoningLine(1);
					log("*" + composeReasoningLine(Question + "\t" + Yes.parameters, Function));

					Answers.add(Yes);
				}
			}
		}
		
		return Answers;
	}
	
	/**
	 * argument Generalization Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> AGEN(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.ARGUMENT, ReasoningDirection.UP);
	}
	
	/**
	 * argument Specification Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> ASPEC(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.ARGUMENT, ReasoningDirection.DOWN);  
	}
	
	/**
	 * argument Similarity Inference
	 * @param PQ question
	 * @return answer
	 */
	private ArrayList<PlausibleAnswer> ASIM(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.ARGUMENT, ReasoningDirection.SIDEWAY);
	}
	
	/**
	 * argument Dissimilarity Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> ADIS(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.ARGUMENT, ReasoningDirection.ASKANCE);
	}
	
	/**
	 * argument Synonymy Inference
	 * @param PQ question
	 * @return answer
	 */
	private ArrayList<PlausibleAnswer> ASYN(PlausibleQuestion PQ)
	{
		return Synonymy(PQ, QuestionFocus.ARGUMENT);
	}
	
	/**
	 * DESCRIPTOR Generalization Inference
	 * @param PQ question
	 * @return answers
	 */
 	private ArrayList<PlausibleAnswer> DGEN(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.DESCRIPTOR, ReasoningDirection.UP);
	}
 	
 	/**
 	 * DESCRIPTOR Specification inference
 	 * @param PQ question
 	 * @return answers
 	 */
	private ArrayList<PlausibleAnswer> DSPEC(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.DESCRIPTOR, ReasoningDirection.DOWN);  
	}
	
	/**
	 * DESCRIPTOR Similarity Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DSIM(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.DESCRIPTOR, ReasoningDirection.SIDEWAY);  
	}
	
	/**
	 * DESCRIPTOR Dissimilarity Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DDIS(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.DESCRIPTOR, ReasoningDirection.ASKANCE);  
	}
	
	/**
	 * DESCRIPTOR Synonymy Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DSYN(PlausibleQuestion PQ)
	{
		return Synonymy(PQ, QuestionFocus.DESCRIPTOR);
	}
	
	/**
	 * referent Generalization Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> RGEN(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.REFERENT, ReasoningDirection.UP);
	}
	
	/**
	 * referent Specification Inference 
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> RSPEC(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.REFERENT, ReasoningDirection.DOWN);
	}
	
	/**
	 * referent Similarity Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> RSIM(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.REFERENT, ReasoningDirection.SIDEWAY);
	}
	
	/**
	 * referent Dissimilarity Inference
	 * @param PQ question 
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> RDIS(PlausibleQuestion PQ)
	{
		return XHirerarchicalTransform(PQ, QuestionFocus.REFERENT, ReasoningDirection.ASKANCE);
	}	
	
	/**
	 * referent Synonymy Inference
	 * @param PQ question 
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> RSYN(PlausibleQuestion PQ)
	{
		return Synonymy(PQ, QuestionFocus.REFERENT);
	}

	/**
	 * performs a general hierarical inference.
	 * Almost all hirerachical inferences (generalizations, specifications, similarity and dissimilarity) use this function as base.
	 * they actually prepare the parameters and call this function
	 * @param pq question
	 * @param qf the node in the question we are working on
	 * @param Direction reasoning direction (up, down, side-way, askance)
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> XHirerarchicalTransform(PlausibleQuestion pq, QuestionFocus qf, ReasoningDirection Direction)
	{
		Node Descriptor = pq.descriptor;
		Node Argument = pq.argument;
		Node Referent = pq.referent;
		
		String hierarchicalStatement = "";
		
		Node ActiveConcept = null;
		String Function = "";

		if (qf == QuestionFocus.DESCRIPTOR)
		{
			Function = "D";
			ActiveConcept = Descriptor;
		}
		else if (qf == QuestionFocus.ARGUMENT)
		{
			Function = "A";
			ActiveConcept = Argument;
		}
		else if (qf == QuestionFocus.REFERENT)
		{
			Function = "R";
			ActiveConcept = Referent;
		}
		else
		{
			MyError.exit("Invalid Hierarchical Inference!");
		}

		switch (Direction)
		{
			case DOWN	: Function += "SPEC"; break;
			case UP		: Function += "GEN"; break;
			case SIDEWAY: Function += "SIM"; break;
			case ASKANCE: Function += "DIS"; break;
			default		: MyError.exit("Invalid Hierarchical Inference!)");
		}

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> hierarchicalAnswers = null;

		// Extracting those nodes have an ISA, INS or SIM relation to argument or vice verse.
		hierarchicalAnswers = FindHierarchicalNodes(ActiveConcept, Direction);
		
		if (hierarchicalAnswers.size() > 0)
			log(composeReasoningLine("'" + hierarchicalAnswers.size() + "' Hierarchical relations for " + ActiveConcept.getName(), Function));

		if (IsEmpty(hierarchicalAnswers))
		{
			// There isn't any hierarchical relation for ActiveConcept
			InferenceEpilogue(pq, Function);
			return null;
		}

		// Reasoning answer List:
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();

		// Temporal answer List that will contain intermediate answers.
		ArrayList<PlausibleAnswer> TempAL = null;

		Node ActiveHierarchicalNode = null;

		// We reason for each GEN relation...
		
		PlausibleAnswer reasonedAnswer = null;
		PlausibleAnswer hierarchicalAnswer = null;
		PlausibleAnswer eliteContext = null;
		PlausibleQuestion newPQ = null;
		String statement = null;
		String DEPText = "";
		String hierarchicalReference = null;
		float DependencyIntensity; 

		int Counter = 0;
		
		for (Object obj: hierarchicalAnswers)
		{
			DependencyIntensity = 1F;

			hierarchicalAnswer	= (PlausibleAnswer)obj;
			ActiveHierarchicalNode	= hierarchicalAnswer.answer;

			if (Direction == ReasoningDirection.UP)
			{
				hierarchicalStatement = ActiveConcept.getName() + " ISA " + ActiveHierarchicalNode.getName();
			}
			else if (Direction == ReasoningDirection.DOWN)
			{
				hierarchicalStatement = ActiveHierarchicalNode.getName() + " ISA " + ActiveConcept.getName();
			}
			else if (Direction == ReasoningDirection.SIDEWAY)
			{
				hierarchicalStatement = ActiveConcept.getName() + " SIM " + ActiveHierarchicalNode.getName();
			}
			else if (Direction == ReasoningDirection.ASKANCE)
			{
				hierarchicalStatement = ActiveConcept.getName() + " DIS " + ActiveHierarchicalNode.getName();
			}

			hierarchicalStatement += hierarchicalAnswer.ComposeCX();
			
			hierarchicalReference = composeReference(hierarchicalAnswer.statement);

			Counter++;
			log(composeReasoningLine(Counter + ") " + hierarchicalStatement, Function));
		
			DEPText = "";
			
			// Checking the required Context
			if (!IsEmpty(hierarchicalAnswer.contexs))
			{				
				StringBuilder sb = null;
				
				// DEPText will be set by reference
				eliteContext = FindTheMostReleventContext(hierarchicalAnswer.contexs, Descriptor, sb);
				
				DEPText = sb.toString();

				if (eliteContext == null)
				{
					log(composeReasoningLine("! Failed to verify that " + DEPText, Function));
					DependencyIntensity = MIN_DEPENDENCY_INTENSITY;
				}
				else
				{
					log(composeReasoningLine(DEPText.toString(), Function));
					DependencyIntensity = eliteContext.parameters.certainty;
				}
			}

			//---------------------------------------------
			_pathHistory.pushReasoningLine(hierarchicalStatement, hierarchicalAnswer.parameters.toString(), hierarchicalReference);

			if (DEPText != "")
			{
				//TODO: empty knowledge source
				_pathHistory.pushReasoningLine(DEPText, "[γ = " + String.format("%.2f", DependencyIntensity) + "]", "");
			}

			_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
			
			if (qf == QuestionFocus.ARGUMENT)
			{
				newPQ = pq.clone();
				newPQ.argument = ActiveHierarchicalNode;
			}
			else if (qf == QuestionFocus.DESCRIPTOR)
			{
				newPQ = pq.clone();
				newPQ.descriptor = ActiveHierarchicalNode;
			}
			else if (qf == QuestionFocus.REFERENT)
			{
				newPQ = pq.clone();
				newPQ.referent = ActiveHierarchicalNode;
			}
			
			//-------- Recall ---------
			
			TempAL = recall(newPQ);
			
			//--------------------------

			if (DEPText == "")
			{
				_pathHistory.popReasoningLine(2);
			}
			else
			{
				_pathHistory.popReasoningLine(3);
			}

			if (TempAL != null)
			{
				for (Object obj2: TempAL)
				{
					reasonedAnswer = (PlausibleAnswer)obj2;
					reasonedAnswer.parameters.certainty = AHierarchicalComputeCertainty(hierarchicalAnswer.parameters, reasonedAnswer.parameters, DependencyIntensity, Direction, Function);

					if (Direction == ReasoningDirection.ASKANCE)
					{
						reasonedAnswer.isNegative = true;
					}
					
					statement = composeStatement(pq, reasonedAnswer);
					reasonedAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth , statement, reasonedAnswer.parameters.toString());

					if (DEPText != "" && eliteContext == null)
					{
						reasonedAnswer.conditions.add(DEPText);
					}
				}

				Answers.addAll(TempAL);
			}
		}

		Answers = combineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	
	/**
	 * finds a hierarchical parent for a node 
	 * @param Concept the node at hand
	 * @param Direction hierarchical direction
	 * @return a list of hierarchical found for the node
	 */
	private ArrayList<PlausibleAnswer> FindHierarchicalNodes(Node Concept, ReasoningDirection Direction)
	{
		ArrayList<PlausibleAnswer> answers = null;
		
		ArrayList<Node> CXs = new ArrayList<Node>();
		CXs.add(KnowledgeBase.HPR_CX);
		
		if (Direction == ReasoningDirection.UP)
		{
			answers = Concept.findTargetNodes(KnowledgeBase.HPR_ISA, KnowledgeBase.HPR_SYN, CXs);
			answers.addAll(Concept.findTargetNodes(KnowledgeBase.HPR_INSTANCE));
		}
		else if (Direction == ReasoningDirection.DOWN)
		{
			answers = Concept.findSourceNodes(KnowledgeBase.HPR_ISA, KnowledgeBase.HPR_SYN, CXs);
			answers.addAll(Concept.findSourceNodes(KnowledgeBase.HPR_INSTANCE));
		}
		else if (Direction == ReasoningDirection.SIDEWAY)
		{
			answers = Concept.findSourceNodes(KnowledgeBase.HPR_SIM, KnowledgeBase.HPR_SYN, CXs);

			//TODO: marked as unnecessary, May 24 2013
			/*
			// Finding SYN relations which are equivalent to SIM in HPR			
			ArrayList<PlausibleAnswer> SYNAL;
			ArrayList<PlausibleAnswer> SecondSynsets;

			// SYN Relations doesn't have CX:
			ArrayList<PlausibleAnswer> Synsets = Concept.findTargetNodes(KnowledgeBase.HPR_SYN);
			if (!Concept.getName().startsWith("SYNSET-"))
			{
				// it is a normal node. we call the above function again to get actual synonyms.

				SYNAL = new ArrayList<PlausibleAnswer>();

				for (PlausibleAnswer SynsetAnswer: Synsets)
				{
					SecondSynsets = SynsetAnswer.answer.findTargetNodes(KnowledgeBase.HPR_SYN);
				
					for (PlausibleAnswer SecondSynsetAnswer: SecondSynsets)
					{
						if (SecondSynsetAnswer.answer != Concept)
						{
							SYNAL.add(SecondSynsetAnswer);
						}
					}
				}				
			}
			else
			{
				SYNAL = Synsets;
			}

			if (answers == null)
			{
				answers = SYNAL;
			}
			else if (SYNAL != null)
			{
				answers.addAll(SYNAL);
			}
			*/
		}
		else if (Direction == ReasoningDirection.ASKANCE)
		{
			answers = Concept.findSourceNodes(KnowledgeBase.HPR_DIS, KnowledgeBase.HPR_SYN, CXs);
		}

		return answers;
	}
	
	/**
	 * computes the intensity of a dependency relation between two nodes. used in dependency-based inferences
	 * @param Impressor first node
	 * @param Dependent second node
	 * @return the intensity in number
	 */
	private float GetDependencyIntensity(Node Impressor, Node Dependent)
	{
		if (Impressor == Dependent)
		{
			return 1F;
		}
		
		PlausibleQuestion PQ = new PlausibleQuestion();
		PQ.argument = Impressor;
		PQ.referent = Dependent;

		PlausibleAnswer Answer;
		
		PQ.descriptor = KnowledgeBase.HPR_DEP;
		Answer = GetFirstAnswer(recall(PQ));
		if (IsYes(Answer))
		{
			return Answer.parameters.certainty;
		}

		PQ.descriptor = KnowledgeBase.HPR_DEPP;
		Answer = GetFirstAnswer(recall(PQ));
		if (IsYes(Answer))
		{
			return Answer.parameters.certainty;
		}

		PQ.descriptor = KnowledgeBase.HPR_DEPN;
		Answer = GetFirstAnswer(recall(PQ));
		if (IsYes(Answer))
		{
			return Answer.parameters.certainty;
		}

		PQ.descriptor = KnowledgeBase.HPR_ISA;
		PQ.argument = Dependent;
		PQ.referent = Impressor;
		Answer = GetFirstAnswer(recall(PQ));
		if (IsYes(Answer))
		{
			return Answer.parameters.certainty;
		}

		return 0F;
	}
	
	/**
	 * finds the best context for inferences which need contextual information like Similarity and Dissimilarity	
	 * @param Contexts list of contexts
	 * @param DESCRIPTOR the node at hand
	 * @return the best context
	 */
	private PlausibleAnswer FindTheMostReleventContext(ArrayList<PlausibleAnswer> Contexts, Node Descriptor, StringBuilder DEPText)
	{
		PlausibleAnswer EliteContext = null;		
		DEPText = new StringBuilder(Descriptor + " DEPENDS ON ");
		
		float DependencyIntencity;
		float EliteDependencyIntencity = 0;

		int Counter = 0;
		
		for(PlausibleAnswer Context: Contexts)
		{
			Counter++;
			
			DependencyIntencity = GetDependencyIntensity(Context.answer, Descriptor);
			
			if (DependencyIntencity < MIN_DEPENDENCY_INTENSITY && EliteContext == null)
			{
				DEPText.append(Context.answer); 

				if (Counter < Contexts.size())
				{
					DEPText.append(" OR ");
				}
				else
				{
					DEPText.append(" (UNVERIFIED CONDITION)");
				}
			}
			else
			{
				if (EliteContext == null)
				{
					EliteContext = Context;
					EliteDependencyIntencity = DependencyIntencity;
				}
				else if (DependencyIntencity > EliteDependencyIntencity)
				{
					EliteContext = Context;
					EliteDependencyIntencity = DependencyIntencity;
				}
			}
		}

		if (EliteContext != null)
		{
			DEPText = new StringBuilder(Descriptor + " DEPENDS ON " + EliteContext.answer);
			EliteContext.parameters.certainty = EliteDependencyIntencity;
		}

		return EliteContext;
	}
	
	/**
	 * Ambiguation Inference
	 * this inference simply remove WordNet sense information from descriptor, argument 
	 * and referent and produces new plausible question to work with.
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> Ambiguation(PlausibleQuestion pq)
	{
		String Function = "AMBIGUATION";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}
		
		//-------------------
		
		ArrayList<PlausibleAnswer> answers = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> tempAnswers = new ArrayList<PlausibleAnswer>();
		
		PlausibleQuestion newPQ[] = new PlausibleQuestion[3];
		String reasoningLine[] = new String[3];
		
		Node newConcept = null;
		String newName = null;
		String statement;
		
		int pos;
		
		if (pq.descriptor != null && pq.descriptor.getName().indexOf("#") != -1)
		{
			pos = pq.descriptor.getName().indexOf("#");
			newName = pq.descriptor.getName().substring(0, pos);
			newConcept = _kb.findConcept(newName);
			
			if (newConcept != null)
			{
				newPQ[0] = pq.clone();
				newPQ[0].descriptor = newConcept;

				reasoningLine[0] = pq.descriptor.getName() + " AMBIGUATES TO " + newConcept.getName();				
				log(composeReasoningLine(pq.descriptor.getName() + " --> " + newConcept.getName(), Function));
			}
		}
		
		if (pq.argument != null && pq.argument.getName().indexOf("#") != -1)
		{
			pos = pq.argument.getName().indexOf("#");
			newName = pq.argument.getName().substring(0, pos);
			newConcept = _kb.findConcept(newName);
			
			if (newConcept != null)
			{
				newPQ[1] = pq.clone();
				newPQ[1].argument = newConcept;
				
				reasoningLine[1] = pq.argument.getName() + " AMBIGUATES TO " + newConcept.getName();				
				log(composeReasoningLine(pq.argument.getName() + " --> " + newConcept.getName(), Function));
			}
		}
		
		if (pq.referent != null && pq.referent.getName().indexOf("#") != -1)	
		{
			pos = pq.referent.getName().indexOf("#");
			newName = pq.referent.getName().substring(0, pos);
			newConcept = _kb.findConcept(newName);
			
			if (newConcept != null)
			{
				newPQ[2] = pq.clone();
				newPQ[2].referent = newConcept;
				
				reasoningLine[2] = pq.referent.getName() + " AMBIGUATES TO " + newConcept.getName();				
				log(composeReasoningLine(pq.referent.getName() + " --> " + newConcept.getName(), Function));
			}
		}
		
		for (int i = 0; i < 3; i++)
		{
			if (newPQ[i] != null)
			{
				_pathHistory.pushReasoningLine(reasoningLine[i], CertaintyParameters.defaultCertainty.toString(), "");
				_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
				
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				
				tempAnswers = recall(newPQ[i]);
				
				//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				
				_pathHistory.popReasoningLine(2);
				
				if (tempAnswers!= null && tempAnswers.size() > 0)
				{
					for (PlausibleAnswer pa: tempAnswers)
					{
						pa.parameters.certainty = SYNComputeCertainty(CertaintyParameters.defaultCertainty, pa.parameters.certainty);
						
						statement = composeStatement(pq, pa);
						pa.AdjustConclusionInJustifications(Function, _reasoningDepth , statement, pa.parameters.toString());
						
						answers.add(pa);
					}
				}
			}
		}
		
		answers = combineEvidences(answers, Function, pq);

		InferenceEpilogue(pq, Function);
		
		return answers;
	}
	
	/**
	 * Disambiguation Inference
	 * This inference disambiguates a plausible question node by adding WordNet sense information to it.
	 * at present it uses the simplest algorithm possible, i.e. it extract all relates synsets from WordNet for a word.
	 * <p>
	 * Example: if the argument of the question at hand is "park" then all available synsets are extracted:
	 * <br> - park§n-8615149: a large area of land preserved in its natural state as public property)
	 * <br> - parking_lot§n-8615638: a lot where cars are parked)
	 * <br> - park§n-3890881: a gear position that acts as a parking brake
	 * <br> ...
	 * <p> and for each synset a new plausible question is created and searched for.
	 * 
	 * @param pq question 
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> Disambiguation(PlausibleQuestion pq)
	{
		//TODO: implement or adopt a real disambiguation algorithm here. 
		// At present we use the simplest algorithm that is picking up the 
		// first sense (most used sense of the word in according to WoedNet)
		
		String Function = "DISAMBIGUATION";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}
			
		//-------------------
		
		ArrayList<PlausibleAnswer> answers = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> tempAnswers = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> synsets = new ArrayList<PlausibleAnswer>();
		
		PlausibleQuestion newPQ[] = new PlausibleQuestion[150];
		String reasoningLine[] = new String[150];
		
		String statement;
		
		int pqNum = 0;
	

		if (pq.descriptor != null && pq.descriptor.getName().indexOf("#") == -1)
		{
			// descriptors can be nouns or verbs.			
			if (pq.type == RelationType.PROPERTY)
			{
				synsets = getSensesFromLemma(pq.descriptor, POS.NOUN);
			}
			else if (pq.type == RelationType.VERB)
			{
				synsets = getSensesFromLemma(pq.descriptor, POS.VERB);
			}
			
			for (PlausibleAnswer synset: synsets)
			{
				newPQ[pqNum] = pq.clone();
				newPQ[pqNum].descriptor = synset.answer;
				newPQ[pqNum].parameters.certainty = synset.parameters.certainty;

				reasoningLine[pqNum] = pq.descriptor.getName() + " DISAMBIGUATES TO " + synset.answer.getName();				
				log(composeReasoningLine(pq.descriptor.getName() + " --> " + synset.answer.getName() + " " + synset.parameters.toString(), Function));
				
				pqNum++;
			}
		}
		
		if (pq.argument != null && pq.argument.getName().indexOf("#") == -1)
		{
			// arguments can be anything: noun, adjective, adverb, and verb;
			synsets = getSensesFromLemma(pq.argument, POS.ANY);
			
			for (PlausibleAnswer synset: synsets)
			{
				newPQ[pqNum] = pq.clone();
				newPQ[pqNum].argument = synset.answer;
				newPQ[pqNum].parameters.certainty = synset.parameters.certainty;

				reasoningLine[pqNum] = pq.argument.getName() + " DISAMBIGUATES TO " + synset.answer.getName();				
				log(composeReasoningLine(pq.argument.getName() + " --> " + synset.answer.getName() + " " + synset.parameters.toString(), Function));
				
				pqNum++;
			}
		}
		
		if (pq.referent != null && pq.referent.getName().indexOf("#") == -1)	
		{
			// referents can be nouns or sometimes adjectives. 
			// As far as I know the referent can be an adjective when the descriptor is a 
			// linking verb (be, become, feel, get, look, seem, smell, sound). 
			// Otherwise it is always a noun. 
			// below I only address to be verbs.
			
			if (pq.descriptor == KnowledgeBase.HPR_IS)
			{
				synsets = getSensesFromLemma(pq.referent, POS.ADJECTIVE);
			}
			else
			{
				synsets = getSensesFromLemma(pq.referent, POS.NOUN);
			}

			for (PlausibleAnswer synset: synsets)
			{
				newPQ[pqNum] = pq.clone();
				newPQ[pqNum].referent = synset.answer;
				newPQ[pqNum].parameters.certainty = synset.parameters.certainty;

				reasoningLine[pqNum] = pq.referent.getName() + " DISAMBIGUATES TO " + synset.answer.getName();				
				log(composeReasoningLine(pq.referent.getName() + " --> " + synset.answer.getName() + " " + synset.parameters.toString(), Function));
				
				pqNum++;
			}
		}
		
		for (int i = 0; i < pqNum; i++)
		{
			if (newPQ[i] != null)
			{
				_pathHistory.pushReasoningLine(reasoningLine[i], newPQ[i].parameters.toString(), "");
				_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
				
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				
				tempAnswers = recall(newPQ[i]);
				
				//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				
				_pathHistory.popReasoningLine(2);
				
				if (tempAnswers!= null && tempAnswers.size() > 0)
				{
					for (PlausibleAnswer pa: tempAnswers)
					{
						pa.parameters.certainty = SYNComputeCertainty(newPQ[i].parameters.certainty, pa.parameters.certainty);
						
						statement = composeStatement(pq, pa);
						pa.AdjustConclusionInJustifications(Function, _reasoningDepth , statement, pa.parameters.toString());
						
						answers.add(pa);
					}
				}
			}
		}
		
		answers = combineEvidences(answers, Function, pq);

		InferenceEpilogue(pq, Function);
		
		return answers;
	}
	
	/**
	 * performs a general Synonymy inference. Actually all introduced synonymy inferences call this function as the base.
	 * @param pq question 
	 * @param qf question focus; the node from the plausible question we are workin on 
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> Synonymy(PlausibleQuestion pq, QuestionFocus qf)
	{
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> AL = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> TempAL = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> Synsets = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> Synonyms = new ArrayList<PlausibleAnswer>();
		
		PlausibleAnswer Synset = null;
		PlausibleQuestion newPQ = null;
		
		Node ActiveConcept = null;
		
		String Function = "";

		switch (qf)
		{
			case DESCRIPTOR	: Function = "DSYN"; ActiveConcept = pq.descriptor; break;
			case ARGUMENT	: Function = "ASYN"; ActiveConcept = pq.argument; break;
			case REFERENT	: Function = "RSYN"; ActiveConcept = pq.referent; break;
			default			: MyError.exit("Invalid SYN Inference!");
		}

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		// Extracting the WordNet & Farsnet synset for the active concept:
		if (ActiveConcept.getLexicalType() == LexicalType.SENSE)
		{
			Synsets = ActiveConcept.findTargetNodes(KnowledgeBase.HPR_SYN);
			
			if (Synsets.size() > 0)
			{
				Synset = Synsets.get(0);
				AL.add(Synset);
				
				log(composeReasoningLine("Found synset: " + Synset.answer.getName(), Function));
			}
		}
		
		// extracting synonyms:
		if (Synsets.size() > 0)
		{
			Synonyms = Synset.answer.findSourceNodes(KnowledgeBase.HPR_SYN);
			
			AL.addAll(Synonyms);
			
			for (PlausibleAnswer pa: Synonyms)
			{
				log(composeReasoningLine("-" + pa.answer.getName(), Function));
			}
		}
		
		if (IsEmpty(AL))
		{
			// There aren't any synonym relations for ActiveConcept
			InferenceEpilogue(pq, Function);
			return null;
		}
		
		//~~~~~~~~~~~ Starting the Inference ~~~~~~~~~~~~
		
		String SYNRelation;
		String statement;
				
		for (PlausibleAnswer synonym: AL)
		{
			switch (qf)
			{
				case DESCRIPTOR	: newPQ = pq.clone(); newPQ.descriptor = synonym.answer; break;
				case ARGUMENT	: newPQ = pq.clone(); newPQ.argument = synonym.answer; break;
				case REFERENT	: newPQ = pq.clone(); newPQ.referent = synonym.answer; break;
			}
			
			SYNRelation = ActiveConcept.getName() + " SYN " + synonym.answer.getName();
			
			String reference = composeReference(synonym.statement);

			_pathHistory.pushReasoningLine(SYNRelation, synonym.parameters.toString(), reference);
			_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
			
			// ---------- Recall ----------
			
			TempAL = recall(newPQ);
			
			//-----------------------------
			
			_pathHistory.popReasoningLine(2);
			
			if (TempAL != null)
			{
				for (PlausibleAnswer pa: TempAL)
				{
					pa.parameters.certainty = SYNComputeCertainty(CertaintyParameters.defaultCertainty, pa.parameters.certainty);
					
					statement = composeStatement(pq, pa);
					pa.AdjustConclusionInJustifications(Function, _reasoningDepth , statement, pa.parameters.toString());
					
					Answers.add(pa);
				}
			}
		}

		Answers = combineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	
	/**
	 * Derivation from Dependency Inference
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DDEP(PlausibleQuestion pq)
	{
		String Function = "DDEP";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}
	
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();

		Do(DDEPP(pq), Answers);
		Do(DDEPN(pq), Answers);

		Answers = combineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	
	/**
	 * Derivation from POSITIVE Dependency Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DDEPP(PlausibleQuestion PQ)
	{
		return DDEPX(PQ, DependencyType.POSITIVE);
	}

	/**
	 * Derivation from NEGATIVE Dependency Inference
	 * @param PQ question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DDEPN(PlausibleQuestion PQ)
	{
		return DDEPX(PQ, DependencyType.NEGATIVE);
	}

	/**
	 * general derivation from dependency inference. Actually all introduced derivation inferences call this function as the base. 
	 * @param pq question
	 * @param dependencyType dependency conceptType
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DDEPX(PlausibleQuestion pq, DependencyType DepType)
	{
		String Function = "";
		String RelationName = "";
		String GraphicalRelationName = "";

		switch (DepType)
		{
			case POSITIVE: Function = "DDEP+"; RelationName = "DEP+"; GraphicalRelationName = "(+)->"; break;
			case NEGATIVE: Function = "DDEP-"; RelationName = "DEP-"; GraphicalRelationName = "(-)->"; break;
			default:return null;
		}

		String Question = ComposeStatement(pq);
		
		log(composeReasoningLine(Question , Function, true));

		ArrayList<PlausibleAnswer> AL = null;
		//ArrayList ALTemp = null;

		Node Relation = _kb.findConcept(RelationName);
		
		// Extracting those nodes affect the descriptor.
		AL = pq.descriptor.findSourceNodes(Relation);

		if (AL.size() > 0)
			log(composeReasoningLine("'" + AL.size() + "' " + RelationName + " dependency relations were found.", Function));

		if (IsEmpty(AL))
		{
			return null;
		}

		// Reasoning answer List:
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();

		// Temporal answer List that will contain intermediate answers.
		ArrayList<PlausibleAnswer> TempAL;
		ArrayList<PlausibleAnswer> TempAL2 = new ArrayList<PlausibleAnswer>();

		// We  reason for each DEP relation...
		PlausibleQuestion newPQ;
		PlausibleAnswer ImpressorAnswer = null;
		PlausibleAnswer ReasonedAnswer = null;
		Node ImpressorNode = null;
		Node AlteredReferent;

		Node AntonymNode;
		
		for (Object obj: AL)
		{
			ImpressorAnswer	= (PlausibleAnswer)obj;
			ImpressorNode	= ImpressorAnswer.answer;

			log(composeReasoningLine(ImpressorNode.getName() + " -- " + RelationName + " --> " + pq.descriptor.getName(), Function));

			String reference = composeReference(ImpressorAnswer.statement);

			_pathHistory.pushReasoningLine(ImpressorNode.getName() + " " + GraphicalRelationName + " " + pq.descriptor.getName(), ImpressorAnswer.parameters.toString(), reference);
			_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
			
			if (pq.referent == null)
			{
				// Question conceptType 1:

				newPQ = pq.clone();
				newPQ.descriptor = ImpressorNode;
				
				TempAL = recall(newPQ);

				if (TempAL != null)
				{
					for (Object obj2: TempAL)
					{
						ReasonedAnswer = (PlausibleAnswer)obj2;

						AntonymNode = ReasonedAnswer.answer.getAntonym();
						if (AntonymNode == null)
						{
							log(composeReasoningLine("No antonym for " + ReasonedAnswer.answer.getName() + "!", Function));
							continue;
						}
						
						if (DepType == DependencyType.POSITIVE)
						{
							//ReasonedAnswer = ReasonedAnswer;
						}
						else
						{
							ReasonedAnswer.answer = AntonymNode;
						}

						TempAL2.add(ReasonedAnswer);
					}
				}
			}
			else
			{
				// Question conceptType 2 & 3:
				// argument may or may not be null
				
				if (DepType == DependencyType.POSITIVE)
				{
					AlteredReferent = pq.referent;
				}
				else if (DepType == DependencyType.NEGATIVE)
				{
					AlteredReferent = pq.referent.getAntonym();
				}
				else
				{
					AlteredReferent = null;
				}

				if (AlteredReferent != null)
				{
					newPQ = pq.clone();
					newPQ.descriptor = ImpressorNode;
					newPQ.referent = AlteredReferent;
					
					TempAL = recall(newPQ);

					if (!IsEmpty(TempAL))
					{
						TempAL2.addAll(TempAL);
					}
				}
			}
			
			_pathHistory.popReasoningLine(2);

			if (TempAL2 != null)
			{
				for (Object obj3: TempAL2)
				{
					ReasonedAnswer = (PlausibleAnswer)obj3;
					
					ReasonedAnswer.parameters.certainty = DDEPComputeCertainty(ImpressorAnswer.parameters.certainty, ImpressorAnswer.parameters.conditionalLikelihood, ReasonedAnswer.parameters.certainty);

					ReasonedAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, ReasonedAnswer), ReasonedAnswer.parameters.toString());
				}
			}
		}
		
		// Here we may have more than an answer in ReasonigAL.
		// So we have to rank them and choose the best one.
		//Answers = CombineEvidences(TempAL2, Function, DESCRIPTOR, argument, null);
		Answers = combineEvidences(TempAL2, Function, pq);

		return Answers;
	}
	
	/**
	 * finds those nodes which affect a particular node by a DEP relation
	 * @param argument the node at hand
	 * @param dependencyType dependency conceptType 
	 * @return a list of nodes
	 */
	private ArrayList<PlausibleAnswer> FindDependencySource(Node argument, DependencyType DepType)
	{
		// Finds answers to the question '? -> argument'

		ArrayList<PlausibleAnswer> Dependencies = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> AL;

		PlausibleAnswer Answer;

		if (DepType == DependencyType.POSITIVE || DepType == DependencyType.ANY || DepType == DependencyType.MARKED)
		{
			Node DEPP = KnowledgeBase.HPR_DEPP;

			AL = argument.findSourceNodes(DEPP);

			if (AL != null)
			{
				for (Object obj: AL)
				{
					Answer = (PlausibleAnswer)obj;
	
					Answer.dependencyType = DependencyType.POSITIVE;
				}
			}

			Dependencies.addAll(AL);
		}

		if (DepType == DependencyType.NEGATIVE || DepType == DependencyType.ANY || DepType == DependencyType.MARKED)
		{
			Node DEPN = KnowledgeBase.HPR_DEPN;

			AL = argument.findSourceNodes(DEPN);

			if (AL != null)
			{
				for (Object obj: AL)
				{
					Answer = (PlausibleAnswer)obj;

					Answer.dependencyType = DependencyType.NEGATIVE;
				}
			}

			Dependencies.addAll(AL);
		}
		if (DepType == DependencyType.UNMARKED || DepType == DependencyType.ANY)
		{
			Node DEP = KnowledgeBase.HPR_DEP;

			AL = argument.findSourceNodes(DEP);

			if (AL != null)
			{
				for (Object obj: AL)
				{
					Answer = (PlausibleAnswer)obj;

					Answer.dependencyType = DependencyType.UNMARKED;
				}
			}

			Dependencies.addAll(AL);
		}

		return Dependencies;
	}

	/**
	 * Transitive Dependency Inference
	 * @param DESCRIPTOR the relation
	 * @param dependencyType dependency conceptType we are interested in
	 * @return nodes with transitive relations found
	 */
	private ArrayList<PlausibleAnswer> TDEP(Node Descriptor, DependencyType DepType)
	{
		return TDEP(Descriptor, null, DepType);
	}
	private ArrayList<PlausibleAnswer> TDEP(Node Descriptor, Node StartingConcept, DependencyType DepType)
	{
		String Function = "TDEP";
		
		PlausibleQuestion pq = new PlausibleQuestion();
		pq.descriptor = Descriptor;
		pq.argument = null;
		pq.referent = null;

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> Dependencies = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> DependenciesLevel1;
		ArrayList<PlausibleAnswer> DependenciesLevel2;
		//ArrayList NextDepthTransitivityDescriptors;
		Node NewArgument;
		PlausibleAnswer AnswerLevel1;
		PlausibleAnswer AnswerLevel2;
		Node InteriorNode;
		
		// We need all marked dependencies, either marked or unmarked. We will segregate them later.
		DependenciesLevel1 = FindDependencySource(Descriptor, DepType);

		if (IsEmpty(DependenciesLevel1))
		{
			// no dependency at all!
			log(composeReasoningLine("'0' " + " Thers is no Dependency of conceptType '" + DepType + "' for '" + Descriptor.getName() + "'.", Function));

			InferenceEpilogue(pq, Function);
			return null;
		}

		for (Object obj: DependenciesLevel1)
		{	
			AnswerLevel1 = (PlausibleAnswer)obj;
			NewArgument = AnswerLevel1.answer;

			if (NewArgument == StartingConcept)
			{
				continue;
			}
			
			DependenciesLevel2 = FindDependencySource(NewArgument, DepType);

			if (IsEmpty(DependenciesLevel2))
			{
				continue;
			}

			for (Object obj2: DependenciesLevel2)
			{
				AnswerLevel2 = (PlausibleAnswer)obj2;
				InteriorNode = AnswerLevel2.answer;

				// To remedy bidirectional DEP relations
				if (InteriorNode == Descriptor)
				{
					continue;	
				}

				AnswerLevel2.dependencyType = TDEPCombineDependencyTypes(AnswerLevel1.dependencyType, AnswerLevel2.dependencyType);
				AnswerLevel2.parameters = TDEPCombineParameters(AnswerLevel1.parameters, AnswerLevel2.parameters);

				log(composeReasoningLine(AnswerLevel2.answer.getName() + " -- DEP(via " + AnswerLevel1.answer.getName() + ") --> " + Descriptor.getName() + "\t" + AnswerLevel2.parameters, Function));

				Dependencies.add(AnswerLevel2);

				// TODO: Should I look for next level transitivity dependencies?
//				NextDepthTransitivityDescriptors = TDEP(AnswerLevel1.Answer, DESCRIPTOR, DependencyType.ANY);
//
//				if (NextDepthTransitivityDescriptors != null)
//				{
//					for (PlausibleAnswer PA in NextDepthTransitivityDescriptors)
//					{
//						PA.Parameters = TDEPCombineParameters(AnswerLevel1.Parameters, PA.Parameters);
//
//						Dependencies.addAll(NextDepthTransitivityDescriptors);
//					}
//				}
			}
		}

		if (IsEmpty(Dependencies))
		{
			//log(ComposeMessage("There isn't any Transitive dependency for '" + DESCRIPTOR.getName() + "'.", Function));
		}

		InferenceEpilogue(pq, Function);
		
		return Dependencies;
	}
	private ArrayList<PlausibleAnswer> TDEP(Node Descriptor, Node Relation)
	{
		Node dummy = null;
		return TDEP(Descriptor, Relation, dummy);
	}
	private ArrayList<PlausibleAnswer> TDEP(Node Descriptor, Node Relation, Node StartingConcept)
	{
		String Function = "TDEP";
		
		PlausibleQuestion pq = new PlausibleQuestion();
		pq.descriptor = Descriptor;
		pq.argument = null;
		pq.referent = null;

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> TransitiveNodes = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> TransitiveRelationsLevel1;
		ArrayList<PlausibleAnswer> TransitiveRelationsLevel2;
		ArrayList<PlausibleAnswer> NestedTransitiveNodes;

		// We need all marked dependencies, either marked or unmarked. We will segregate them later.
		TransitiveRelationsLevel1 = Descriptor.findSourceNodes(Relation, KnowledgeBase.HPR_SYN);

		if (IsEmpty(TransitiveRelationsLevel1))
		{
			// no dependency at all!
			log(composeReasoningLine("Thers is no transitive '" + Relation.getName() + "' relations for '" + Descriptor.getName() + "'.", Function));

			InferenceEpilogue(pq, Function);
			return null;
		}

		for (PlausibleAnswer TransitiveRelationLevel1: TransitiveRelationsLevel1)
		{
			if (TransitiveRelationLevel1.answer == StartingConcept)
			{
				continue;
			}

			TransitiveRelationsLevel2 = TransitiveRelationLevel1.answer.findSourceNodes(Relation, KnowledgeBase.HPR_SYN);

			if (!IsEmpty(TransitiveRelationsLevel2))
			{
				for (PlausibleAnswer TransitiveRelationLevel2: TransitiveRelationsLevel2)
				{
					if (TransitiveRelationLevel2.answer == Descriptor)
					{
						continue;
					}
					
					TransitiveRelationLevel2.parameters = TDEPCombineParameters(TransitiveRelationLevel1.parameters, TransitiveRelationLevel2.parameters);
					
					TransitiveNodes.add(TransitiveRelationLevel2);

					NestedTransitiveNodes = TDEP(TransitiveRelationLevel1.answer, Relation, Descriptor);

					if (!IsEmpty(NestedTransitiveNodes))
					{
						for (PlausibleAnswer NestedTransitiveNode: NestedTransitiveNodes)
						{
							NestedTransitiveNode.parameters = TDEPCombineParameters(TransitiveRelationLevel1.parameters, NestedTransitiveNode.parameters);

							TransitiveNodes.add(NestedTransitiveNode);
						}
					}
				}
			}
		}

		if (IsEmpty(TransitiveNodes))
		{
			log(composeReasoningLine("There isn't any Transitive dependency for '" + Descriptor.getName() + "'.", Function));
		}

		InferenceEpilogue(pq, Function);
		
		return TransitiveNodes;	
	}
	
	/**
	 * Dependency-based Analogy Inference
	 * this inference is not part of the original HPR theory and I found it in a follow-up paper by one of the 
	 * Michalsik's students. I found it useful (though sometimes tricky even for humans to follow and understand it!) 
	 * it is an sketch:
	 * <br>	1: Climate(Surrey) = {?}
	 * <br> 2: Climate <--> ?
	 * <br> 3: Climate <--> Latitude 
	 * <br> 4: Latitude(Surrey) = {?}
	 * <br> 5: Latitude(Surrey) = {>40-degrees}
	 * <br> 6: Latitude(?)	= {>40-degrees}
	 * <br> 7: Latitude(Holland) = {>40-degrees}
	 * <br> 8: Climate(Holland) = {Temperate}
	 * <br> -------------------------------------
	 * <br> :: Climate(Surrey) = {Temperate}
	 * 
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DEPA(PlausibleQuestion pq)
	{
		Node Descriptor = pq.descriptor;
		Node Argument = pq.argument;
		Node Referent = pq.referent;
		
		if (Argument == null)
		{
			// Notice: This inference hasn't a referent-based conunterpart; that is,
			// you may ask Climate(Surrey) = {?} and the answer will be 'Temprate'. 
			// Nonetheless if you ask Climate(?) = {Temperate} the answer includes just 'Holland'.
			// I didn't implement the the referent-based counterpart because it would be in vein because of
			// the present restriction that each inference have to return just one answer. Thus, for the above
			// question the answer will be always Holland. Thus:

			return null;
		}


		String Function = "DEPA";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> AL;
		ArrayList<PlausibleAnswer> ImpressorsList;
		ArrayList<PlausibleAnswer> TDEPImpressorsList;

		ImpressorsList = FindDependencySource(Descriptor, DependencyType.ANY);

		if (IsEmpty(ImpressorsList))
		{
			//log(ComposeMessage("There isn't any direct dependency relation for '" + DESCRIPTOR.getName() + "'", Function));
			
			InferenceEpilogue(pq, Function);
			return null;
		}
		
		log(composeReasoningLine("'" + ImpressorsList.size() + "' direct impressors were found.", Function));
		
		int Counter = 0;
		for (PlausibleAnswer Impressor: ImpressorsList)
		{
			Counter++;
			log(composeReasoningLine(Counter + ") " + Impressor.answer.getName() , Function));
		}
		
		log(composeReasoningLine("Finding indirect impressors using the TDEP inference..." , Function));
		// Finding indirect impressors using the TDEP inference:
		TDEPImpressorsList = TDEP(Descriptor, DependencyType.ANY);

		if  (TDEPImpressorsList == null)
		{
			TDEPImpressorsList = new ArrayList<PlausibleAnswer>();
		}
		
		log(composeReasoningLine("'" + TDEPImpressorsList.size() + "' indirect impressors were found.", Function));			
		
		ImpressorsList.addAll(TDEPImpressorsList);
		
		AL = DEPBasedAnalogyWithSpecificImpressors(ImpressorsList, pq);

		if (Argument != null && Referent != null)
		{
			// Question conceptType 3:

			AL = AdjustYesNoAnswers(AL, Referent);
		}

		AL = combineEvidences(AL, Function, pq);

		InferenceEpilogue(pq, Function);

		return AL;
	}

	/**
	 * a specification of Dependency-based Analogy Inference discussed above
	 * @param ImpressorsList the list of impresser nodes (nodes which impress (inverse of depend) other nodes) we are interested in. 
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DEPBasedAnalogyWithSpecificImpressors(ArrayList<PlausibleAnswer> ImpressorsList, PlausibleQuestion pq)
	{
		/*
			1: Climate(Surrey) = {?}	(Original Question)
			2: Climate <-- ?
			3: Climate <-- Latitude
			4: Latitude(Surrey) = {?}
			5: Latitude(Surrey) = {>40-degrees}
			6: Latitude(?) = {>40-degrees}
			7: Latitude(Holland) = {>40-degrees}
			8: Climate(Holland) = {?}
			9: Climate(Holland) = {Temperate}
			10: Climate(Surrey) = {Temperate}
		*/
		
		Node Descriptor = pq.descriptor;
		Node Argument = pq.argument;

		String Function = "DEPA";

		ArrayList<PlausibleAnswer> AL = new ArrayList<PlausibleAnswer>();

		PlausibleAnswer Impressor;
		PlausibleAnswer PrimaryReferent;
		PlausibleAnswer SecondaryArgument;
		PlausibleAnswer SecondaryReferent; 
		
		PlausibleQuestion newPQ;

		Node ImpressorNode;
		Node PrimaryReferentNode;
		Node SecondaryArgumentNode;
		//Node SecondaryReferentNode;
		
		ArrayList<PlausibleAnswer> PrimaryReferentsList;
		ArrayList<PlausibleAnswer> SecondaryArgumentsList;
		ArrayList<PlausibleAnswer> SecondaryReferentsList;

		int ImpressorCounter = 0;

		// 2:
		for (Object obj: ImpressorsList)
		{
			ImpressorCounter++;

			// 3:
			Impressor = (PlausibleAnswer)obj;
			ImpressorNode = Impressor.answer;

			log(composeReasoningLine(ImpressorCounter + ") " + ImpressorNode.getName() + " -- DEPX --> " + Descriptor.getName(), Function));
			
			// 4:
			newPQ = pq.clone();
			newPQ.descriptor = ImpressorNode;
			newPQ.referent = null;
			
			PrimaryReferentsList = recall(newPQ);

			if (IsEmpty(PrimaryReferentsList))
			{
				log(composeReasoningLine("No primary referents for rule " + ImpressorCounter, Function));
				continue;
			}

			for (Object obj2: PrimaryReferentsList)
			{
				// 5:
				PrimaryReferent = (PlausibleAnswer)obj2;
				PrimaryReferentNode = PrimaryReferent.answer;

				// 6:
				newPQ = pq.clone();
				newPQ.descriptor = ImpressorNode;
				newPQ.argument = null;
				newPQ.referent = PrimaryReferentNode;
				
				SecondaryArgumentsList = recall(newPQ, Argument);

				if (IsEmpty(SecondaryArgumentsList))
				{
					log(composeReasoningLine("No secondary arguments for rule " + ImpressorCounter, Function));
					continue;
				}

				for (Object obj3: SecondaryArgumentsList)
				{
					// 7:
					SecondaryArgument = (PlausibleAnswer)obj3;
					SecondaryArgumentNode = SecondaryArgument.answer;

					// TODO: I should change FindArguments as it doesn't return again FirstArgument as a SecondArgument.
					// Then I can remove the following line
					if (SecondaryArgumentNode == Argument)
					{
						log(composeReasoningLine("Secondary arguments mismatch for rule " + ImpressorCounter, Function));
						continue;
					}

					if (SecondaryArgument.isNegative)
					{
						log(composeReasoningLine("Inappropriate secondary argument " + ImpressorCounter, Function));
						continue;
					}


					// (3), (5), (7), (8)

					String referenceImpressor = composeReference(Impressor.statement);
					String referencePrimaryReferent = composeReference(PrimaryReferent.statement);
					String referenceSecondaryArgument = composeReference(SecondaryArgument.statement);
					
					_pathHistory.pushReasoningLine(ImpressorNode.getName() + " -- DEPX --> " + Descriptor.getName(), Impressor.parameters.toString(), referenceImpressor);
					_pathHistory.pushReasoningLine(ComposePlausibleQuestion(ImpressorNode, Argument, PrimaryReferentNode, PrimaryReferent.isNegative), PrimaryReferent.parameters.toString(), referencePrimaryReferent);
					_pathHistory.pushReasoningLine(ComposePlausibleQuestion(ImpressorNode, SecondaryArgumentNode, PrimaryReferentNode, SecondaryArgument.isNegative), SecondaryArgument.parameters.toString(), referenceSecondaryArgument);
					_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*DEPA(" + _reasoningDepth + ")", "", "");
					
					newPQ = pq.clone();
					newPQ.argument = SecondaryArgumentNode;
					newPQ.referent = null;
					
					SecondaryReferentsList = recall(newPQ);
					
					_pathHistory.popReasoningLine(4);

					if (IsEmpty(SecondaryReferentsList))
					{
						log(composeReasoningLine("No secondary referents for impressor number " + ImpressorCounter, Function));
						continue;
					}

					for (Object obj4: SecondaryReferentsList)
					{
						// 9:
						SecondaryReferent = (PlausibleAnswer)obj4;
						//SecondaryReferentNode = SecondaryReferent.Answer;

						// The reasoning line has been filled out before

						// 10:
						// finally we've got the answer!
						SecondaryReferent.parameters.certainty = DEPAComputeCertainty(Impressor.parameters, PrimaryReferent.parameters, SecondaryArgument.parameters, SecondaryReferent.parameters);

						SecondaryReferent.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, SecondaryReferent), SecondaryReferent.parameters.toString());

						AL.add(SecondaryReferent);
					}
				}
			}
		}
	
		return AL;
	}
	
	/**
	 * Derivation from Implication Inference
	 * does both DIMPFull & DIMPPartial sub inferences.
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DIMP(PlausibleQuestion pq)
	{
		String Function = "DIMP";
		
		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> Answers1 = DIMPFull(pq);
		ArrayList<PlausibleAnswer> Answers2 = DIMPPartial(pq);

		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();

		if (Answers1 != null)
		{
			Answers.addAll(Answers1);
		}
		if (Answers2 != null)
		{
			Answers.addAll(Answers2);
		}

		Answers = combineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	
	/**
	 * the original DIMP inference explained in the HPR theory.
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DIMPFull(PlausibleQuestion pq)
	{
		Node Descriptor = pq.descriptor;
		Node Argument = pq.argument;
		Node Referent = pq.referent;
		
		String Function = "DIMP";
				
		Node IMP = KnowledgeBase.HPR_IMP;
		
		ArrayList<PlausibleAnswer> AL = new ArrayList<PlausibleAnswer>();
		PlausibleAnswer FinalAnswer;
		PlausibleAnswer KindOfRelation;
        			
		ArrayList<PlausibleStatement> ConsequentStatements;
		ArrayList<?> ImplicationStatements;

		PlausibleStatement ConsequentStatement;
		PlausibleStatement ImplicationStatement;
		PlausibleStatement AntecedentStatement;
		
		PlausibleQuestion newPQ;
		
		Node AntecedentArgument;
		Node AntecedentReferent;
		Node AntecedentDescriptor;			
		Node ConsequentArgument;
		Node ConsequentReferent;
		Node ConsequentDescriptor;

		ArrayList<PlausibleAnswer> Answers;
		
		int RuleCount = 0;
		String RuleText;
		String ISAText;
		String statement;
		
		// Retrieving all consequent statements having DESCRIPTOR in their relationType:
		ConsequentStatements = getStatements(Descriptor, ConditionalType.CONSEQUENT);

		if (ConsequentStatements.size() > 0)
			log(composeReasoningLine("'" + ConsequentStatements.size() + "' Rule(s) were found for " + Descriptor.getName(), Function));
		
		for (Object obj: ConsequentStatements)
		{
			RuleCount++;
			
			ConsequentStatement = (PlausibleStatement)obj;
			ConsequentDescriptor = ConsequentStatement.relationType;
			ConsequentArgument = ConsequentStatement.argument;
			ConsequentReferent = ConsequentStatement.referent;
			
			// Now we can test Implications:
			
			ImplicationStatements = ConsequentStatement.findSourceNodes(IMP);

			for (Object obj2: ImplicationStatements)
			{
				ImplicationStatement = (PlausibleStatement)((PlausibleAnswer)obj2).statement;
				AntecedentStatement = (PlausibleStatement)((PlausibleAnswer)obj2).answer;

				AntecedentArgument = ConsequentArgument;
				AntecedentDescriptor = AntecedentStatement.relationType;
				AntecedentReferent = AntecedentStatement.referent;

				RuleText =	"*IF* " + 
							ComposePlausibleQuestion(AntecedentDescriptor, AntecedentArgument, AntecedentReferent, false) +
							" *THEN* " + 
							ComposePlausibleQuestion(ConsequentDescriptor, ConsequentArgument, ConsequentReferent, false);
				
				log(composeReasoningLine("Rule '" + RuleCount + "': " + RuleText + "\t" + ImplicationStatement.parameters, Function));

				String reference = composeReference(ImplicationStatement);
				
				_pathHistory.pushReasoningLine(RuleText, ImplicationStatement.parameters.toString(), reference);
				_pathHistory.pushReasoningLine("*ISA RELATION GOES HERE*" + _reasoningDepth, "", "");
				_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*DIMP(" + _reasoningDepth + ")", "", "");
				
				newPQ = pq.clone();
				newPQ.descriptor = AntecedentDescriptor;
				newPQ.referent = AntecedentReferent;
				
				Answers = recall(newPQ);
				
				_pathHistory.popReasoningLine(3);
				
				if (IsEmpty(Answers))
				{
					continue;
				}

				for (Object obj3: Answers)
				{
					FinalAnswer = (PlausibleAnswer)obj3;
					
					ISAText = "";
					
					// To fill in ISAText
					if (Argument == null) //---------------------------------------
					{
						// Question conceptType 2:
						KindOfRelation = IsAKindOf(FinalAnswer.answer, ConsequentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + ConsequentArgument.getName(), Function));
							continue;
						}

						ISAText = FinalAnswer.answer.getName() + " ISA " + ConsequentArgument+ " : " +  KindOfRelation.parameters;

						if (Referent != ConsequentReferent)
						{
							log(composeReasoningLine("Unsatisfied consequent in the rule.", Function));
							continue;								
						}

						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = IMPComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}
					else if (Referent == null) //----------------------------------
					{
						// Question conceptType 1:
						KindOfRelation = IsAKindOf(Argument, ConsequentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + ConsequentArgument.getName(), Function));
							continue;
						}

						ISAText = Argument.getName() + " ISA " + ConsequentArgument + " : " +  KindOfRelation.parameters;

						FinalAnswer.answer = ConsequentReferent;
						
						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = IMPComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}
					else //--------------------------------------------------------
					{
						// Question Type3:
						KindOfRelation = IsAKindOf(Argument, ConsequentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + ConsequentArgument.getName(), Function));
							continue;
						}

						ISAText = Argument.getName() + " ISA " + ConsequentArgument + " : " +  KindOfRelation.parameters;

						FinalAnswer.answer = FinalAnswer.answer;
						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = IMPComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}

					AL.add(FinalAnswer);
				}
			}
		}

		return AL;
	}

	/**
	 * a relaxed form of DIMP inference (see above) which needs only descriptors and full statements as antecedents
	 * this inference has been added to make use of WordNet CAUSES and ENTAILS relations.
	 * @param pq question
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DIMPPartial(PlausibleQuestion pq)
	{
		Node Descriptor = pq.descriptor;

		PlausibleQuestion newPQ;
		
		String Function = "DIMP";

		ArrayList<PlausibleAnswer> PartialIMPAnswers = Descriptor.findSourceNodes(KnowledgeBase.HPR_IMP, KnowledgeBase.HPR_SYN);

		if (IsEmpty(PartialIMPAnswers))
		{
			//log(ComposeMessage("No 'IMP' Relations for " + DESCRIPTOR.getName(), Function));
			return null;
		}
		else
		{
			log(composeReasoningLine("'" + PartialIMPAnswers.size() + "' direct 'IMP' Relations for " + Descriptor.getName(), Function));
		}

		ArrayList<PlausibleAnswer> TransitivePartialIMPAnswers = TDEP(Descriptor, KnowledgeBase.HPR_IMP);

		if (IsEmpty(TransitivePartialIMPAnswers))
		{
			log(composeReasoningLine("No transitive 'IMP' Relations for " + Descriptor.getName(), Function));
		}
		else
		{
			log(composeReasoningLine("'" + TransitivePartialIMPAnswers.size() + "' indirect 'IMP' Relations for " + Descriptor.getName(), Function));

			PartialIMPAnswers.addAll(TransitivePartialIMPAnswers);
		}

		ArrayList<PlausibleAnswer> FinalAnswers = new ArrayList<PlausibleAnswer>();
		ArrayList<PlausibleAnswer> Answers;

		PlausibleAnswer Answer;
		int PartialIMPCounter = 0;
		String PartialIMPRelation;
		
		for (PlausibleAnswer PartialIMPAnswer: PartialIMPAnswers)
		{
			PartialIMPCounter++;

			PartialIMPRelation = PartialIMPAnswer.answer.getName() + " IMPLIES " + Descriptor.getName();
			log(composeReasoningLine(PartialIMPCounter + ") " + PartialIMPRelation, Function));

			String reference = composeReference(PartialIMPAnswer.statement);
			
			_pathHistory.pushReasoningLine(PartialIMPRelation, PartialIMPAnswer.parameters.toString(), reference);
			_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
			
			newPQ = pq.clone();
			newPQ.descriptor = PartialIMPAnswer.answer;
			
			Answers = recall(newPQ);
			
			_pathHistory.popReasoningLine(2);

			if (!IsEmpty(Answers))
			{
				for (PlausibleAnswer ReasonedAnswer: Answers)
				{
					Answer = ReasonedAnswer;
					Answer.parameters.certainty = IMPComputeCertainty(PartialIMPAnswer.parameters.certainty, PartialIMPAnswer.parameters.conditionalLikelihood, Answer.parameters.certainty);

					Answer.AdjustConclusionInJustifications(Function, _reasoningDepth , composeStatement(pq, Answer), Answer.parameters.toString());

					FinalAnswers.add(Answer);
				}
			}
		}

		return FinalAnswers;
	}
	
	/**
	 * Abduction Inference
	 * This one performs a so-called abductive inference. it is a weak kind of inference!
	 * @param pq question
	 * @return answers
	 */

	private ArrayList Abduction(PlausibleQuestion pq)
	{
		Node DESCRIPTOR = pq.descriptor;
		Node argument = pq.argument;
		Node referent = pq.referent;

		String Function = "ABDUCTION";
		
		if (!InferencePrologue(pq, Function))
		{
			return null;
		}
		
		Node IMP = KnowledgeBase.HPR_IMP;
		
		ArrayList AL = new ArrayList();
		PlausibleAnswer FinalAnswer;
		PlausibleAnswer KindOfRelation;
		
		PlausibleQuestion newPQ;
        			
		ArrayList AntecedentStatements;
		ArrayList ImplicationStatements;

		PlausibleStatement AntecedentStatement;
		PlausibleStatement ImplicationStatement;
		PlausibleStatement ConsequentStatement;
		
		Node ConsequentArgument;
		Node ConsequentReferent;
		Node ConsequentDescriptor;			
		Node AntecedentArgument;
		Node AntecedentReferent;
		Node AntecedentDescriptor;

		ArrayList Answers;
		
		int RuleCount = 0;
		String RuleText;
		String ISAText;
		String statement;
		
		// Retrieving all ANTECEDENT statements having DESCRIPTOR in their relationType:
		AntecedentStatements = getStatements(DESCRIPTOR, ConditionalType.ANTECEDENT);

		log(composeReasoningLine("'" + AntecedentStatements.size() + "' Rule(s) were found for " + DESCRIPTOR.getName(), Function));
		
		for (Object obj: AntecedentStatements)
		{
			RuleCount++;
			
			AntecedentStatement = (PlausibleStatement)obj;
			AntecedentDescriptor = AntecedentStatement.relationType;
			AntecedentArgument = AntecedentStatement.argument;
			AntecedentReferent = AntecedentStatement.referent;
			
			// Now we can test Implications:
			
			ImplicationStatements = AntecedentStatement.findTargetNodes(IMP);

			for (Object obj2: ImplicationStatements)
			{
				ImplicationStatement = (PlausibleStatement)((PlausibleAnswer)obj2).statement;
				ConsequentStatement = (PlausibleStatement)((PlausibleAnswer)obj2).answer;

				ConsequentArgument = AntecedentArgument;
				ConsequentDescriptor = ConsequentStatement.relationType;
				ConsequentReferent = ConsequentStatement.referent;

				RuleText =	"*IF* " + 
					ComposePlausibleQuestion(AntecedentDescriptor, AntecedentArgument, AntecedentReferent, false) +
					" *THEN* " + 
					ComposePlausibleQuestion(ConsequentDescriptor, ConsequentArgument, ConsequentReferent, false);
				
				log(composeReasoningLine("Rule '" + RuleCount + "': " + RuleText + "\t" + ImplicationStatement.parameters, Function));
				
				String reference = composeReference(ImplicationStatement);
				
				_pathHistory.pushReasoningLine(RuleText, ImplicationStatement.parameters.toString(), reference);
				_pathHistory.pushReasoningLine("*ISA RELATION GOES HERE*" + _reasoningDepth, "", "");
				_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
				
				newPQ = pq.clone();
				newPQ.descriptor = ConsequentDescriptor;
				newPQ.referent = ConsequentReferent;
				
				Answers = recall(newPQ);
				
				if (IsEmpty(Answers))
				{
					continue;
				}

				_pathHistory.popReasoningLine(3);

				for (Object obj3: Answers)
				{
					FinalAnswer = (PlausibleAnswer)obj3;
					
					ISAText = "";
					
					// To fill in ISAText
					if (argument == null) //---------------------------------------
					{
						// Question conceptType 2:
						KindOfRelation = IsAKindOf(FinalAnswer.answer, AntecedentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + AntecedentArgument.getName(), Function));
							continue;
						}

						ISAText = FinalAnswer.answer.getName() + " ISA " + AntecedentArgument+ " : " +  KindOfRelation.parameters;

						if (referent != AntecedentReferent)
						{
							log(composeReasoningLine("Unsatisfied CONSEQUENT in the rule.", Function));
							continue;								
						}

						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = AbductionComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}
					else if (referent == null) //----------------------------------
					{
						// Question conceptType 1:
						KindOfRelation = IsAKindOf(argument, AntecedentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + AntecedentArgument.getName(), Function));
							continue;
						}

						ISAText = argument.getName() + " ISA " + AntecedentArgument + " : " +  KindOfRelation.parameters;

						FinalAnswer.answer = AntecedentReferent;
						
						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = AbductionComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}
					else //--------------------------------------------------------
					{
						// Question Type3:
						KindOfRelation = IsAKindOf(argument, AntecedentArgument);
						if (KindOfRelation == null)
						{
							log(composeReasoningLine(FinalAnswer.answer.getName() + " IS NOT A " + AntecedentArgument.getName(), Function));
							continue;
						}

						ISAText = argument.getName() + " ISA " + AntecedentArgument + " : " +  KindOfRelation.parameters;

						FinalAnswer.answer = FinalAnswer.answer;
						FinalAnswer.copyParameters(FinalAnswer.parameters);
						FinalAnswer.parameters.certainty = AbductionComputeCertainty(ImplicationStatement.parameters, KindOfRelation.parameters.certainty, FinalAnswer.parameters.certainty);
						statement = composeStatement(pq, FinalAnswer);
						FinalAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth, statement, FinalAnswer.parameters.toString(), "*ISA RELATION GOES HERE*" + _reasoningDepth, ISAText);
					}

					AL.add(FinalAnswer);
				}
			}
		}

		AL = combineEvidences(AL, Function, pq);

		InferenceEpilogue(pq, Function);

		return AL;
	}

	
	/**
	 * simple checks if two nodes have a kind-of relation (transitive)
	 * This function is redundant. It is equivalent to RECALL(ISA, Child, WantedParent). 
	 * However it is much faster because it concentrates on ISA relations which bear actually to the task.
	 * 
	 * @param Child child
	 * @param WantedParent parent
	 * @return null if the relation doesn't hold, otherwise it is not.
	 */
	private PlausibleAnswer IsAKindOf(Node Child, Node WantedParent)
	{
		_reasoningDepth++;

		String Question = "ISA(" + Child.getName() + ")={" + WantedParent.getName() + "}?";			
		log(composeReasoningLine(Question, "ISKIND", true));

		if (Child == WantedParent)
		{
			PlausibleAnswer PA = new PlausibleAnswer();
			PA.answer = Child;
			PA.parameters.certainty = CertaintyParameters.defaultCertainty;

			log(composeReasoningLine(Question + "\tYes\t" + PA.parameters, "ISKIND")); 
			_reasoningDepth--;
			return PA;
		}
		
		// Extracting those nodes which have an ISA (or INS) relation to Child
		ArrayList<PlausibleAnswer> ParentAnswers = FindHierarchicalNodes(Child, ReasoningDirection.UP);
		PlausibleAnswer ParentAnswer;
		PlausibleAnswer GrandFatherAnswer;

		for (Object obj: ParentAnswers)
		{
			ParentAnswer = (PlausibleAnswer)obj;
			
			if (ParentAnswer.answer == WantedParent)
			{
				// Parameters have been initialized before in FindHierarchicalNodes-FindTargetNodes
				log(composeReasoningLine(Question + "\tYes\t" + ParentAnswer.parameters, "ISKIND")); 
				_reasoningDepth--;
				return ParentAnswer;	
			}
			else
			{
				GrandFatherAnswer = IsAKindOf(ParentAnswer.answer, WantedParent);
				
				if (GrandFatherAnswer == null)
				{
					_reasoningDepth--;
					log(composeReasoningLine(Question + "\tNo", "ISKIND")); 
					return null;
				}
				
				GrandFatherAnswer.parameters.certainty = GrandFatherAnswer.parameters.certainty*ParentAnswer.parameters.certainty;
				GrandFatherAnswer.parameters.dominance = GrandFatherAnswer.parameters.dominance*ParentAnswer.parameters.dominance;
						
				log(composeReasoningLine(Question + "\tYes\t" + GrandFatherAnswer.parameters, "ISKIND")); 
				_reasoningDepth--;
				return GrandFatherAnswer;
			}
		}
	
		log(composeReasoningLine(Question + "\tNo", "ISKIND")); 
		_reasoningDepth--;
		return null;
	}

	/**
	 * extract all statements of a relation conceptType
	 * @param relationType the relation conceptType 
	 * @param RequestedStatType conceptType of the statements needed
	 * @return list of statements
	 */
	private ArrayList<PlausibleStatement> getStatements(Node relationType, ConditionalType requestedStatType)
	{
		String key = relationType.getName() + "-" + requestedStatType.toString();
		
		ArrayList<PlausibleStatement> statements = _cacheStatements.get(key);
		
		if (statements != null)
			return statements;
		
		String StatName = "*" + relationType.getName() + " (";
		
		statements = new ArrayList<PlausibleStatement>();
		
		int counter = 1;

		PlausibleStatement statement = (PlausibleStatement)_kb.findConcept(StatName + counter + ")");

		while (statement != null)
		{	
			if (requestedStatType == ConditionalType.CONSEQUENT && statement.IsConsequentStatement())
			{
				statements.add(statement);
			}
			else if (requestedStatType == ConditionalType.ANTECEDENT && statement.IsAntecedentStatement())
			{
				statements.add(statement);
			}
			else if (requestedStatType == ConditionalType.NOT_CONDITIONAL && statement.conditionalType == ConditionalType.NOT_CONDITIONAL)
			{
				statements.add(statement);
			}

			counter++;
			statement = (PlausibleStatement)_kb.findConcept(StatName + counter + ")");
		}
		
		_cacheStatements.put(key, statements);

		return statements;
	}
	
	/**
	 * Causality Inference for Referents
	 * @param pq question
	 * @return answers
	 */
	/*
	private ArrayList RCausality(PlausibleQuestion pq)
	{
		Node descriptor = pq.Descriptor;
		Node argument = pq.Argument;
		Node referent = pq.Referent;


		String Function = "CAUSINF";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		if (referent != null && referent != KnowledgeBase.HPR_YES)
		{
			log(ComposeMessage("Bad referent", Function));
			
			InferenceEpilogue(pq, Function);
			return null;
		}

		if (descriptor.pos != POS.VERB)
		{
			log(ComposeMessage("Descriptor is not a verb!", Function));
			
			InferenceEpilogue(pq, Function);
			return null;			
		}

		// Finding all nodes having a CAUSES relation to the descriptor:
		ArrayList<PlausibleAnswer> CauseAnswers = descriptor.findSourceNodes(KnowledgeBase.HPR_CAUSES, KnowledgeBase.HPR_SYN);
		
		if (IsEmpty(CauseAnswers))
		{
			log(ComposeMessage("No direct 'Cause' Relations for " + descriptor.getName(), Function));

			InferenceEpilogue(pq, Function);
			return null;
		}
		else
		{
			log(ComposeMessage("'" + CauseAnswers.size() + "' direct 'Cause' relations were found for " + descriptor.getName(), Function));
		}

		ArrayList TransitiveCauseAnswers = TDEP(descriptor, KnowledgeBase.HPR_CAUSES);

		if (IsEmpty(TransitiveCauseAnswers))
		{
			log(ComposeMessage("No indirect 'Cause' Relations for " + descriptor.getName(), Function));
		}
		else
		{
			log(ComposeMessage("'" + CauseAnswers.size() + "' indirect 'Cause' relations were found for " + descriptor.getName(), Function));
			
			CauseAnswers.addAll(TransitiveCauseAnswers);
		}

		ArrayList Answers = new ArrayList();

		PlausibleAnswer answer;
		ArrayList<PlausibleAnswer> SecondaryArguments;
		int CauseCounter = 0;
		float SecondaryStatementsCertainty;
		String CauseRelation;
		String SecondaryStatement;
		String AnswerText;
		
		for (PlausibleAnswer CauseAnswer: CauseAnswers)
		{
			CauseCounter++;

			CauseRelation = CauseAnswer.answer.getName() + " CAUSES " + descriptor.getName();
			log(ComposeMessage(CauseCounter + ") " + CauseRelation, Function));

			SecondaryArguments = argument.findSourceNodes(CauseAnswer.answer);

			if (IsEmpty(SecondaryArguments))
			{
				log(ComposeMessage("No supplementary statement for '" + CauseAnswer.answer.getName() + "'.", Function));
				continue;
			}

			SecondaryStatementsCertainty = DempsterShapherCombination(SecondaryArguments);

			answer = new PlausibleAnswer();

			answer.answer = KnowledgeBase.HPR_YES;
			answer.parameters.certainty = CausalityComputeCertainty(CauseAnswer.parameters.certainty, CauseAnswer.parameters.conditionalLikelihood, SecondaryStatementsCertainty);

			_pathHistory.PushReasoningLine(CauseRelation, CauseAnswer.parameters.toString());

			for (PlausibleAnswer SecondaryArgument: SecondaryArguments)
			{
				SecondaryStatement = ComposePlausibleQuestion(CauseAnswer.answer, SecondaryArgument.answer, argument, SecondaryArgument.IsNegative);
				_pathHistory.PushReasoningLine(SecondaryStatement, SecondaryArgument.parameters.toString());
			}

			_pathHistory.PushReasoningLine("> CAUSALITY:", "");

			AnswerText = ComposePlausibleQuestion(descriptor, argument, answer.answer, answer.IsNegative);
			
			_pathHistory.PushReasoningLine(AnswerText, answer.parameters.toString());
			answer.AddJustification(_pathHistory.getReasoningLines());
			_pathHistory.PopReasoningLine(SecondaryArguments.size() + 3);

			log("*" + ComposeMessage(AnswerText, Function));

			Answers.add(answer);
		}

		Answers = CombineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	*/
	/**
	 * Causality Inference for Arguments
	 * @param pq question
	 * @return answers
	 */
	/*
	private ArrayList ACausality(PlausibleQuestion pq)
	{
		String Function = "CAUSINF";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		if (pq.Referent != KnowledgeBase.HPR_YES || pq.Argument != null)
		{
			log(ComposeMessage("Bad referent or argument!", Function));
			
			InferenceEpilogue(pq, Function);
			return null;
		}

		if (pq.Descriptor.PartOfSpeech(KnowledgeBase.HPR_SYN) != POS.Verb)
		{
			log(ComposeMessage("DESCRIPTOR is not a verb!", Function));
			
			InferenceEpilogue(pq, Function);
			return null;			
		}

		// Finding all nodes having a WN-CAUSES relation to the descriptor:

		ArrayList<PlausibleAnswer> descriptorCauses = pq.Descriptor.findSourceNodes(KnowledgeBase.HPR_CAUSES, KnowledgeBase.HPR_SYN);

		if (IsEmpty(descriptorCauses))
		{
			log(ComposeMessage("No 'Causes' Relations for " + pq.Descriptor.getName(), Function));

			InferenceEpilogue(pq, Function);
			return null;
		}

		ArrayList Answers = new ArrayList();

		PlausibleAnswer answer;
		int CauseCounter = 0;
		String CauseRelation;
		String AnswerText;
		String StatName;
		int StatCounter;
		
		for (PlausibleAnswer descriptorCause: descriptorCauses)
		{
			CauseCounter++;

			CauseRelation = descriptorCause.answer.getName() + " CAUSES " + pq.Descriptor.getName();
			log(ComposeMessage(CauseCounter + ") " + CauseRelation, Function));

			StatName = "*" + descriptorCause.answer.getName().toLowerCase() + " (";
			StatCounter = 1;

			// Finding Statements with CauseAnswer as their descriptors.			
			PlausibleStatement Statement = (PlausibleStatement)_kb.FindConcept(StatName + StatCounter + ")");

			while (Statement != null)
			{	
				answer = new PlausibleAnswer();

				answer.answer = Statement.referent;
				answer.parameters.certainty = CausalityComputeCertainty(descriptorCause.parameters.certainty, descriptorCause.parameters.conditionalLikelihood, Statement.parameters.certainty);

				_pathHistory.PushReasoningLine(CauseRelation, descriptorCause.parameters.toString());
				
				AnswerText = ComposePlausibleQuestion(Statement);
				_pathHistory.PushReasoningLine(AnswerText, Statement.parameters.toString());
				log(ComposeMessage(AnswerText, Function));

				_pathHistory.PushReasoningLine("> CAUSALITY:", "");
				
				AnswerText = ComposePlausibleQuestion(pq.Descriptor, answer.answer, KnowledgeBase.HPR_YES, answer.IsNegative);
				_pathHistory.PushReasoningLine(AnswerText, answer.parameters.toString());
				log("*" + ComposeMessage(AnswerText, Function));

				answer.AddJustification(_pathHistory.getReasoningLines());
				
				_pathHistory.PopReasoningLine(4);

				Answers.add(answer);

				//---------------
				StatCounter++;
				Statement = (PlausibleStatement)_kb.FindConcept(StatName + StatCounter + ")");
			}
		}

		Answers = CombineEvidences(Answers, Function, pq);

		InferenceEpilogue(pq, Function);

		return Answers;
	}
	*/
	/**
	 * Attribute Inference
	 * this inference was inspired from a specific WordNet relation named ATTRIBUTE.
	 * this relation says that a noun can have certain adjectives as attributes, e.g. for "weight" we have 
	 * "heavy", "light", etc. using these relation plus IS relation we can draw conclusions as exemplified below:
	 * <br> 1. IS(cage) = {heavy}
	 * <br> 2. weight -- ATTRIBUTE --> heavy
	 * <br> ----------------------------------
	 * <br> :: weight(cage) = {heavy}
	 * 
	 * @param pq question
	 * @return answers
	 */
	
	private ArrayList Attribute(PlausibleQuestion pq)
	{
		Node descriptor = pq.descriptor;
		Node argument = pq.argument;
		Node referent = pq.referent;

		PlausibleQuestion newPQ;

		String Function = "ATTRIBUTE";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> Answers = null;
		ArrayList<PlausibleAnswer> FinalAnswers = null;

		Node SYN = KnowledgeBase.HPR_SYN;
		
		if (descriptor == KnowledgeBase.HPR_IS)
		{
			if (referent == null)
			{
				// We can't answer this conceptType of question (QT1);

				InferenceEpilogue(pq, Function);
				return null;
			}
			
			// QT 2 & 3:
			//	1. IS(?|cage) = {heavy}
			//	2. weight -- Attribute --> heavy
			//	3. weight (?|cage) = {heavy}
			
			if (referent.getPos() != POS.ADJECTIVE && referent.getPos() != POS.SETELLITE_ADJECTIVE)
			{
				InferenceEpilogue(pq, Function);
				return null;
			}

			ArrayList<PlausibleAnswer> Attributes = referent.findTargetNodes(KnowledgeBase.HPR_ATTRIBUTE, SYN);

			if (!IsEmpty(Attributes))
			{
				for (PlausibleAnswer Attribute: Attributes)	
				{
					String reference = composeReference(Attribute.statement);
					
					_pathHistory.pushReasoningLine(referent.getName() + " ATTRIBUTES " + Attribute.answer.getName(), Attribute.parameters.toString(), reference);
					_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");
				
					newPQ = pq.clone();
					newPQ.descriptor = Attribute.answer;
					
					Answers = recall(newPQ);

					if (IsEmpty(Answers))
					{
						continue;
					}

					_pathHistory.popReasoningLine(2);

					for (PlausibleAnswer answer: Answers)
					{
						answer.parameters.certainty = AttributeComputeCertainty(Attribute.parameters.certainty, answer.parameters.certainty);
						
						answer.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, answer), answer.parameters.toString());	
					}
				}
			}

			FinalAnswers = Answers;
		}
		else //-------------------------------------------
		{
			if (descriptor.getPos() != POS.NOUN)
			{
				log(composeReasoningLine("DESCRIPTOR is not a noun!", Function));

				InferenceEpilogue(pq, Function);
				return null;	
			}

			ArrayList<PlausibleAnswer> Attributes = descriptor.findTargetNodes(KnowledgeBase.HPR_ATTRIBUTE, SYN);

			if (IsEmpty(Attributes))
			{
				log(composeReasoningLine("No attributes for '" + descriptor + "'.", Function));

				InferenceEpilogue(pq, Function);
				return null;
			}

			FinalAnswers = new ArrayList();

			if (referent != null)//------------------------------------------------
			{
				int AttCounter = 0;
				CertaintyParameters CP = new CertaintyParameters();
				CP.certainty = -1F;
				
				PlausibleAnswer AttributeMatched = null;
				
				for (PlausibleAnswer Attribute: Attributes)
				{
					AttCounter++;
					log(composeReasoningLine(AttCounter + ") " + descriptor.getName() + " ATTRIBUTES " + Attribute.answer.getName(), Function));				

					if (Attribute.answer == referent)
					{
						CP = Attribute.parameters;
						AttributeMatched = Attribute;
					}
				}

				if (AttributeMatched == null)
				{
					log(composeReasoningLine("referent '" + referent.getName() + " is not in the attribute list of '" + descriptor.getName() + "'.", Function));
				
					InferenceEpilogue(pq, Function);
					return null;
				}
				
				String reference = composeReference(AttributeMatched.statement);

				_pathHistory.pushReasoningLine(descriptor.getName() + " ATTRIBUTES " + referent.getName(), CP.toString(), reference);
				_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");

				newPQ = pq.clone();
				newPQ.descriptor = KnowledgeBase.HPR_IS;
				
				Answers = recall(newPQ);

				if (IsEmpty(Answers))
				{
					log(composeReasoningLine("No answer!", Function));
				
					InferenceEpilogue(pq, Function);
					return null;
				}

				_pathHistory.popReasoningLine(2);

				for (PlausibleAnswer answer: Answers)
				{
					answer.parameters.certainty = AttributeComputeCertainty(CP.certainty, answer.parameters.certainty);
					
					if (argument == null)
					{
						answer.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, answer), answer.parameters.toString());	
					}
					else
					{
						answer.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, answer), answer.parameters.toString());
					}
				}

				FinalAnswers = Answers;
			}//------------------------------------------------------
			else if (referent == null)
			{
				int AttCounter = 0;
				for (PlausibleAnswer Attribute: Attributes)
				{
					AttCounter++;
					log(composeReasoningLine(AttCounter + ") " + descriptor.getName() + " ATTRIBUTES " + Attribute.answer.getName(), Function));
					
					String reference = composeReference(Attribute.statement);

					_pathHistory.pushReasoningLine(descriptor.getName() + " ATTRIBUTES " + Attribute.answer.getName(), Attribute.parameters.toString(), reference);
					_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");

					newPQ = pq.clone();
					newPQ.descriptor = KnowledgeBase.HPR_IS;
					newPQ.referent = Attribute.answer;
					
					Answers = recall(newPQ);
					
					_pathHistory.popReasoningLine(2);

					if (IsEmpty(Answers))
					{
						log(composeReasoningLine("Unsupported Attribute!", Function));
						continue;
					}

					PlausibleAnswer answer = (PlausibleAnswer)Answers.get(0);

					answer.answer = Attribute.answer;
					answer.parameters.certainty = AttributeComputeCertainty(Attribute.parameters.certainty, answer.parameters.certainty);
					answer.AdjustConclusionInJustifications(Function, _reasoningDepth, composeStatement(pq, answer) , answer.parameters.toString());

					FinalAnswers.add(answer);
				}
			}
		}
		
		//ArrayList NounAttributes = DESCRIPTOR.Find			
			
		FinalAnswers = combineEvidences(FinalAnswers, Function, pq);

		InferenceEpilogue(pq, Function);
		return FinalAnswers;
	}


	/**
	 * DESCRIPTOR INVERSE Transform Inference 
	 * @param pq
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> DescriptorInverseTransform(PlausibleQuestion pq)
	{		
		Node Descriptor = pq.descriptor;
		Node Argument = pq.argument;
		Node Referent = pq.referent;
		
		String Function = "INVERSE";

		if (!InferencePrologue(pq, Function))
		{
			return null;
		}

		ArrayList<PlausibleAnswer> APConcepts = Descriptor.findTargetNodes(KnowledgeBase.HPR_INVERSE);

		if (IsEmpty(APConcepts))
		{
			//log(ComposeMessage(" No inverse relations were found for " + DESCRIPTOR.getName(), Function));
			InferenceEpilogue(pq, Function);
			return null;
		}
		else
		{
			log(composeReasoningLine("'" + APConcepts.size() + "' inverse relations were found for " + Descriptor.getName(), Function));
		}
		
		String RelationText;
		PlausibleAnswer ReasonedAnswer;
		PlausibleQuestion newPQ;
		
		//Node APNode;
		
		ArrayList<PlausibleAnswer> Answers;
		ArrayList<PlausibleAnswer> FinalAnswers = new ArrayList<PlausibleAnswer>();

		for (PlausibleAnswer APConcept: APConcepts)
		{
			//APNode = APConcept.Answer;
			
			RelationText = Descriptor.getName() + " " + KnowledgeBase.HPR_INVERSE.getName() + " " + APConcept.answer.getName();

			log(composeReasoningLine(RelationText, Function));
			//---------------------------------------------
			
			String reference = composeReference(APConcept.statement);

			_pathHistory.pushReasoningLine(RelationText, APConcept.parameters.toString(), reference);
			_pathHistory.pushReasoningLine("*CONCLUSION GOES HERE*" + Function + "(" + _reasoningDepth + ")", "", "");

			if (Argument == null)
			{
				newPQ = pq.clone();
				newPQ.descriptor = APConcept.answer;
				newPQ.argument = Referent;
				newPQ.referent = null;
				
				Answers = recall(newPQ);
			}
			else if (Referent == null)
			{
				newPQ = pq.clone();
				newPQ.descriptor = APConcept.answer;
				newPQ.argument = null;
				newPQ.referent = Argument;
				
				Answers = recall(newPQ);
			}
			else
			{
				newPQ = pq.clone();
				newPQ.descriptor = APConcept.answer;
				newPQ.argument = Referent;
				newPQ.referent = Argument;
				
				Answers = recall(newPQ);
			}

			_pathHistory.popReasoningLine(2);

			if (!IsEmpty(Answers))
			{
				for (Object obj: Answers)
				{
					ReasonedAnswer = (PlausibleAnswer)obj;
					ReasonedAnswer.parameters.certainty = DITComputeCertainty(APConcept.parameters.certainty, ReasonedAnswer.parameters.certainty);

					ReasonedAnswer.AdjustConclusionInJustifications(Function, _reasoningDepth , composeStatement(pq, ReasonedAnswer), ReasonedAnswer.parameters.toString());

					FinalAnswers.add(ReasonedAnswer);
				}
			}
		}
		
		// Here we may have more than an answer in ReasonigAL.
		// So we have to rank them and choose the best one.
		FinalAnswers = combineEvidences(FinalAnswers, Function, pq);

		InferenceEpilogue(pq, Function);

		return FinalAnswers;

	}
	
	/**
	 * This function does a Compare inference, the only multi-argument inference in the reasoning engine.
	 * Actually it decides to do one of the sub-inferences available: Compare, Differences and Similarities.
	 * @param PQ question 
	 * @return answers
	 */
	private ArrayList<PlausibleAnswer> MultiArgumentInference(PlausibleQuestion PQ)
	{
		PlausibleAnswer Answer = null;
		
		if (PQ.descriptor.getName().toLowerCase() == "compare")
		{
			Answer = Compare(PQ.argument, PQ.secondArgument, true, true); 
		}
		else if (PQ.descriptor.getName().toLowerCase() == "difference")
		{
			Answer = Compare(PQ.argument, PQ.secondArgument, false, true); 
		}
		else if (PQ.descriptor.getName().toLowerCase() == "similarity")
		{
			Answer = Compare(PQ.argument, PQ.secondArgument, true, false);
		}

		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();
		Answers.add(Answer);

		return Answers;
	}

	/**
	 * the base function for Compare Inference (see above)
	 * @param Arg1 first argument to compare
	 * @param Arg2 second argument to compare
	 * @param Similarities determines of we want to find similarities between to arguments
	 * @param Differences determines of we want to find differences between to arguments
	 * @return an answer
	 */
	private PlausibleAnswer Compare(Node Arg1, Node Arg2, boolean Similarities, boolean Differences)
	{
		String SimilarityAnswer = "";
		String DifferenceAnswer = "";

		Node CommonParent = _kb.findCommonParent(Arg1, Arg2, _maxReasoningDepth);

		PlausibleStatement Arg2Statement;
		
		Hashtable<String, PlausibleStatement> Arg1StatementsHash = new Hashtable<String, PlausibleStatement>();
		Hashtable<String, PlausibleStatement> Arg2StatementsHash = new Hashtable<String, PlausibleStatement>();
		
		ComputeAllInheritedRelations(Arg1, CommonParent, Arg1StatementsHash);
		ComputeAllInheritedRelations(Arg2, CommonParent, Arg2StatementsHash);
		//Node SlippingConcept = Arg2;

		PlausibleStatement Arg1Statement;
		
		Collection<PlausibleStatement> Arg1Statements = Arg1StatementsHash.values();
		
		for (Object obj: Arg1Statements)
		{
			Arg1Statement = (PlausibleStatement)obj;
			
			if (!Arg2StatementsHash.contains(Arg1Statement.relationType.getName()))
			{
				continue;
			}
			
			Arg2Statement = (PlausibleStatement)Arg2StatementsHash.get(Arg1Statement.relationType.getName());

			if (Arg1Statement.referent == Arg2Statement.referent)
			{
				SimilarityAnswer += Arg1Statement.relationType.getName() + "(both " + "'" + Arg1.getName() + "' & '" + Arg2.getName() + "')={" + Arg1Statement.referent.getName() + "}\r";
			}
			else
			{
				DifferenceAnswer += Arg1Statement.relationType.getName() + "('" + Arg1.getName() + "')={" + Arg1Statement.referent.getName() + "} whereas " + Arg1Statement.relationType.getName() + "('" + Arg2.getName() + "')={" + Arg2Statement.referent.getName() + "}\r";
			}
		}

		String Answer = "";
		if (Similarities)
		{
			Answer += "Similarity: \r";
			
			if (CommonParent != null)
			{
				if (Arg1 == CommonParent)
				{
					Answer += Arg2.getName() + " ISA " + Arg1.getName() + "\r" ;
				}
				else if (Arg2 == CommonParent)
				{
					Answer += Arg1.getName() + " ISA " + Arg2.getName() + "\r" ;
				}
				else
				{
					Answer += "Both '" + Arg1.getName() + "' & '" + Arg2.getName()  + "' are " + CommonParent.getName() + "(s)\r";
				}
			}

			Answer += SimilarityAnswer + "\r";
		}
		if (Differences)
		{
			Answer += "Difference: \r" + DifferenceAnswer + "\r";
		}

		Node AnswerNode = new Node(Answer);
		PlausibleAnswer FinalAnswer = new PlausibleAnswer();
		
		FinalAnswer.answer = AnswerNode;
		FinalAnswer.parameters.certainty = CertaintyParameters.defaultCertainty;
	
		return FinalAnswer;
	}

	/**
	 * computes all relation a concept have in the _kb recursively
	 * @param Concept the concept
	 * @param CommonParent a parent of the node
	 * @param VisitedRelations a hash table which will contain all relations
	 */
	private void ComputeAllInheritedRelations(Node Concept, Node CommonParent, Hashtable<String, PlausibleStatement> VisitedRelations)
	{
		if (_reasoningDepth >= _maxReasoningDepth)
		{
			return;
		}

		_reasoningDepth++;
		
		ArrayList<PlausibleStatement> Statements = Concept.getAllStatements(KnowledgeBase.HPR_SYN);

		for (PlausibleStatement Statement: Statements)
		{
			if (!VisitedRelations.contains(Statement.relationType.getName()))
			{
				VisitedRelations.put(Statement.relationType.getName(), Statement);
			}
		}

		ArrayList<Node> Parents = Concept.findAllParents(1);

		for (Node Parent: Parents)
		{
			if (Parent == CommonParent)
			{
				continue;
			}
			
			ComputeAllInheritedRelations(Parent, CommonParent, VisitedRelations);
		}
	}

	/**
	 * computes the certainty in answer produced by a hierarchical inference (SPEC, GEN, SIM, DIS). 
	 * It actually calls other more specific functions to accomplish this task.
	 * @param RelationParameters 
	 * @param AnswerParameters
	 * @param ContextRelevency
	 * @param Direction
	 * @param Function
	 * @return combined certainty
	 */
	private float AHierarchicalComputeCertainty(CertaintyParameters RelationParameters, CertaintyParameters AnswerParameters, float ContextRelevency, ReasoningDirection Direction, String Function)
	{
		switch(Direction)
		{
			case UP		: return GENComputeCertainty(RelationParameters.certainty, RelationParameters.dominance, AnswerParameters.certainty, ContextRelevency, Function);
			case DOWN	: return SPECComputeCertainty(RelationParameters.certainty, RelationParameters.dominance, AnswerParameters.certainty, ContextRelevency, Function);
			case SIDEWAY: return SIMComputeCertainty(RelationParameters.certainty, RelationParameters.similarity, AnswerParameters.certainty, ContextRelevency);
			case ASKANCE: return DISComputeCertainty(RelationParameters.certainty, RelationParameters.similarity, AnswerParameters.certainty, ContextRelevency);
			default		: MyError.exit("Direction can't be UNDETERMINED!");break;
		}

		// You shouldn't be here!
		return 0F; 
	}

	/**
	 * computes the certainty in answer produced by a GEN (generalization) inference
	 * @param GENCertainty
	 * @param GENDominance
	 * @param AnswerCertainty
	 * @param ContextRelevency
	 * @param Function
	 * @return combined certainty
	 */
	private float GENComputeCertainty(float GENCertainty, float GENDominance, float AnswerCertainty, float ContextRelevency, String Function)
	{
		Function = Function.replace("GEN", "SPEC");
		
		float TurnOverPenalty = 1;

		if (_pathHistory.doesGenSpecTurnOver(Function))
		{
			TurnOverPenalty = GEN_SPEC_DEGRADATION_FACTOR;
		}
		
		float Certainty;

		Certainty = TurnOverPenalty *
					GENCertainty *
					GENDominance *
					AnswerCertainty *
					ContextRelevency;

		return Certainty;
	}
	
	/**
	 * computes the certainty in answer produced by a SPEC (specification) inference
	 * @param SPECCertainty
	 * @param SPECDominance
	 * @param AnswerCertainty
	 * @param ContextRelevency
	 * @param Function
	 * @return combined certainty
	 */
	private float SPECComputeCertainty(float SPECCertainty, float SPECDominance, float AnswerCertainty, float ContextRelevency, String Function)
	{
		Function = Function.replace("SPEC", "GEN");
		
		float TurnOverPenalty = 1;

		if (_pathHistory.doesGenSpecTurnOver(Function))
		{
			TurnOverPenalty = GEN_SPEC_DEGRADATION_FACTOR;
		}
		
		float Certainty;

		Certainty = TurnOverPenalty *
					SPECCertainty *
					SPECDominance *
					AnswerCertainty *
					ContextRelevency ;

		return Certainty;
	}
	
	/**
	 * computes the certainty in answer produced by a SIM inference
	 * @param SIMCertainty
	 * @param SIMSimilarity
	 * @param AnswerCertainty
	 * @param ContextRelevency
	 * @return combined certainty
	 */
	private float SIMComputeCertainty(float SIMCertainty, float SIMSimilarity, float AnswerCertainty, float ContextRelevency)
	{
		float Certainty;

		Certainty = SIMCertainty *
					SIMSimilarity *
					AnswerCertainty *
					ContextRelevency;

		return Certainty;
	}
	
	/**
	 * computes the certainty in answer produced by a DIS inference
	 * @param DISCertainty
	 * @param Similarity
	 * @param AnswerCertainty
	 * @param ContextRelevency
	 * @return combined certainty
	 */
	private float DISComputeCertainty(float DISCertainty, float Similarity, float AnswerCertainty, float ContextRelevency)
	{
		float Certainty;

		Certainty = DISCertainty *
					(1 - Similarity) *
					AnswerCertainty *
					ContextRelevency;

		return Certainty;
	}
	
	/**
	 * computes the certainty in answer produced by a DDEP inference
	 * @param DEPCertainty
	 * @param DEPConditionalLikelihood
	 * @param AnswerCertainty
	 * @return combined certainty
	 */
	private float DDEPComputeCertainty(float DEPCertainty, float DEPConditionalLikelihood, float AnswerCertainty)
	{
		float Certainty;

		Certainty = DEPCertainty *
					DEPConditionalLikelihood *
					AnswerCertainty;

		return Certainty;		
	}

	/**
	 * computes the certainty dependency relation
	 * @param Type1
	 * @param Type2
	 * @return combined certainty
	 */
	private DependencyType TDEPCombineDependencyTypes(DependencyType Type1, DependencyType Type2)
	{
		if (Type1 == DependencyType.POSITIVE && Type2 == DependencyType.POSITIVE)
		{
			return DependencyType.POSITIVE;
		}
		if (Type1 == DependencyType.POSITIVE && Type2 == DependencyType.NEGATIVE)
		{
			return DependencyType.NEGATIVE;
		}
		if (Type1 == DependencyType.NEGATIVE && Type2 == DependencyType.POSITIVE)
		{
			return DependencyType.NEGATIVE;
		}
		if (Type1 == DependencyType.NEGATIVE && Type2 == DependencyType.NEGATIVE)
		{
			return DependencyType.POSITIVE;
		}
		else
		{
			return DependencyType.UNMARKED;
			//MyError.ThrowAGeneralException("I can combine just 'POSITIVE' & 'NEGATIVE' dependency dypes");
		}
	}
	
	/**
	 * computes the certainty in answer produced by a TDEP inference
	 * @param FirstRelation
	 * @param SecondRelation
	 * @return combined certainty
	 */
	private CertaintyParameters TDEPCombineParameters(CertaintyParameters FirstRelation, CertaintyParameters SecondRelation)
	{
		CertaintyParameters CP = new CertaintyParameters();

		CP.conditionalLikelihood =	FirstRelation.conditionalLikelihood * 
									SecondRelation.conditionalLikelihood;

		CP.certainty =	FirstRelation.certainty * 
						SecondRelation.certainty;

		return CP;
	}

	/**
	 * computes the certainty in answer produced by a DEPA inference
	 * @param DEPParams
	 * @param PrimaryReferent
	 * @param SecondaryArgument
	 * @param SecondReferent
	 * @return combined certainty
	 */
	private float DEPAComputeCertainty(CertaintyParameters DEPParams, CertaintyParameters PrimaryReferent, CertaintyParameters SecondaryArgument, CertaintyParameters SecondReferent)
	{
		/*
			1: Climate(Surrey) = {?}
			2: Climate <--> ?
			3: Climate <--> Latitude
			4: Latitude(Surrey) = {?}
			5: Latitude(Surrey) = {>40-degrees}
			6: Latitude(?)	= {>40-degrees}
			7: Latitude(Holland) = {>40-degrees}
			8: Climate(Holland) = {Temperate}
			9: Climate(Surrey) = {Temperate}
		*/
		float Certainty;

		Certainty =	PrimaryReferent.certainty *				
					SecondReferent.certainty *
					SecondaryArgument.certainty *
					DEPParams.certainty *
					DEPParams.conditionalLikelihood;

		return Certainty;
	
	}

	/**
	 * computes the certainty in answer produced by an IMP inference
	 * @param IMPParams
	 * @param ISACertainty
	 * @param StatementCertainty
	 * @return combined certainty
	 */
	private float IMPComputeCertainty(CertaintyParameters IMPParams, float ISACertainty, float StatementCertainty)
	{
		float Certainty;

		Certainty = IMPParams.certainty *
					IMPParams.conditionalLikelihood *
					ISACertainty *
					StatementCertainty;

		return Certainty;
	}
	
	/**
	 * another function to compute the certainty in answer produced by a DEPA inference
	 * @param IMPCertainty
	 * @param IMPConditionalLikelihood
	 * @param AnswerCertainty
	 * @return combined certainty
	 */
	private float IMPComputeCertainty(float IMPCertainty, float IMPConditionalLikelihood, float AnswerCertainty)
	{
		return	IMPCertainty *
				IMPConditionalLikelihood *
				AnswerCertainty;
	}

	/**
	 * computes the certainty in answer produced by an ABDUCTION inference
	 * @param IMPParams
	 * @param ISACertainty
	 * @param StatementCertainty
	 * @return combined certainty
	 */
	private float AbductionComputeCertainty(CertaintyParameters IMPParams, float ISACertainty, float StatementCertainty)
	{
		float Certainty;

		Certainty = ABDUCTION_DEGRADATION_FACTOR *
					IMPParams.certainty *
					IMPParams.conditionalLikelihood *
					ISACertainty *
					StatementCertainty;

		return Certainty;
	}

	/**
	 * computes the certainty in answer produced by a SYN inference
	 * @param SYNCertainty
	 * @param AnswerCertainty
	 * @return combined certainty
	 */
	private float SYNComputeCertainty(float SYNCertainty, float AnswerCertainty)
	{
		return	SYNCertainty *
				AnswerCertainty;
		
		// to make this function more realistic you may consider comparing the contextual information (adjacent words in a certain proximity).
	}
	
	/**
	 * computes the certainty in answer produced by a ACAUSALITY or RCAUSALITY inference
	 * @param CausesCertainty
	 * @param CausesConditionalLikelihood
	 * @param OriginalStatementCertainty
	 * @return combined certainty
	 */
	private float CausalityComputeCertainty(float CausesCertainty, float CausesConditionalLikelihood, float OriginalStatementCertainty)
	{
		return	CausesCertainty *
				CausesConditionalLikelihood *
				OriginalStatementCertainty;
	}
	
	/**
	 * computes the certainty in answer produced by a DESCRIPTOR INVERSE Transformation inference
	 * @param APCertainty
	 * @param OriginalStatementCertainty
	 * @return combined certainty
	 */
	private float DITComputeCertainty(float APCertainty, float OriginalStatementCertainty)
	{
		return	APCertainty *
				OriginalStatementCertainty;
	}

	/**
	 * computes the certainty in answer produced by a ATTRIBUTE inference
	 * @param AttributeCertainty
	 * @param AnswerCertainty
	 * @return combined certainty
	 */
	private float AttributeComputeCertainty(float AttributeCertainty, float AnswerCertainty)
	{
		return	AttributeCertainty *
				AnswerCertainty;
	}
	
	/**
	 * computes the certainty in contextual answer produced
	 * @param statementCertainty
	 * @param cxCertainty
	 * @return combined certainty
	 */
	private float CXComputeCertainty(float statementCertainty, float cxCertainty)
	{
		return statementCertainty * 
				cxCertainty;
	}

	/**
	 * implements a simplification of empster-Shapher theory to combine evidences.
	 * it is used to combine the certainty values of competing answer and combine them.
	 * @param Certainty1
	 * @param Certainty2
	 * @return combined certainty
	 */
	private float DempsterShapherCombination(float Certainty1, float Certainty2)
	{
		return Certainty1 + Certainty2 - Certainty1*Certainty2;
	}
	/*
	private float DempsterShapherCombination(ArrayList Answers)
	{
		if (Answers.size() == 0)
		{
			return 0;
		}
		
		float Certainty = ((PlausibleAnswer)Answers.get(0)).parameters.certainty;

		if (Answers.size() == 1)
		{
			return Certainty;
		}

		for (int i=1; i<Answers.size(); i++)
		{
			Certainty = DempsterShapherCombination(Certainty, ((PlausibleAnswer)Answers.get(i)).parameters.certainty);
		}

		return Certainty;
	}
	 */
	
	/**
	 * combines different answers and merges similar ones and finally chooses the best ones to returned back to caller inferences
	 * @param answers
	 * @param Inference
	 * @param pq
	 * @return combined certainty
	 */
	private ArrayList<PlausibleAnswer> combineEvidences(ArrayList<PlausibleAnswer> answers, String Inference, PlausibleQuestion pq)
	{
		if (IsEmpty(answers))
		{
			//log(ComposeMessage("Failed!", Inference));
			return null;
		}

		printAnswers(answers, "~", Inference, pq);

		answers = Summerize(answers);
		printAnswers(answers, "^", Inference, pq);

		answers = ChooseEliteAnswers(answers);
		printAnswers(answers, ":", Inference, pq);

		return answers;		
	}

	/**
	 * combines answers and merges similar ones
	 * @param Answers
	 * @return summerized answers
	 */
	private ArrayList<PlausibleAnswer> Summerize(ArrayList<PlausibleAnswer> Answers)
	{
		if (IsEmpty(Answers) || Answers.size() < 2)
		{
			return Answers;
		}
		
		Hashtable<String, PlausibleAnswer> DistinctAnswers = new Hashtable<String, PlausibleAnswer>();
		PlausibleAnswer repetitiveAnswer;
		ArrayList<PlausibleAnswer> AL = new ArrayList<PlausibleAnswer>();
		String Name;

		for(PlausibleAnswer Answer: Answers)
		{
			if (!Answer.isNegative)
			{
				Name = Answer.answer.getName();
			}
			else
			{
				Name = "¬" + Answer.answer.getName();
			}
			
			if (DistinctAnswers.containsKey(Name))
			{
				repetitiveAnswer = (PlausibleAnswer)DistinctAnswers.get(Name);
				
				ArrayList<String> newJustifications = repetitiveAnswer.getDifferentJustificationsWith(Answer);
				
				if (newJustifications.isEmpty())
					continue;
				
				repetitiveAnswer.parameters.certainty = DempsterShapherCombination(repetitiveAnswer.parameters.certainty, Answer.parameters.certainty);
				repetitiveAnswer.AddJustifications(newJustifications);
				repetitiveAnswer.conditions.addAll(Answer.conditions);
			}
			else
			{
				DistinctAnswers.put(Name, Answer);	
				AL.add(Answer);
			}
		}

		// Handling equality & inequality:

		Hashtable<String, PlausibleAnswer> FinalAnswersTable = new Hashtable<String, PlausibleAnswer>();
		ArrayList<PlausibleAnswer> FinalAnswers = new ArrayList<PlausibleAnswer>();

		for (PlausibleAnswer Answer: AL)
		{
			if (FinalAnswersTable.containsKey(Answer.answer.getName()))
			{
				repetitiveAnswer = (PlausibleAnswer)FinalAnswersTable.get(Answer.answer.getName());

				if (Answer.isNegative && !repetitiveAnswer.isNegative)
				{
					repetitiveAnswer.parameters.certainty = repetitiveAnswer.parameters.certainty - Answer.parameters.certainty;

					if (repetitiveAnswer.parameters.certainty < 0)
					{
						repetitiveAnswer.isNegative = true;
						repetitiveAnswer.parameters.certainty = -repetitiveAnswer.parameters.certainty;
						repetitiveAnswer.RemoveJustifications();
						repetitiveAnswer.AddJustifications(Answer.GetTechnicalJustifications());
					}
				}
				else if (!Answer.isNegative && repetitiveAnswer.isNegative)
				{
					repetitiveAnswer.parameters.certainty = -repetitiveAnswer.parameters.certainty + Answer.parameters.certainty;

					if (repetitiveAnswer.parameters.certainty >= 0)
					{
						repetitiveAnswer.isNegative = false;
						repetitiveAnswer.RemoveJustifications();
						repetitiveAnswer.AddJustifications(Answer.GetTechnicalJustifications());
					}
				}
				else
				{
					MyError.exit("Both answers may not be negative or posotive in the same time!");
				}
			}
			else
			{
				FinalAnswersTable.put(Answer.answer.getName(), Answer);
				FinalAnswers.add(Answer);
			}
		}

		return FinalAnswers;
	}

	/**
	 * generates a simple YES/NO answer based on current deduced answers 
	 * @param answers 
	 * @param referent
	 * @return adjusted answer
	 */
	private ArrayList<PlausibleAnswer> AdjustYesNoAnswers(ArrayList<PlausibleAnswer> answers, Node referent)
	{
		if (IsEmpty(answers))
		{
			return null;
		}
		
		ArrayList<PlausibleAnswer> AdjustedAnswers = new ArrayList<PlausibleAnswer>();

		PlausibleAnswer Yes = new PlausibleAnswer();
		//PlausibleAnswer No  = new PlausibleAnswer();

		Yes.answer = KnowledgeBase.HPR_YES;

		for (PlausibleAnswer Answer: answers)
		{
			if (Answer.answer == referent)
			{
				Yes.copyParameters(Answer.parameters);
				Yes.AddJustifications(Answer.GetTechnicalJustifications());

				AdjustedAnswers.add(Yes);
			}
		}

		return AdjustedAnswers;
	}

	/**
	 * runs an inference
	 * @param Answers
	 * @param AllAnswers
	 */
	private void Do(ArrayList<PlausibleAnswer> Answers, ArrayList<PlausibleAnswer> AllAnswers)
	{
		if (!IsEmpty(Answers))
		{
			AllAnswers.addAll(Answers);
		}
	}

	/**
	 * produces messages to be put in the <code>internalReasoningLines</code> variable
	 * @param Message
	 * @param Inference
	 * @return formatted message
	 */
	private String composeReasoningLine(String Message, String Inference)
	{
		return composeReasoningLine(Message, Inference, false);
	}
	private String composeReasoningLine(String Message, String Inference, boolean IsFirstCall)
	{
		if (!_logReasoningLinesToFile)
			return "";
		
		String out = "";

		out += _reasoningDepth;// + "\t";

		//Out += "\t";

		for (int i=0; i<_reasoningDepth-1; i++) 
		{
			out += "\t";
		}

		if (IsFirstCall && _reasoningDepth <= 1)
		{
			out += ">";
		}
		else if (IsFirstCall)
		{
			out += ">";
		}
		else
		{
			out += "\t";
		}

		out+= Inference;

		out += "\t";

		out += Message;

		return out;
	}
	
	/**
	 * generates a text representation of a plausible question
	 * @param pq
	 * @return text representation
	 */
	private String ComposeStatement(PlausibleQuestion pq)
	{
		return composeStatement(pq, null);
	}
	
	/**
	 * generates a text representation of a plausible question and fills in the extracted answer in the appropriate place
	 * @param pq
	 * @param pa
	 * @return text representation
	 */
	private String composeStatement(PlausibleQuestion pq, PlausibleAnswer pa)
	{
		Node Argument = pq.argument;
		Node Referent = pq.referent;
		
		boolean isNegative = false;		
		boolean isAnswer = false;
		
		if (pa != null)
		{
			isAnswer = true;
			isNegative = pa.isNegative;
		}		
		
		if (Argument == null)
		{
			if (isAnswer)
			{
				Argument = pa.answer;
			}
		}
		else if (Referent == null)
		{
			if (isAnswer)
			{
				Referent = pa.answer;
			}
		}	
		
		String strPQ = ComposePlausibleQuestion(pq.descriptor, Argument, Referent, isNegative);
		
		if (pq.cxTime != KnowledgeBase.HPR_ANY)
		{
			strPQ += " CX:TIME = {";
			
			if (pq.cxTime != null)
			{
				strPQ += pq.cxTime.getName();
			}
			else if (pa != null)
			{
				strPQ += pa.answer.getName();
			}
			else
			{
				strPQ += "?";
			}
			
			strPQ += "}";
		}
		
		if (pq.cxLocation != KnowledgeBase.HPR_ANY)
		{
			strPQ += " CX:LOCATION = {";
			
			if (pq.cxLocation != null)
			{
				strPQ += pq.cxLocation.getName();
			}
			else if (pa != null)
			{
				strPQ += pa.answer.getName();
			}
			else
			{
				strPQ += "?";
			}
			
			strPQ += "}";
		}
		
		return strPQ;
	}
	
	/**
	 * generates a text representation of a plausible statement
	 * @param PS
	 * @return text representation
	 */
	private String ComposePlausibleQuestion(PlausibleStatement PS)
	{
		String Sign = "=";

		if (PS.IsNegative())
		{
			Sign = "≠";
		}

		return PS.relationType.getName() + "(" + PS.argument.getName() + ")" + Sign + "{" + PS.referent.getName() + "}";
	}	
	private String ComposePlausibleQuestion(Node Descriptor, Node Argument, Node Referent, boolean IsAnswerNagative)
	{
		String Sign = "=";
		
		if (IsAnswerNagative)
		{
			Sign = "≠";
		}

		
		String Out = "";

		if (Descriptor == null)
		{
			Out += "?";
		}
		else
		{
			Out += Descriptor.getName();
		}
		Out += "(";

		if (Argument == null)
		{
			Out += "?";
		}
		else
		{
			Out += Argument.getName();
		}
		Out += ")" + Sign + "{";

		if (Referent == null)
		{
			Out += "?";
		}
		else
		{
			Out += Referent.getName();
		}
		Out += "}";

		return Out;
	}
	
	/**
	 * this function should be called at the end of each inference to adjust the reasoning depth and history
	 * @param pq
	 * @param function
	 */
	private void InferenceEpilogue(PlausibleQuestion pq, String function)
	{
		_reasoningDepth--;
		_pathHistory.popHistory(function, pq);
	}

	/**
	 * this function should be called at the beginning of each inference to adjust the reasoning depth and history
	 * @param pq
	 * @param Function
	 * @return true/false
	 */
	private boolean InferencePrologue(PlausibleQuestion pq, String Function)
	{
		_reasoningDepth++;
		totalCalls++;

		String Question = ComposeStatement(pq);

		log(composeReasoningLine(Question, Function, true));

		// We wouldn't like to reason for ever!
		if (_reasoningDepth > _maxReasoningDepth)
		{
			// It's exceeded the Max. Reasoning Depth
			log(composeReasoningLine("BACKTRACK", Function));
			totalBackTracks++;
			_reasoningDepth--;

			return false;
		}

		// TODO: change the way we find out that a node has been visited before:
		// We don't think twice about a matter.
		// TODO: we should take into account the CXTIME and CXLOCATION in the plausible question 
		// hen searching in history
		if (_pathHistory.isInHistory(Function, pq.descriptor, pq.argument, pq.referent))
		{
			log("@" + composeReasoningLine("Recurrent Question!", Function));
			//Print(ComposeMessage("History = " + PathHistory.ComposeHistory(), Function));

			_reasoningDepth--;
			return false;
		}

		_pathHistory.pushHistory(Function, pq.descriptor, pq.argument, pq.referent);

		return true;
	}

	/**
	 * puts a message into the <code>internalReasoningLine</code> variable
	 * @param Output
	 */
	private void log(String Output)
	{
		if (_logReasoningLinesToFile)
		{		
			/*
			if (reasoningDepth <= maxOnlineStdoutPrintLevel)
			{
				System.out.println(Output);
			}
			*/
			
			try
			{
				_internalReasoningLinesLogFile.write(Output + "\r\n");
			}
			catch(Exception e)
			{
				_internalReasoningLinesLogFile = null;
			}
		}
	}
	
	/**
	 * checks if an ArrayList is null or empty
	 * @param AL
	 * @return true/false
	 */
	private boolean IsEmpty(ArrayList<PlausibleAnswer> AL)
	{
		if (AL == null) 
		{
			return true;
		}
		if (AL.size() == 0)
		{
			return true;
		}
		
		return false;
	}

	/**
	 * checks if we have a YES answer (manages negative NOs too)
	 * @param answer
	 * @return true/false
	 */
	private boolean IsYes(PlausibleAnswer Answer)
	{
		if (Answer == null)
		{
			return false;
		}

		if (Answer.answer == KnowledgeBase.HPR_YES && !Answer.isNegative)
		{
			return true;
		}

		if (Answer.answer == KnowledgeBase.HPR_NO && Answer.isNegative)
		{
			return true;
		}

		return false;
	}


	/**
	 * chooses the top <code>_maxAnswersNumber</code> answers to be return to the caller inference
	 * @param Answers
	 * @return top elite answers
	 */
	private ArrayList<PlausibleAnswer> ChooseEliteAnswers(ArrayList<PlausibleAnswer> Answers)
	{
		Collections.sort(Answers);

		if (Answers.size() <= _maxAnswersNumber)
		{
			return Answers;
		}

		ArrayList<PlausibleAnswer> EliteAnswers = new ArrayList<PlausibleAnswer>();
		
		int Counter = 0;
		for (PlausibleAnswer Answer: Answers)
		{
			Counter++;
			EliteAnswers.add(Answer);
			
			if (Counter == _maxAnswersNumber)
			{
				break;
			}
		}

		return EliteAnswers;		
	}

	/**
	 * a handy function to print extracted answers to the <code>internalReasoningLine</code> variable
	 * @param Answers
	 * @param Symbol
	 * @param Inference
	 * @param pq
	 */
	private void printAnswers(ArrayList<PlausibleAnswer> Answers, String Symbol, String Inference, PlausibleQuestion pq)
	{
		String Statement;
		
		for (PlausibleAnswer Answer: Answers)
		{
			Statement = composeStatement(pq, Answer);
			
			log(Symbol + composeReasoningLine(Statement + "\t" + Answer.parameters, Inference));
		}
	}

	/**
	 * checks if a question is well-formed, i.e. if it  has the minimum number of not null arguments
	 * @param pq
	 * @return true/false
	 */
	private boolean isValidPlausibleQuestion(PlausibleQuestion pq)
	{
		if (pq.descriptor == null)
		{
			return false;
		}

		if (pq.argument == null && pq.referent == null)
		{
			return false;
		}
		
		/*
		if (PQ.Argument == KnowledgeBase.HPR_ANY && PQ.Referent == null)
		{
			return false;
		}
		
		if (PQ.Argument == null && PQ.Referent == KnowledgeBase.HPR_ANY)
		{
			return false;
		}
		*/

		if (pq.IsMultiArgument)
		{
			if (pq.argument == null || pq.secondArgument == null)
			{
				return false;
			}
		}

		if (pq.IsMultiReferent)
		{
			if (pq.referent == null || pq.secondReferent == null)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * create an artificial answer with a custom message in it
	 * @param Msg
	 * @return an plausible answer with the message as its answer
	 */
	private ArrayList<PlausibleAnswer> createArtificialAnswer(String Msg)
	{
		return createArtificialAnswer(Msg, false);
	}
	private ArrayList<PlausibleAnswer> createArtificialAnswer(String Msg, boolean IsStructuredText)
	{
		ArrayList<PlausibleAnswer> Answers = new ArrayList<PlausibleAnswer>();
		
		// the reasoning engine didn't return any answer (even 'I don't know') so:
		Node IDonNotUnderstand = new Node(Msg);
		PlausibleAnswer Answer = new PlausibleAnswer();
		Answer.answer = IDonNotUnderstand;
		Answer.parameters.certainty = CertaintyParameters.defaultCertainty;

		if (IsStructuredText)
		{
			Answer.isStructuredTextAnswer = true;
		}

		Answers = new ArrayList<PlausibleAnswer>();
		Answers.add(Answer);

		return Answers;
	}
	
	/**
	 * a handy function to extract the first answer from an array list of answers
	 * @param Answers
	 * @return the first answer in the list
	 */
	private PlausibleAnswer GetFirstAnswer(ArrayList<PlausibleAnswer> Answers)
	{
		if (IsEmpty(Answers))
		{
			return null;
		}
        
		return (PlausibleAnswer)Answers.get(0);
	}
	
	/**
	 * getter function for the _kb variable
	 * @return the current knowledge base
	 */
	public KnowledgeBase getKnowledgeBase()
	{
		return _kb;
	}
	
	/**
	 * Logs something to external file
	 * @param line The text string to be logged
	 */
	private void logToFile(String line)
	{
		if (_logFile != null)
		{
			String now = Common.getDateTime("yyyy-MM-dd HH:mm:ss");
			
			try
			{
				_logFile.write(now + " " + line + "\r\n");
			}
			catch (IOException e)
			{
				try
				{
					_logFile.close();
				}
				catch (IOException e1)
				{}
				
				_logFile = null;
			}
		}
	}
	
	/**
	 * Starts QA logging to external log file
	 * @return True if the log file was opened successfully, false otherwise
	 */
	public boolean logStart()
	{
		try
		{
			_logFile = new BufferedWriter(new FileWriter("log/hpr.log", true)); // append mode
		}
		catch (IOException e)
		{
			_logFile = null;			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Ends QA logging to external log file
	 */
	public void logEnd()
	{
		if (_logFile != null)
		{
			try
			{
				_logFile.close();
			}
			catch (Exception e) 
			{
				_logFile = null;
			}
		}		
	}
	
	/**
	 * Finds antonyms of a concept using DIS relations
	 * 
	 * @param lex The word we are searching for
	 * @param pos The part-of-speech tag for that word
	 * @return An array of <code>PlausibleAnswer</code>s  
	 */
	public ArrayList<PlausibleAnswer[]> findAntonyms(String lex, POS pos)
	{				
		ArrayList<PlausibleAnswer[]> answersset = new ArrayList<PlausibleAnswer[]>();

		ArrayList<PlausibleAnswer> senses = getSensesFromLemma(lex, pos);
			
		if (senses.size() == 0)
			return null;
		
		for (PlausibleAnswer sense: senses)
		{
			HashSet<PlausibleAnswer> answers  = new HashSet<PlausibleAnswer>();
			
			ArrayList<PlausibleAnswer> sims = findSynonyms(sense.answer);
			
			for (PlausibleAnswer sim: sims)
			{
				ArrayList<PlausibleAnswer> diss = sim.answer.findTargetNodes(KnowledgeBase.HPR_DIS);
				
				answers.addAll(diss);				
			}
			
			HashSet<PlausibleAnswer> answersExpanded  = new HashSet<PlausibleAnswer>();
			
			for (PlausibleAnswer answer: answers)
			{
				answersExpanded.addAll(findSynonyms(answer.answer));
			}
			
			if (answersExpanded.size() > 0)
			{									
				answersset.add((PlausibleAnswer[])answersExpanded.toArray(new PlausibleAnswer[0]));
			}
		}	

		return answersset;
	}
	
	
	/**
	 * Finds synonyms of a concept using SIM relations
	 * 
	 * @param lex The word we are searching for
	 * @param pos The part-of-speech tag for that word
	 * @return Synonyms found
	 */
	public ArrayList<PlausibleAnswer[]> findSynonyms(String lex, POS pos)
	{		
		ArrayList<PlausibleAnswer[]> answerset = new ArrayList<PlausibleAnswer[]>();
		
		ArrayList<PlausibleAnswer> answers = null;
		
		ArrayList<PlausibleAnswer> senses = getSensesFromLemma(lex, pos);
		
		if (senses.size() == 0)
			return null;
		
		for (PlausibleAnswer sense: senses)
		{
			answers = new ArrayList<PlausibleAnswer>();
			
			ArrayList<PlausibleAnswer> sims = findSynonyms(sense.answer);
			
			if (sims.size() > 0)
			{									
				answerset.add((PlausibleAnswer[])sims.toArray(new PlausibleAnswer[0]));
			}
		}		
		
		return answerset;
	}
	
	public HashSet<Node> findSynonymsMerged(String lex, POS pos)
	{		
		HashSet<Node> out = new HashSet<Node>();
		HashSet<String> seen = new HashSet<String>();
		
		ArrayList<PlausibleAnswer> senses = getSensesFromLemma(lex, pos);
		
		if (senses.size() == 0)
			return out;
		
		for (PlausibleAnswer sense: senses)
		{
			ArrayList<PlausibleAnswer> sims = findSynonyms(sense.answer);
			
			for (PlausibleAnswer sim: sims)
			{
				if (sim.answer.getLexicalType() == LexicalType.SYNSET)
					continue;
				
				String senseLess = Common.removeSenseInfo(sim.answer.getName());
				
				if (seen.contains(senseLess))
					continue;
				
				out.add(sim.answer);
				seen.add(senseLess);				
			}
		}
		
		return out;
	}
	
	
	public ArrayList<PlausibleAnswer> findSynonyms(Node concept)
	{
		HashSet<String> seens = new HashSet<String>();
		
		return findSynonyms(concept, seens);
	}
	
	/**
	 * Finds all the synonyms of a concept
	 * @param concept input concept
	 * @return the list of its synonyms
	 */
	private ArrayList<PlausibleAnswer> findSynonyms(Node concept, HashSet<String> seens)
	{
		ArrayList<PlausibleAnswer> answers = new ArrayList<PlausibleAnswer>();
		
		if (seens.contains(concept.getName()))
			return answers;
		
		ArrayList<PlausibleAnswer> synonyms = findSiblingLexsPlusSynset(concept);
		
		synonyms.addAll(concept.findTargetNodes(KnowledgeBase.HPR_SIM));
		
		for (PlausibleAnswer synonym: synonyms)
		{
			if (seens.contains(synonym.answer.getName()))
				continue;
			
			answers.add(synonym);
			seens.add(synonym.answer.getName());
			
			ArrayList<PlausibleAnswer> sims = synonym.answer.findTargetNodes(KnowledgeBase.HPR_SIM);
			
			for (PlausibleAnswer sim: sims)
			{
				answers.addAll(findSynonyms(sim.answer, seens));
			}
		}
		
		return answers;
	}
	
	/**
	 * Gets a string and tries to find the a synset corresponding to that string with the right part-of-speech tag
	 * @param lex input string
	 * @param pos part of speech required
	 * @return found synsets
	 */
	public ArrayList<PlausibleAnswer> findSynsetsFromLemma(String lex, POS pos)
	{
		ArrayList<PlausibleAnswer> output = new ArrayList<PlausibleAnswer>(); 
		
		ArrayList<PlausibleAnswer> senses = getSensesFromLemma(lex, pos);
		ArrayList<PlausibleAnswer> found;
		
		if (senses.size() == 0)
		{
			return output;
		}
		else
		{
			for (PlausibleAnswer sense: senses)
			{
				output.addAll(sense.answer.findTargetNodes(KnowledgeBase.HPR_SYN));
			}
		}
		
		return output;
	}
	
	/**
	 * Finds siblings of a sense node. That is, all the nodes belonging to the same synset as this node does.
	 * @param sense The input sense node
	 * @return A list of siblings
	 */
	public ArrayList<PlausibleAnswer> findSiblingLexs(Node sense)
	{
		ArrayList<PlausibleAnswer> output = new ArrayList<PlausibleAnswer>(); 
		
		ArrayList<PlausibleAnswer> synsets = sense.findTargetNodes(KnowledgeBase.HPR_SYN);
		
		if (synsets.size() == 0)
			return output;
		
		Node synset = synsets.get(0).answer;
		
		return synset.findSourceNodes(KnowledgeBase.HPR_SYN);
	}
	
	/**
	 * Finds siblings of a sense node plus the synset node itself.
	 * @param sense The input sense node
	 * @return A list of siblings + the synset node
	 */
	public ArrayList<PlausibleAnswer> findSiblingLexsPlusSynset(Node sense)
	{
		ArrayList<PlausibleAnswer> output = new ArrayList<PlausibleAnswer>(); 
		
		ArrayList<PlausibleAnswer> synsets = sense.findTargetNodes(KnowledgeBase.HPR_SYN);
		
		if (synsets.size() == 0)
			return output;
		
		Node synset = synsets.get(0).answer;
		
		ArrayList<PlausibleAnswer> out = synset.findSourceNodes(KnowledgeBase.HPR_SYN);
		
		out.add(synsets.get(0));
		
		return out;
	}
	
	public ArrayList<PlausibleAnswer> getSensesFromLemma(Node lemma, POS pos)
	{
		return getSensesFromLemma(lemma.getName(), pos);
	}
	/**
	 * Extracts all senses for a lemma
	 * 
	 * @param lemma The word to search for
	 * @param pos The POS tag of the word
	 * @return a list of senses found for this word
	 */
	public ArrayList<PlausibleAnswer> getSensesFromLemma(String name, POS pos)
	{	
		ArrayList<PlausibleAnswer> senses = new ArrayList<PlausibleAnswer>();
		
		if (pos == POS.ANY)
		{
			senses.addAll(getSensesFromLemma(name, POS.NOUN));
			senses.addAll(getSensesFromLemma(name, POS.ADJECTIVE));
			senses.addAll(getSensesFromLemma(name, POS.SETELLITE_ADJECTIVE));
			senses.addAll(getSensesFromLemma(name, POS.ADVERB));
			senses.addAll(getSensesFromLemma(name, POS.VERB));
			
			return senses;
		}
		
		//---------------------
		
		switch (pos)
		{
			case NOUN					: name += "#n"; break;
			case VERB					: name += "#v"; break;
			case ADJECTIVE				: name += "#a"; break;
			case SETELLITE_ADJECTIVE	: name += "#s"; break;
			case ADVERB					: name += "#r"; break;
			case ANY				: MyError.exit("Invalid POS!");
		}
		
		if (_cacheDisambiguations.containsKey(name))
		{
			return _cacheDisambiguations.get(name);
		}
		
		//int allSenseCount = 0;
		int i = 1;
		Node sense = _kb.findConcept(name + i);
		
		while (sense != null)
		{
			// TODO: the following code segment might be logically wrong as WNTaggedCount is calculated and rewritten every time 
			/*synset = sense.FindTargetNodes(KnowledgeBase.HPR_SYN).get(0);
			
			if (synset != null)
			{
				synset.answer.WNTaggedCount = sense.WNTaggedCount;
				allSenseCount += sense.WNTaggedCount;
				
				senses.add(synset);
			}
			*/
			
			senses.add(new PlausibleAnswer(sense));
			
			i++;
			sense = _kb.findConcept(name + i);
		}
		
		// TODO: think of better a disambiguation metric instead of Wordnet's tagged count as a basis 
		/*
		for (PlausibleAnswer pa: senses)
		{
			if (allSenseCount == 0)
			{
				pa.parameters.certainty = 1.0F / (float)synsets.size();
			}
			else
			{
				pa.parameters.certainty = (float)pa.answer.WNTaggedCount / (float)allSenseCount;
				
				if (pa.parameters.certainty == 0)
				{
					pa.parameters.certainty = 0.01F;
				}
			}
		}
		*/
		
		if (senses.size() > 0)
			_cacheDisambiguations.put(name, senses);

		return senses;
	}
	
	/**
	 * Composes the reference for a plausible statement
	 * @param ps
	 * @return
	 */
	public String composeReference(PlausibleStatement ps)
	{
		if (ps == null)
			return "";
    	
    	if (ps.getSourceType() == SourceType.UNKNOWN)
    		return "";
		
		String out = "SOURCE = " + ps.getSourceType().getFarsiName();
		
		ArrayList<PlausibleAnswer> refs = ps.findTargetNodes(KnowledgeBase.HPR_REF);
		
		if (refs.size() == 0)
			return out;
		
		out += ", REFERENCES = {";
		
		for (PlausibleAnswer ref: refs)
		{
			out += ref.answer.getName();
			
			ArrayList<PlausibleAnswer> verses = ref.answer.findTargetNodes(KnowledgeBase.HPR_VERSE_FARSI);
			
			if (verses.size() > 0)
			{
				out += ": " + verses.get(0).answer;
			}
			
			out += ", ";
		}
		
		out += "]";
		
		out = out.replace(", }", "}");
		
		return out;
	}

	public ArrayList<PlausibleAnswer> answerPovQuestion(String arg, String des, String ref)
	{
		PlausibleQuestion pq = new PlausibleQuestion();
		
		pq.argument = _kb.findConcept(arg);
		pq.referent = _kb.findConcept(ref);
		pq.descriptor = _kb.findConcept(des);
		
		return answerQuestion(pq);
	}
}