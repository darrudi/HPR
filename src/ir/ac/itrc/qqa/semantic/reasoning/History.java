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
	private Hashtable<String, HistoryEmement> historyStorage = new Hashtable<String, HistoryEmement>();
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * pushes a reasoning step into history
	 * @param inference the inference
	 * @param descriptor 
	 * @param argument
	 * @param referent
	 */
	public void PushHistory(String inference, Node descriptor, Node argument, Node referent)
	{
		String Key = ComposeSearchKey(inference, descriptor, argument, referent);

		HistoryEmement he = new HistoryEmement();

		he.SearchKey = Key;

		he.NextHistoryElement = lastNodeInHistory;

		lastNodeInHistory = he;

		historyStorage.put(Key, he);
	}

	/**
	 * pops a reasoning step from the history
	 * @param inference the inference name
	 * @param pq plausible question
	 */
	public void PopHistory(String inference, PlausibleQuestion pq)
	{
		String Key = ComposeSearchKey(inference, pq.descriptor, pq.argument, pq.referent);

		HistoryEmement HE = (HistoryEmement)historyStorage.get(Key);
		MyError.assertNotNull(HE);

		if (HE != lastNodeInHistory)
		{
			MyError.exit("You tried to pop a node from history stack which wasn't the last one!");
		}
		
		MyError.assertNotNull(lastNodeInHistory);

		HistoryEmement Temp = lastNodeInHistory.NextHistoryElement;

		historyStorage.remove(Key);
		
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
	public boolean IsInHistory(String inference, Node descriptor, Node argument, Node referent)
	{
		String Key = ComposeSearchKey(inference, descriptor, argument, referent);
		 
		HistoryEmement HE = historyStorage.get(Key);
		
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
	public String ComposeHistory()
	{
		String Out = "";

		HistoryEmement Temp = lastNodeInHistory;

		while (Temp != null)
		{
			Out += Temp.SearchKey + " <-- ";

			Temp = Temp.NextHistoryElement;
		}

		Out += "START";

		return Out;
	}

	/**
	 *  generates a brief string representation from the content of the history
	 * @return the content 
	 */
	public String ComposeHistoryBrief()
	{
		String Out = "";
		String Text;
		int Pos;

		ArrayList<String> TempStrings = new ArrayList<String>();

		HistoryEmement Temp = lastNodeInHistory;

		while (Temp != null)
		{
			Pos = Temp.SearchKey.indexOf("[");
            			
			Text = Common.removeSubstring(Temp.SearchKey, Pos, Temp.SearchKey.length() - Pos);
			
			if (Text != "RECALL")
			{
				TempStrings.add(Text);
			}
			
			Temp = Temp.NextHistoryElement;
		}

		for (int i = TempStrings.size() - 1; i >= 0; i--)
		{
			Text = (String)TempStrings.get(i);

			Out += Text;
			
			if (i != 0)
			{
				Out += "|";
			}
		}


		return Out;
	}

	/**
	 * pushes a reasoning line to the current reasoning step 
	 * @param Statement reasoning line
	 * @param Certainty the certainty associated with this reasoning step 
	 * @return true if successful, false otherwise
	 */
	public void pushReasoningLine(String Statement, String Certainty, String Reference)
	{
		lastNodeInHistory.AddReasningLine(Statement, Certainty, Reference);
	}
	
	/**
	 * pops a number of reasoning lines from the current reasoning step
	 * @param LinesNum number of lines to be removed
	 * @return true if successful, false otherwise
	 */
	public boolean popReasoningLine(int LinesNum)
	{
		return lastNodeInHistory.PopReasoningLine(LinesNum);
	}

	/**
	 * composes all the reasoning line (justification) for the current reasoning step
	 * @return composed justification
	 */
	public String GetReasoningLines()
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

			Temp = Temp.NextHistoryElement;
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
	private String ComposeSearchKey(String inference, Node descriptor, Node argument, Node referent)
	{
		MyError.assertNotNull(descriptor);
		
		String Key;

		if (referent == null && argument == null)
		{
			Key = inference + "[" + descriptor.getName() + "]";
		}
		else if (referent == null && argument != null)
		{
			Key = inference + "[" + descriptor.getName() + "(" + argument.getName() + ")={?}]";
		}
		else if (referent != null && argument == null)
		{
			Key = inference + "[" + descriptor.getName() + "(?)={" + referent.getName() +  "}]";
		}
		else
		{
			// None is null;
			Key = inference + "[" + descriptor.getName() + "(" + argument.getName() + ")={" + referent.getName() +  "}]";
		}

		return Key;
	}
	
	/**
	 * by looking into history checks whether a GEN-SPEC inference chain has been occurred over siblings.
	 * most of the time this kind of inference chain yields invalid answers. 
	 * e.g. Live(Penguin)={Arctic}, ISA(Penguin)={Bird}, ISA(Sparrow)={Bird} => Live(Sparrow)={Arctic}!
	 * thus, in these situations the reasoning engine reduces the certainty of the answer dramatically.   
	 * @param Function
	 * @return true/false
	 */
	public boolean DoesGenSpecTurnOver(String Function)
	{
		HistoryEmement Temp = lastNodeInHistory;

		while (Temp != null)
		{
			if (Temp.SearchKey.startsWith(Function))
			{
				return true;
			}

			Temp = Temp.NextHistoryElement;
		}

		return false;
	}
}

