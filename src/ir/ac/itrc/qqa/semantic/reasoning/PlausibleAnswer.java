package ir.ac.itrc.qqa.semantic.reasoning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import ir.ac.itrc.qqa.semantic.enums.DependencyType;
import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.enums.TargetMatchType;
import ir.ac.itrc.qqa.semantic.kb.CertaintyParameters;
import ir.ac.itrc.qqa.semantic.kb.Node;
import ir.ac.itrc.qqa.semantic.util.Common;

/**
 * implements a single plausible answer used extensively all over the HPR Engine for passing results back and forth.
 * @author Ehsan Darrudi
 *
 */
public class PlausibleAnswer implements Comparable<Object>
{
	/** the answer */
	public Node answer = null;

	/** polarity of the answer */
	public boolean isNegative = false;

	/** certainty parameters attached to this answer */
	public CertaintyParameters parameters = new CertaintyParameters();

	/** for dependency conceptType answers */
	public DependencyType dependencyType = DependencyType.ANY;

	/** justification for this answer */
	private ArrayList<String> _justifications = new ArrayList<String>();

	/** contextual info for this answer */
	public ArrayList<PlausibleAnswer> contexs = new ArrayList<PlausibleAnswer>();
	
	/** for conditional answers holds the unsatisfied premises */
	public ArrayList<String> conditions = new ArrayList<String>();

	/** for tagged answers */
	public boolean isStructuredTextAnswer = false;

	public PlausibleStatement statement = null;
	
	public PlausibleQuestion question = null;
	
	public TargetMatchType targetMatch = TargetMatchType.NOT_SET;
	
	public SourceType source = SourceType.UNKNOWN;
	
	public float score = 0;

	// ~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * Default constructor
	 */
	public PlausibleAnswer()
	{
		// do nothing;
	}
	
	/**
	 * Constructor
	 * @param answer The answer associated with this answer
	 */
	public PlausibleAnswer(Node answer)
	{
		this.answer = answer;
		
		this.parameters = new CertaintyParameters();		
	}

	public void copyParameters(CertaintyParameters CP)
	{
		parameters = new CertaintyParameters(CP);
	}
	
	public void copyParameters(PlausibleAnswer answer)
	{
		parameters = new CertaintyParameters(answer.parameters);
		isNegative = answer.isNegative;
	}
	
	public PlausibleAnswer(String answer)
	{
		this(new Node(answer));		
	}

	/**
	 * adds a line of justification for this answer
	 * @param JustificationIn justification
	 */
	public void AddJustification(String JustificationIn)
	{
		_justifications.add(JustificationIn);
	}

	/**
	 * adds several lines of justifications for this answer
	 * @param JustificationsIn justifications
	 */
	public void AddJustifications(ArrayList<String> JustificationsIn)
	{
		_justifications.addAll(JustificationsIn);
	}
	
	/**
	 * removes all justifications for this answer
	 */
	public void RemoveJustifications()
	{
		_justifications.clear();
	}

	/**
	 * inserts the conclusion into justifications
	 * @param Inference inference name
	 * @param Depth reasoning depth
	 * @param Statement accompanying statement
	 * @param Certainty certainty parameters for this conclusion
	 */
	public void AdjustConclusionInJustifications(String Inference, int Depth, String Statement, String Certainty)
	{
		AdjustConclusionInJustifications(Inference, Depth, Statement, Certainty, "", "");
	}
	public void AdjustConclusionInJustifications(String Inference, int Depth, String Statement, String Certainty, String ExtraOld,	String ExtraNew)
	{
		String Justification;
		String Old = "*CONCLUSION GOES HERE*" + Inference + "(" + Depth + ")";
		String New = "> " + Inference + ":\r" + Statement + " : " + Certainty;

		ArrayList<String> NewJustifications = new ArrayList<String>();

		Iterator<String> JustificationEnum = _justifications.iterator();
		while (JustificationEnum.hasNext())
		{
			Justification = (String) JustificationEnum.next();
			Justification = Justification.replace(Old, New);

			if (ExtraOld != "")
			{
				Justification = Justification.replace(ExtraOld, ExtraNew);
			}

			NewJustifications.add(Justification);
		}

		_justifications = NewJustifications;
	}
	

