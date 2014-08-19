package ir.ac.itrc.qqa.semantic.reasoning;

import java.util.HashSet;

/**
 * implements each element in the reasoning history
 * @author Ehsan Darrudi
 *
 */
public class HistoryEmement
{
	/** search key for this element to be used in the history hash table*/
	String searchKey = "";
	
	/** link to the next element */
	HistoryEmement nextHistoryElement = null;
	
	/** number of reasoning lines in this element */
	int reasoningLineNum = 0;
	
	/** the maximum number of reasoning steps (lines) allowed */
	final int maxReasningLineNum = 30;
	
	/** the container for reasoning lines */
	private String[] reasoningLine = new String[maxReasningLineNum];
	
	private HashSet<String> inferences = new HashSet<String>(); 

	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

	/**
	 * adds a reasoning line to this element (reasoning step)
	 * 
	 *  @param statement the statement we're working on
	 *  @param certainty the certainty in that statement
	 *  @return true if successful, false otherwise
	 */
	public void pushReasningLine(String statement, String certainty, String reference)
	{
		if (reasoningLineNum == maxReasningLineNum)
		{
			return;
		}
		
		reasoningLine[reasoningLineNum] = statement;

		if (!certainty.isEmpty())
		{
			reasoningLine[reasoningLineNum] += " : " + certainty; 
		}
		
		if (!reference.isEmpty())
		{
			reasoningLine[reasoningLineNum] += " ~ " + reference;
		}
		
		reasoningLineNum++;
	}
	
	
	/**
	 * composes all reasoning lines associated with this history element
	 * @return the composed reasoning lines
	 */
	public String GetReasoningLines()
	{
		String Out = "";

		for(int i = 0; i < reasoningLineNum; i++)
		{
			Out += reasoningLine[i] + "\r";	
		}

		return Out;
	}

	/**
	 * pops a number of reasoning lines from this history element
	 * @param linesNum the number of lines to be removed
	 * @return true if successful, false otherwise
	 */
	public boolean popReasoningLine(int linesNum)
	{
		if (reasoningLineNum - linesNum < 0)
		{
			return false;
		}

		reasoningLineNum -= linesNum;

		return true;
	}
}