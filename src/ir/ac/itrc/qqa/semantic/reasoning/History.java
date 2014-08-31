package ir.ac.itrc.qqa.semantic.reasoning;

import ir.ac.itrc.qqa.semantic.kb.Node;
import ir.ac.itrc.qqa.semantic.util.MyError;
import ir.ac.itrc.qqa.semantic.util.Common;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * implements a simple history to keep track of reasoning steps and to avoid getting trapped in a reasoning loop 
 * @author Ehsan Darrudi
 *
 */
public class History
{
	/** the link-list pointer to the last history element */
	private HistoryEmement lastNodeInHistory;

	/** a hash to keep track of all elements in the history */
	private Hashtable<String, HistoryEmement> epochs = new Hashtable<String, HistoryEmement>();
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * pushes a reasoning step into history
	 * @param inference the inference
	 * @param descriptor 
	 * @param argument
	 * @param referent
	 */
	public void pushHistory(String inference, Node descriptor, Node argument, Node referent)
	{
		String key = composeSearchKey(inference, descriptor, argument, referent);

		HistoryEmement he = new HistoryEmement();

		he.searchKey = key;

		he.nextHistoryElement = lastNodeInHistory;

		lastNodeInHistory = he;

		epochs.put(key, he);
	}

	/**
	 * pops a reasoning step from the history
	 * @param inference the inference name
	 * @param pq plausible question
	 */
	public void popHistory(String inference, PlausibleQuestion pq)
	{
		String key = composeSearchKey(inference, pq.descriptor, pq.argument, pq.referent);

		HistoryEmement HE = (HistoryEmement)epochs.get(key);
		MyError.assertNotNull(HE);

		if (HE != lastNodeInHistory)
		{
			MyError.exit("You tried to pop a node from history stack which wasn't the last one!");
		}
		
		MyError.assertNotNull(lastNodeInHistory);

		HistoryEmement Temp = lastNodeInHistory.nextHistoryElement;

		epochs.remove(key);
		
		lastNodeInHistory = Temp;
	}
	
	/**
	 * checks if a reasoning step exists in the history
	 * @param inference 
	 * @param DESCRIPTOR
	 * @param argument
	 * @param referent
	 * @return true/false
	 */
	public boolean isInHistory(String inference, Node descriptor, Node argument, Node referent)
	{
		String Key = composeSearchKey(inference, descriptor, argument, referent);
		 
		HistoryEmement HE = epochs.get(Key);
		
		if (HE != null)
		{
			return true;	
		}

		return false;
	}
	
	/**
	 * generates a string from the content of the history
	 * @return the content
	 */
	public String composeHistory()
	{
		String out = "";

		HistoryEmement temp = lastNodeInHistory;

		while (temp != null)
		{
			out += temp.searchKey + " <-- ";

			temp = temp.nextHistoryElement;
		}

		out += "START";

		return out;
	}

	/**
	 *  generates a brief string representation from the content of the history
	 * @return the content 
	 */
	public String composeHistoryBrief()
	{
		String out = "";
		String text;
		int pos;

		ArrayList<String> tempStrings = new ArrayList<String>();

		HistoryEmement temp = lastNodeInHistory;

		while (temp != null)
		{
			pos = temp.searchKey.indexOf("[");
            			
			text = Common.removeSubstring(temp.searchKey, pos, temp.searchKey.length() - pos);
			
			if (text != "RECALL")
			{
				tempStrings.add(text);
			}
			
			temp = temp.nextHistoryElement;
		}

		for (int i = tempStrings.size() - 1; i >= 0; i--)
		{
			text = (String)tempStrings.get(i);

			out += text;
			
			if (i != 0)
			{
				out += "|";
			}
		}


		return out;
	}

	/**
	 * pushes a reasoning line to the current reasoning step 
	 * @param statement reasoning line
	 * @param certainty the certainty associated with this reasoning step 
	 * @return true if successful, false otherwise
	 */
	public void pushReasoningLine(String statement, String certainty, String reference)	
	{		
		lastNodeInHistory.pushReasningLine(statement, certainty, reference);
	}
	
	/**
	 * pops a number of reasoning lines from the current reasoning step
	 * @param LinesNum number of lines to be removed
	 * @return true if successful, false otherwise
	 */
	public boolean popReasoningLine(int LinesNum)
	{
		return lastNodeInHistory.popReasoningLine(LinesNum);
	}

	/**
	 * composes all the reasoning line (justification) for the current reasoning step
	 * @return composed justification
	 */
	public String getReasoningLines()
	{
		String Out = "";
		String ReasningLine = "";
		HistoryEmement Temp = lastNodeInHistory;

		//String[] Buffer = new String[100];

		while (Temp != null)
		{
			ReasningLine = Temp.GetReasoningLines();

			if (ReasningLine != "")
			{
				Out += ReasningLine;
				Out += "\r";
			}

			Temp = Temp.nextHistoryElement;
		}

		return Out;	
	}

	/**
	 * used internally to compose a search key for the history hashtable
	 * @param inference 
	 * @param DESCRIPTOR
	 * @param argument
	 * @param referent
	 * @return the composed search key
	 */
	private String composeSearchKey(String inference, Node descriptor, Node argument, Node referent)
	{
		MyError.assertNotNull(descriptor);
		
		String key;

		if (referent == null && argument == null)
		{
			key = inference + "[" + descriptor.getName() + "]";
		}
		else if (referent == null && argument != null)
		{
			key = inference + "[" + descriptor.getName() + "(" + argument.getName() + ")={?}]";
		}
		else if (referent != null && argument == null)
		{
			key = inference + "[" + descriptor.getName() + "(?)={" + referent.getName() +  "}]";
		}
		else
		{
			// None is null;
			key = inference + "[" + descriptor.getName() + "(" + argument.getName() + ")={" + referent.getName() +  "}]";
		}

		return key;
	}
	
	/**
	 * by looking into history checks whether a GEN-SPEC inference chain has been occurred over siblings.
	 * most of the time this kind of inference chain yields invalid answers. 
	 * e.g. Live(Penguin)={Arctic}, ISA(Penguin)={Bird}, ISA(Sparrow)={Bird} => Live(Sparrow)={Arctic}!
	 * thus, in these situations the reasoning engine reduces the certainty of the answer dramatically.   
	 * @param Function
	 * @return true/false
	 */
	public boolean doesGenSpecTurnOver(String Function)
	{
		HistoryEmement Temp = lastNodeInHistory;

		while (Temp != null)
		{
			if (Temp.searchKey.startsWith(Function))
			{
				return true;
			}

			Temp = Temp.nextHistoryElement;
		}

		return false;
	}
}