	/**
	 * gets stored justifications
	 * @return a list of justifications
	 */
	public ArrayList<String> GetTechnicalJustifications()
	{
		return _justifications;
	}
	
	public String toString()
	{
		return toString(false);
	}

	public String toString(boolean IsTagged)
	{
		if ((/* IsDescriptiveAnswer && */IsTagged) || isStructuredTextAnswer)
		{
			// for descriptive answers we always return tagged answer because
			// we can't show multi-line answers in the the console's listbox .
			//return TagAnswer();
			return answer.getName();
		}
		else
		{
			if (isNegative)
			{
				return "≠ " + answer.getName() + " " + parameters.toString();
			}
			else
			{
				return answer.getName() + " " + parameters.toString();
			}
		}
	}

	@Override
	public int compareTo(Object obj)
	{
		PlausibleAnswer other = (PlausibleAnswer)obj; 
		
		if (this.parameters.certainty > other.parameters.certainty)
		{
			return -1;
		}
		else if (this.parameters.certainty < other.parameters.certainty)
		{
			return +1;
		}
		else 
		{
			// the answers have equal certainty, the lengthier answer is preferable as it may contain more information
			if (this.answer.getName().length() > other.answer.getName().length())
				return -1;
			else if (this.answer.getName().length() < other.answer.getName().length())
				return +1;
		}

		return 0;
	}
	
	public String toStringWithoutSign()
	{
		return answer.getName();
	}

	public String certaintyToString()
	{
		return certaintyToString(false);
	}

	public String certaintyToString(boolean isQualifyNumber)
	{
		if (isQualifyNumber)
		{
			return Common.qualifyNumber(parameters.certainty);
		}
		else
		{
			return String.format("%.2f", parameters.certainty);
		}
	}

	/**
	 * generates a text representation of contextual info attached to this answer 
	 * @return text representation
	 */
	public String ComposeCX()
	{
		if (contexs == null || contexs.size() == 0)
		{
			return "";
		}

		String Out = ", CX ";
		int Num = 0;

		for (PlausibleAnswer CX : contexs)
		{
			Num++;

			Out += CX.answer.getName();

			if (Num != contexs.size())
			{
				Out += " AND ";
			}
		}

		return Out;
	}
	
	/**
	 * converts plausible answers to nodes. the certainty parameters are discarded.
	 * @param PlausibleAnswers array list containing initial answers
	 * @return a list of nodes
	 */
	public static ArrayList<Node> ConvertPlausibleAnswersToNodes(ArrayList<PlausibleAnswer> PlausibleAnswers)
	{
		ArrayList<Node> Nodes = new ArrayList<Node>();
		
		for (PlausibleAnswer Answer: PlausibleAnswers)
		{
			Nodes.add(Answer.answer);
		}

		return Nodes;
	}
	
	private String AdjustParameter(String Text, String Param, String name, boolean IsQualifyNumber)
	{
		if (!IsQualifyNumber)
		{
			// no need to qualify parameter.
			return Text.replace(Param, name);
		}

		int Pos = Text.indexOf(Param);

		while (Pos != -1)
		{

			Text = Common.removeSubstring(Text, Pos, Param.length());
			Text = Common.insertSubstring(Text, Pos, name);

			Pos += (name.length() - Param.length()) + 4; // too pass two spaces
															// and '=' sign

			int Length = 0;
			while (Text.charAt(Pos + Length) != ']'
					&& Text.charAt(Pos + Length) != ' '
					&& Text.charAt(Pos + Length) != ',')
			{
				Length++;
			}

			String Quantification = Text.substring(Pos, Length);

			String Qualification = Common.qualifyNumber(Quantification);

			Text = Common.removeSubstring(Text, Pos, Length);
			Text = Common.insertSubstring(Text, Pos, Qualification);

			Pos = Text.indexOf(Param);
		}

		return Text;
	}
	
