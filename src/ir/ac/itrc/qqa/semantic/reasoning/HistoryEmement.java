package ir.ac.itrc.qqa.semantic.reasoning;

/**
 * implements each element in the reasoning history
 * @author Ehsan Darrudi
 *
 */
public class HistoryEmement
{
	/** search key for this element to be used in the history hash table*/
	String SearchKey = "";
	
	/** link to the next element */
	HistoryEmement NextHistoryElement = null;
	
	/** number of reasoning lines in this element */
	int ReasoningLineNum = 0;
	
	/** the maximum number of reasoning steps (lines) allowed */
	final int MaxReasningLineNum = 30;
	
	/** the container for reasoning lines */
	String[] ReasoningLine = new String[MaxReasningLineNum];

	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	/**
	 * adds a reasoning line to this element (reasoning step)
	 * 
	 *  @param statement the statement we're working on
	 *  @param certainty the certainty in that statement
	 *  @return true if successful, false otherwise
	 */
	public void AddReasningLine(String statement, String certainty, String reference)
	{
		if (ReasoningLineNum == MaxReasningLineNum)
		{
			return;
		}
		
		ReasoningLine[ReasoningLineNum] = statement;

		if (!certainty.isEmpty())
		{
			ReasoningLine[ReasoningLineNum] += " : " + certainty; 
		}
		
		if (!reference.isEmpty())
		{
			ReasoningLine[ReasoningLineNum] += " ~ " + reference;
		}
		
		ReasoningLineNum++;
	}

	/**
	 * composes all reasoning lines associated with this history element
	 * @return the composed reasoning lines
	 */
	public String GetReasoningLines()
	{
		String Out = "";

		for(int i = 0; i < ReasoningLineNum; i++)
		{
			Out += ReasoningLine[i] + "\r";	
		}

		return Out;
	}

	/**
	 * pops a number of reasoning lines from this history element
	 * @param LinesNum the number of lines to be removed
	 * @return true if successful, false otherwise
	 */
	public boolean PopReasoningLine(int LinesNum)
	{
		if (ReasoningLineNum - LinesNum < 0)
		{
			return false;
		}

		ReasoningLineNum -= LinesNum;

		return true;
	}
}