	private String TagAnswer()
	{
		String TaggedAnswer = Common.removeSenseInfos(answer.getName());

		TaggedAnswer = tagPlausibleRelations(TaggedAnswer);
		TaggedAnswer = tagReferences(TaggedAnswer);

		TaggedAnswer = TaggedAnswer.replace(" -- ", " -- <b>");
		TaggedAnswer = TaggedAnswer.replace(" --> ", "</b> -- ");
		TaggedAnswer = TaggedAnswer.replace("Node: ", "Concept: ");
		TaggedAnswer = TaggedAnswer.replace("Out Links:", "");
		TaggedAnswer = TaggedAnswer.replace("In Links:", "");

		TaggedAnswer = TaggedAnswer.replace("\r", "<br>");
		TaggedAnswer = TaggedAnswer.replace(" '", " <font color=green>");
		TaggedAnswer = TaggedAnswer.replace("('", "(<font color=green>");
		TaggedAnswer = TaggedAnswer.replace("' ", "</font> ");
		TaggedAnswer = TaggedAnswer.replace("')", "</font>)");

		TaggedAnswer = TaggedAnswer.replace("Similarity: ",
				"<font color=#9966FF>Similarities: </font>");
		TaggedAnswer = TaggedAnswer.replace("Difference: ",
				"<font color=#9966FF>Differences: </font>");
		TaggedAnswer = TaggedAnswer.replace(" whereas ",
				"<font color=red> whereas </font>");

		TaggedAnswer = TaggedAnswer.replace("[", "<font color=#CCCCCC>[");
		TaggedAnswer = TaggedAnswer.replace("]", "]</font>");

		TaggedAnswer = TaggedAnswer.replace("DEP+", "Affects/Causes");

		TaggedAnswer = "<font face=tahoma size=2>" + TaggedAnswer + "</font>";

		return TaggedAnswer;
	}

	public ArrayList<String> getConditions()
	{
		return getConditions(true);
	}

	public ArrayList<String> getConditions(boolean IsTagged)
	{
		String Line;

		ArrayList<String> TextConditions = new ArrayList<String>();

		for (String Condition : conditions)
		{
			if (IsTagged)
			{
				Line = "<font face=tahoma size=2>"	+ tagPlausibleRelations(Common.removeSenseInfos(Condition)) + "</font>";
			}
			else
			{
				Line = Condition + "\r";
			}

			TextConditions.add(Line);
		}

		return TextConditions;
	}


	public ArrayList<String> GetTaggedNaiveJustifications()
	{
		return getTaggedNaiveJustifications(true);
	}
	
	public ArrayList<String> getTaggedNaiveJustifications(boolean IsQualifyNumber)
	{
		ArrayList<String> NewJustifications = new ArrayList<String>();
		String Text;

		for (String Justification : _justifications)
		{
			Text = Justification;

			//Text = Common.RemoveSenses(Text);

			Text = InsertHTMLTags(Text, IsQualifyNumber);

			NewJustifications.add(Text);
		}

		return NewJustifications;
	}

	public ArrayList<String> TaggedNaiveJustificationWithSenses()
	{
		ArrayList<String> NewJustifications = new ArrayList<String>();
		int NumberPosition;
		String Text;

		for (String Justification : _justifications)
		{
			Text = Justification;

			NumberPosition = Text.indexOf("#");
			while (NumberPosition != -1)
			{
				Text = Common.insertSubstring(Text, NumberPosition,
						"<font color='#CCCCCC'>");

				NumberPosition += "<font color='#CCCCCC'>".length();

				while (Text.charAt(NumberPosition) != ' '
						&& Text.charAt(NumberPosition) != '}'
						&& Text.charAt(NumberPosition) != ')'
						&& Text.charAt(NumberPosition) != '(')
				{
					NumberPosition++;
				}
				Text = Common.insertSubstring(Text, NumberPosition,
						"</font>");

				NumberPosition = Text.indexOf("#", NumberPosition);
			}

			Text = InsertHTMLTags(Text, false);

			NewJustifications.add(Text);
		}

		return NewJustifications;
	}

	private String InsertHTMLTags(String Text, boolean IsQualifyNumber)
	{
		int EnterPosition;
		Integer LineNumber;

		Text = Text.replace("AGEN", "::Argument Generalization</span>");
		Text = Text.replace("ASPEC", "::Argument Specification</span>");
		Text = Text.replace("ASIM", "::Argument Similarity</span>");
		Text = Text.replace("ADIS", "::Argument Dissimilarity</span>");
		Text = Text.replace("ASYN", "::Argument Synonymy</span>");

		Text = Text.replace("DGEN", "::Descriptor Generalization</span>");
		Text = Text.replace("DSPEC", "::Descriptor Specification</span>");
		Text = Text.replace("DSIM", "::Descriptor Similarity</span>");
		Text = Text.replace("DDIS", "::Descriptor Dissimilarity</span>");
		Text = Text.replace("DSYN", "::Descriptor Synonymy</span>");

		Text = Text.replace("RGEN", "::Referent Generalization</span>");
		Text = Text.replace("RSPEC", "::Referent Specification</span>");
		Text = Text.replace("RSIM", "::Referent Similarity</span>");
		Text = Text.replace("RDIS", "::Referent Dissimilarity</span>");
		Text = Text.replace("RSYN", "::Referent Synonymy</span>");

		Text = Text.replace("DDEP+", "::Derivation from POSITIVE Dependency</span>");
		Text = Text.replace("DDEP-", "::Derivation from NEGATIVE Dependency</span>");
		Text = Text.replace("DEPA", "::Dependency-based Analogy</span>");
		Text = Text.replace("DIMP", "::Derivation from Implication</span>");
		Text = Text.replace("ABD", "::Abduction</span>");

		Text = Text.replace("CAUSINF", "::Causality Inference</span>");
		Text = Text.replace("ATTINF", "::Attribute Inference</span>");

		Text = Text.replace("APDT", "::DESCRIPTOR Inversion Transform</span>");
		
		Text = Text.replace("DISAMBIGUATION", "::Disambiguation</span>");
		Text = Text.replace("AMBIGUATION", "::Ambiguation</span>");
		
		Text = Text.replace("INVERSE", "::Inverse Inference</span>");

		Text = tagPlausibleRelations(Text);
		Text = tagReferences(Text);

		Text = "<b>1</b>. " + Text;
		LineNumber = 2;
		
		EnterPosition = Text.indexOf("\r");
		
		while (EnterPosition != -1)
		{
			if (EnterPosition + 1 < Text.length() && Text.charAt(EnterPosition + 1) != '\r')
			{
				if (Text.charAt(EnterPosition + 1) != '>')
				{
					Text = Common.insertSubstring(Text, EnterPosition + 1, "<b>" + LineNumber.toString() + "</b>. ");
					LineNumber++;
				}
				else
				{
					// Text = Text.Remove(EnterPosition + 1, 1);
				}

				EnterPosition = Text.indexOf("\r", EnterPosition);
			}

			EnterPosition = Text.indexOf("\r", EnterPosition + 1);
		}

		Text = Text.replace(" : [", " : <font color='red' size=1>[");
		Text = Text.replace("]", "]</font>");
		Text = Text.replace("\r", "</font><br><font face=\"tahoma\" size = 2>");
		Text = Text.replace("> ::", "<span style='color:navy'>");
		
		Text = AdjustParameter(Text, "γ", "اطمینان", IsQualifyNumber);
		Text = AdjustParameter(Text, "φ", "بسامد", IsQualifyNumber);
		Text = AdjustParameter(Text, "τ", "نوعیت", IsQualifyNumber);
		Text = AdjustParameter(Text, "σ", "شباهت", IsQualifyNumber);
		Text = AdjustParameter(Text, "δ", "غلبه", IsQualifyNumber);
		Text = AdjustParameter(Text, "α", "احتمال", IsQualifyNumber);
		Text = AdjustParameter(Text, "β", "احتمال معکوس", IsQualifyNumber);
		Text = AdjustParameter(Text, "µa", "گوناگونی آرگومان", IsQualifyNumber);
		Text = AdjustParameter(Text, "µr", "گوناگونی ارجاع", IsQualifyNumber);
		Text = AdjustParameter(Text, "ξ", "ترادف", IsQualifyNumber);
		
		/*Text = AdjustParameter(Text, "γ", "Certainty", IsQualifyNumber);
		Text = AdjustParameter(Text, "φ", "Frequency", IsQualifyNumber);
		Text = AdjustParameter(Text, "τ", "Typicality", IsQualifyNumber);
		Text = AdjustParameter(Text, "σ", "Similarity", IsQualifyNumber);
		Text = AdjustParameter(Text, "δ", "Dominance", IsQualifyNumber);
		Text = AdjustParameter(Text, "α", "Likelihood", IsQualifyNumber);
		Text = AdjustParameter(Text, "β", "Reverse Likelihood", IsQualifyNumber);
		Text = AdjustParameter(Text, "µa", "argument Multiplicity", IsQualifyNumber);
		Text = AdjustParameter(Text, "µr", "referent Multiplicity",	IsQualifyNumber);
		Text = AdjustParameter(Text, "ξ", "Synonymy", IsQualifyNumber);*/

		Text = Text.replace(" CX ", " <font color=maroon> از لحاظ </font>");
		Text = Text.replace("≠", "<font color=red>≠</font>");
		Text = Text.replace("(", "<font color=maroon>(</font>");
		Text = Text.replace(")", "<font color=maroon>)</font>");
		Text = Text.replace("{", "<font color=maroon>{</font>");
		Text = Text.replace("}", "<font color=maroon>}</font>");
		Text = Text.replace(",", "<font color=maroon>,</font>");

		Text = "<font face=\"tahoma\" size = 2>" + Text + "<font>";

		return Text;
	}
		
	
	@Override
	public boolean equals(Object object)
	{
		PlausibleAnswer other = (PlausibleAnswer)object;
		
		if (other.answer == this.answer && 
			other.parameters.certainty == this.parameters.certainty && 
			other.isNegative == this.isNegative)
			return true;
		
		return false;
	}
	
	@Override public int hashCode()
	{
		return (int) (this.answer.hashCode() + this.parameters.certainty * 1000000 + ((this.isNegative)?1:0) );
	}
	
	
	private String tagPlausibleRelations(String text)
	{
		text = text.replace(" INVERSE ",		insertSnippet(" معکوس "));
		text = text.replace(" ISA ",			insertSnippet(" نوعی است از "));
		text = text.replace(" SIM ",			insertSnippet(" شبیه است به "));
		text = text.replace(" DIS ",			insertSnippet(" شبیه نیست به "));
		text = text.replace(" (+)-> ",			insertSnippet(" تاثیر مثبت دارد روی "));
		text = text.replace(" DEP+ ",			insertSnippet(" تاثیر مثبت دارد روی "));
		text = text.replace(" (-)-> ",			insertSnippet(" تاثیر منفی دارد روی "));
		text = text.replace(" DEP- ",			insertSnippet(" اثیر منفی دارد روی "));
		text = text.replace(" -- DEPX --> ",	insertSnippet(" تاثیری دارد روی "));
		text = text.replace(" DEP ",			insertSnippet(" تاثیری دارد روی "));
		text = text.replace("*IF* ",			insertSnippet("اگر "));
		text = text.replace(" *THEN* ",			insertSnippet(" آنگاه "));
		text = text.replace(" SYN ",			insertSnippet(" مترادف "));
		text = text.replace(" CAUSES ",			insertSnippet(" باعث "));
		text = text.replace(" ATTRIBUTES ",		insertSnippet(" مشخصات "));
		text = text.replace(" ENTAILS ",		insertSnippet(" حکم می کند "));
		text = text.replace(" DISAMBIGUATES TO ",		insertSnippet(" قابل ابهاد زدایی است به "));
		text = text.replace(" AMBIGUATES TO ",		insertSnippet(" قابل ابهام زایی است به "));

		text = text.replace(" DEPENDS ON ",insertSnippet(" وابسته است به "));
		text = text.replace(" OR ",insertSnippet(" یا "));
		text = text.replace(" AND ",insertSnippet(" و "));
		text = text.replace("(UNVERIFIED CONDITION)", "<font color=red>(برای خودم فرض کردم)</font>");
		
		text = text.replace("GLOSS", "تعریف");
		text = text.replace("PART-OF", "جزئی است از");
		text = text.replace("HAS-PART", "جزئی دارد");
		text = text.replace("INS", "نمونه ای است از");
		text = text.replace("EXAMPLE", "مثال");
		text = text.replace("IS", "هست");
		text = text.replace("ATTRIBUTE", "ویژگی");
		
		return text;
	}
	
	private String tagReferences(String text)
	{
		text = text.replace(" ~ SOURCE = ",	"<font size=1>" + insertSnippet("<br> منبع: ", "green"));
		text = text.replace(", REFERENCES = ", insertSnippet("<br> ارجاع: ", "green"));
		
		return text;
	}
	
	private String insertSnippet(String Text)
	{
		return insertSnippet(Text, "black");
	}
	private String insertSnippet(String Text, String Color)
	{
		return "<b><span style='color:" + Color + "'>" + Text + "</span></b>";
	}
	
	public static ArrayList<PlausibleAnswer> mergeUnique(ArrayList<PlausibleAnswer> candidates)
	{
		//Common.log("Merging non-unique answers ... ");
		
		HashMap<String, PlausibleAnswer> map = new HashMap<String, PlausibleAnswer>();
		
		int i = 0;
		
		for (PlausibleAnswer candidate: candidates)
		{
			PlausibleAnswer probe = map.get(candidate.answer.getName());
			
			if (probe != null)
			{
				probe.parameters.certainty = Math.max(candidate.parameters.certainty, probe.parameters.certainty);
				
				//if (i++ < 5)
					//Common.log("\tremoved repetitive answer '" + probe.answer.getName() + "'");
			}
			else
			{
				map.put(candidate.answer.getName(), candidate);
			}
		}
		
		ArrayList<PlausibleAnswer> outs = new ArrayList<PlausibleAnswer>(map.values());
		
		Collections.sort(outs);
		
		//Common.log("Merged " + candidates.size() + " answers to " + outs.size());
		
		return outs;
	}
	
	@Override
	protected PlausibleAnswer clone()
	{
		PlausibleAnswer newAnswer = new PlausibleAnswer(this.answer);
		
		newAnswer.isNegative = this.isNegative;
		newAnswer.parameters = new CertaintyParameters(this.parameters);
		
		//TODO: not complete!!!
		
		return newAnswer;
	}
	
	public PlausibleAnswer makeCopy(String newName)
	{
		Node newNode = answer.makeCopy(newName);
		
		PlausibleAnswer newAnswer = new PlausibleAnswer(newNode);
		
		newAnswer.parameters = new CertaintyParameters(this.parameters);
		
		return newAnswer;
	}
}
