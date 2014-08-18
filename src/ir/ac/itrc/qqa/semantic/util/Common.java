package ir.ac.itrc.qqa.semantic.util;

import ir.ac.itrc.qqa.semantic.Pos;
import ir.ac.itrc.qqa.semantic.enums.LexicalType;
import ir.ac.itrc.qqa.semantic.enums.Colors;
import ir.ac.itrc.qqa.semantic.enums.DirectoryEntryType;
import ir.ac.itrc.qqa.semantic.enums.LogMode;
import ir.ac.itrc.qqa.semantic.enums.POS;
import ir.ac.itrc.qqa.semantic.enums.PosCoarse;
import ir.ac.itrc.qqa.semantic.enums.PosFine;
import ir.ac.itrc.qqa.semantic.enums.PosNumber;
import ir.ac.itrc.qqa.semantic.enums.PosPerson;
import ir.ac.itrc.qqa.semantic.enums.PosPolarity;
import ir.ac.itrc.qqa.semantic.enums.PosTense;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This class implements simple text processing and utility tasks.
 * @author Ehsan Darrudi
 *
 */
public class Common
{
	private static LogMode _logMode = LogMode.BRIEF;
	
	private static KnowledgeBase _kb = null;
	
	/** External log file */
	private static BufferedWriter _logFile = null;
	
	private static int _uniqueNumber = 0;
	
	private static final String[] _yakan 	= {"صفر", "يک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه"};  
	private static final String[] _dahgan 	= {"", "", "بيست", "سي", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"}; 
	private static final String[] _dahyek 	= {"ده", "يازده", "دوازده", "سيزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"};
	private static final String[] _sadgan 	= {"", "يکصد", "دويست", "سيصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد"};
	private static final String[] _basex 	= {"", "هزار", "ميليون", "ميليارد", "تريليون"};
	
	//~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=
	
	/**
	 * Simulates string.remove() in C#
	 * @param text original string
	 * @param startPosition start position
	 * @param length the number of characters to be removed
	 * @return the altered string
	 */
	public static String removeSubstring(String text, int startPosition, int length)
	{
		if (length == 0)
			return text;
		
		String out = "";
		
		if (startPosition != 0)
			out = text.substring(0, startPosition);
			
		return  out + text.substring(startPosition + length);	
	}


	/**
	 * Simulates C# string.insert() function
	 * @param original original string
	 * @param startPosition start position
	 * @param insert the string to be shoved in
	 * @return altered string
	 */
	public static String insertSubstring(String original, int startPosition, String insert)
	{
		return original.substring(0, startPosition) + insert + original.substring(startPosition);
	}

	/**
	 * Removes parenthesis in a string
	 * @param Text original string
	 * @return altered string 
	 */
	public static String removeParenthesis(String text)
	{
		int pos = text.indexOf('(');
		
		if (pos == -1)
			return text;
		
		Stack<Integer> stack = new Stack<Integer>();
		
		pos = 0;
		
		while (pos < text.length())
		{
			if (text.charAt(pos) == '(')
			{
				stack.push(pos);
			}
			else if (text.charAt(pos) == ')')
			{
				if (stack.isEmpty())
					return text;
				
				Integer start = stack.pop();
				
				text = text.substring(0, start) + text.substring(pos + 1);
				
				pos = start;
				
				continue;
			}
			
			pos++;
		}
		
		return text.replace("  ", " ").trim();
	}
	
	private static int nextPos(String text, int start)
	{
		if (start == text.length())
			return -1;
		
		int posLeft = text.indexOf('(', start);
		int posRight = text.indexOf(')', start);
		
		if (posLeft * posRight < 0)
			return Math.max(posLeft, posRight);
		else
			return Math.min(posLeft, posRight);
	}
	
	public static String removeParenthesisWithException(String text, String... exceptions)
	{
		HashSet<String> exceptionHash = new HashSet<String>(Arrays.asList(exceptions));
		
		Pattern pattern = Pattern.compile("(\\([^\\)]*\\))");
		Matcher matcher = pattern.matcher(text);
		
		String modified = text;
		
		while (matcher.find())
		{
			String inside = matcher.group(1);
			inside = inside.substring(1, inside.length() - 2).trim();
			
			if (!exceptionHash.contains(inside))
			{
				modified = modified.replace(text.substring(matcher.start(), matcher.end()), "");
			}
		}
		
		return modified.replace("  ", " ").trim();
	}

	
	public static String trimRelationName(String text)
	{
		text = removeParenthesis(text);
		
		if (text.charAt(0) == '*')
			text = text.substring(1).trim();
		
		return text;
	}
	
	public static String trimNodeName(String text)
	{
//		if (text.length() > 20)
//		{
//			text = text.substring(0, 20) + "<br>" + text.substring(20);
//		}
		
		return text;
	}

	/**
	 * Extracts the string contained inside parenthesis
	 * @param Text the source string
	 * @return the string inside parenthesis
	 */
	public static String extractInsideParenthesis(String Text)
	{
		int Position1 = Text.indexOf("(");

		if (Position1 == -1)
		{
			return "";
		}

		int Position2 = Text.lastIndexOf(")");

		if (Position1 > Position2)
		{
			return "";
		}

		Text = Text.substring(Position1 + 1, Position2 - 1);

		return Text;
	}

	/**
	 * Capitalizes all words in a string
	 * @param text the original string
	 * @return altered string
	 */
	public static String capitalizeAllWords(String text)
	{
		String[] Split = text.split("_");

		String Out = "";

		for (int i=0; i < Split.length; i++)
		{
			Out += capitalize(Split[i]);

			if (i < Split.length)
			{
				Out += "_";
			}
		}

		return Out;
	}

	/**
	 * Capitalize on single word
	 * @param word the word
	 * @return capitalized word
	 */
	public static String capitalize(String word)
	{
		String FirstLetter = word.substring(0, 1);
		return FirstLetter.toUpperCase() + word.substring(1);
	}

	/**
	 * Determines the Part-of-Speech integer code based on a string code.
	 * used only when parsing WordNet data files
	 * @param ss_type the POS
	 * @return the POS code
	 */
	public static int getPosNumber(String ss_type)
	{
		switch (ss_type.charAt(0))
		{
			case 'o' : return 0;	// other
			case 'n' : return 1;	// noun
			case 'v' : return 2;	// verb
			case 'a' : return 3;	// adjective
			case 'r' : return 4;	// adverb
			case 's' : return 5;	// Satellite adjective
			default  : return -1;	// error
		}
	}
	
	/**
	 * Determines the POS tag of a sense based on special characters ('#' delimiter) in it.
	 * @param sense The word to be processed
	 * @return The part-of-speech tag
	 */
	public static POS convertSingleCharStringToPos(String sense)
	{
		int index = sense.indexOf("#");
		
		if (index == sense.length() - 1)
			return POS.ANY;
		
		switch (sense.charAt(index + 1))
		{
			case 'n' : return POS.NOUN;	// noun
			case 'v' : return POS.VERB;	// verb
			case 'a' : return POS.ADJECTIVE;	// adjective
			case 'r' : return POS.ADVERB;	// adverb
			case 's' : return POS.SETELLITE_ADJECTIVE;	// Satellite adjective
			case 'u' : return POS.ANY;	// Unknown
			default  : MyError.exit("Invalid POS character!");
		}
		
		return POS.ANY;
	}
	
	/**
	 * Maps the POS tags from strings to <code>POS</code> class 
	 * @param str String representation of a POS
	 * @return the POS-class mapped pos
	 */
	public static POS convertStringToPos(String str)
	{
		if (str.equals("Noun"))
			return POS.NOUN;
		else if (str.equals("Adverb"))
			return POS.ADVERB;
		else if (str.equals("Adjective"))
			return POS.ADJECTIVE;
		else if (str.equals("Verb"))
			return POS.VERB;
		else
			MyError.exit("Invalid POS!");
		
		return POS.ANY;
	}
	
	/**
	 * Converts POS tags to single character strings
	 * @param pos The pos tag
	 * @return The single character representation of pos
	 */
	public static String convertPosToSingleCharString(POS pos)
	{
		switch (pos)
		{
			case NOUN				: return "n";
			case ADJECTIVE			: return "a";
			case ADVERB				: return "r";
			case VERB				: return "v";
			case SETELLITE_ADJECTIVE: return "a";			
		}
		
		MyError.exit("Invalid POS `" + pos + "`!");
		return "";
	}
	
	/**
	 * Produces the abbreviated form of a pos string
	 * @param pos The full pos string
	 * @return The abbreviated single character pos
	 */
	public static String convertPosToSingleCharString(String pos)
	{
		pos = pos.toLowerCase();
		
		if (pos.equals("noun"))
			return "n";
		else if (pos.equals("adverb"))
			return "r";
		else if (pos.equals("adjective"))
			return "a";
		else if (pos.equals("verb"))
			return "v";
		else
			MyError.exit("Invalid POS `" + pos + "`!");
		
		return "";
	}

	/**
	 * Determines the POS string code based on an integer code
	 * used only when parsing WordNet data files
	 * 
	 * @param posCode the POS code
	 * @return the POS
	 */
	public static String convertIntegerToSingleCharStringPos(int posCode)
	{
		switch (posCode)
		{
			case 1 : return "n";
			case 2 : return "v";
			case 3 : return "a";
			case 4 : return "r";
			case 5 : return "s";
		}

		MyError.exit("Invalid Part-Of-Speech!");
		return "";
	}
	
	/**
	 * 
	 * @param posNum 
	 * @return POS tag
	 */
	public static POS convertIntegerToPos(int posNum)
	{
		switch (posNum)
		{
			case 0 : return POS.ANY;
			case 1 : return POS.NOUN;
			case 2 : return POS.VERB;
			case 3 : return POS.ADJECTIVE;
			case 4 : return POS.ADVERB;
			case 5 : return POS.SETELLITE_ADJECTIVE;
		}

		MyError.exit("Invalid Part-Of-Speech!");
		return POS.ANY;
	}
	
	public static LexicalType getCategoryFromInteger(int catNum)
	{
		switch (catNum)
		{
			case 0 : return LexicalType.ANY;
			case 1 : return LexicalType.SENSE;
		}

		MyError.exit("Invalid LexicalType!");
		
		return LexicalType.ANY;
	}

	/**
	 * produces a fuzzy (qualitative) representation of a certainty parameter
	 * @param NumberText the parameter (in text)
	 * @return the fuzzy representation
	 */
	public static String qualifyNumber(String NumberText)
	{
		float Number = Float.parseFloat(NumberText);
		
		return qualifyNumber(Number);
	}

	/**
	 * produces a fuzzy (qualitative) representation of a certainty parameter
	 * @param Number the parameter
	 * @return the fuzzy representation
	 */
	public static String qualifyNumber(float Number)
	{
		if (Number == 0)
		{
			return "Zero";
		}
		else if (Number <= 0.05)
		{
			return "Just about zero";
		}
		else if (Number <= 0.2)
		{
			return "Very weak";
		}
		else if (Number <= 0.4)
		{
			return "Weak";
		}
		else if (Number <= 0.6)
		{
			return "Average";
		}
		else if (Number <= 0.8)
		{
			return "Strong";
		}
		else if (Number < 0.99)
		{
			return "Very Strong";
		}
		else
		{
			return "Sure";
		}
	}

	/**
	 * removes sense information from a composite string.
	 * it simply removes the '#' character and all other character after sense names in a string.
	 * @param Text the string
	 * @return the string without sense information
	 */
	public static String removeSenseInfos(String Text)
	{
		int Pos = Text.indexOf("#");
		while (Pos != -1)
		{

			if (Pos + 1 >= Text.length() || getPosNumber(removeSubstring(Text, 0, Pos + 1)) == -1)
			{
				return Text;
			}

			int Counter = 0;
			while (Pos + 2 + Counter < Text.length() && isNumber(Text.charAt(Pos + 2 + Counter)))
			{
				Counter++;
			}

			Text = removeSubstring(Text, Pos, 2 + Counter);

			Pos = Text.indexOf("#");
		}
		
		return Text;
	}

	/**
	 * determines if a character is digit or not
	 * @param Letter the character
	 * @return true/false
	 */
	public static boolean isNumber(char Letter)
	{
		if (Letter >= '0' && Letter <= '9')
			return true;
		else
			return false;
	}
	
	/**
	 * trims (beginning and end of) strings from all characters provided
	 * @param source input string
	 * @param paddings characters to be removed
	 * @return trimmed string
	 */
	public static String trimAll(String source, char[] paddings)
	{
		String pads = String.copyValueOf(paddings);
		
		return trimAll(source, pads);
	}
	public static String trimAll(String source, String paddings)
	{
		String quote = Pattern.quote(paddings);
		
		String regex = "[" + quote + "]*$|^[" + quote + "]*";
		
		return source.replaceAll(regex, "");
	}
	
	/**
	 * removes some non-standard characters from a string
	 * @param text input text
	 * @return cleaned text
	 */
	public static String cleanText(String text)
	{
		if (text != null)
		{
			text = text.replaceAll("&AMP;", "&");
			text = text.replaceAll("_", " ");
			text = text.replaceAll("  ", " ");
		}
		
		return text;
	}
	
	/**
	 * generates a text representation of contextual info
	 * @param CXs contextual nodes
	 * @return a text representation
	 */
	public static String composeCX(ArrayList<PlausibleAnswer> CXs)
	{
		if (CXs == null || CXs.size() == 0)
		{
			return "";
		}

		String Out = ", CX(";
		int Num = 0;

		for (PlausibleAnswer CX: CXs)
		{
			Num++;

			Out += CX.answer.getName();

			if (Num != CXs.size())
			{
				Out +=" AND ";
			}
		}

		Out += ")";
		
		return Out;
	}
	
	public static String composeCXHumanReadable(ArrayList<PlausibleAnswer> CXs)
	{
		if (CXs == null || CXs.size() == 0)
		{
			return "";
		}

		String Out = ", از نظر (";
		int Num = 0;

		for (PlausibleAnswer CX: CXs)
		{
			Num++;

			Out += CX.answer.getName();

			if (Num != CXs.size())
			{
				Out +=" و ";
			}
		}

		Out += ")";
		
		return Out;
	}
	
	/**
	 * Checks if an ArrayLis is null or empty
	 * @param AL the list
	 * @return true/false
	 */
	public static boolean isEmpty(ArrayList AL)
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
	 * prints to the standard output
	 */
	public static void print()
	{
		System.out.println("");		
	}
	public static void print(String msg)
	{
		System.out.println(msg);		
	}
	public static void print(ArrayList array)
	{
		System.out.println(array);
	}
	public static void printInline(String msg)
	{
		System.out.print(msg);		
	}
	public static void printError(String msg)
	{
		System.err.println(msg);		
	}
	
	/**
	 * Color on the text terminal can be produced using the "ANSI escape sequences". For example:
	 * 
	 * echo -e "\033[44;37;5m ME \033[0m COOL"
	 * 
	 * The above sets the background to blue, foreground white, blinking video, and prints " ME ", then resets the terminal back to defaults and prints " COOL". The "-e" is an option specific to the echo command--it enables the interpretations of the special characters. The "\033[" introduces the escape sequence. The "m" means "set attribute" and thus finishes the sequence. The actual codes in the example above are "44;37;5" and "0".
	 * 
	 * Change the "44;37;5" to produce different color combinations--the number/order of codes do not matter. The codes to choose from are listed below:
	 * 
	 * Code Action/Color
	 * 
	 * ---------------------------
	 * 
	 * 0 reset all attributes to their defaults
	 * 1 set bold
	 * 2 set half-bright (simulated with color on a color display)
	 * 4 set underscore (simulated with color on a color display)
	 * 5 set blink
	 * 7 set reverse video
	 * 22 set normal intensity
	 * 24 underline off
	 * 25 blink off
	 * 27 reverse video off
	 * 30 set black foreground
	 * 31 set red foreground
	 * 32 set green foreground
	 * 33 set brown foreground
	 * 34 set blue foreground
	 * 35 set magenta foreground
	 * 36 set cyan foreground
	 * 37 set white foreground
	 * 
	 * 38 set underscore on, set default foreground color
	 * 39 set underscore off, set default foreground color
	 * 
	 * 40 set black background
	 * 41 set red background
	 * 42 set green background
	 * 43 set brown background
	 * 44 set blue background
	 * 45 set magenta background
	 * 46 set cyan background
	 * 47 set white background
	 * 
	 * 49 set default background color
	 * 
	 * Other interesting codes:
	 * 
	 * \033[2J clear screen
	 * \033[0q clear all keyboard LEDs (won't work from Xterm)
	 * \033[1q set "Scroll Lock" LED
	 * \033[2q set "Num Lock" LED
	 * \033[3q set Caps Lock LED
	 * \033[15;40H move the cursor to line 15, column 40
	 * \007 bell (beep)
	 * @param color
	 */
	public static void setConsoleColor(Colors color)
	{
		if (color == Colors.RESET)
		{
			printInline("\033[0m");
		}
		else
			printInline("\033[1;3" + color.ordinal() + "m");			
	}
	
	/**
	 * Formats and returns a string containing the current date & time
	 * @return current data + time
	 */
	public static String getDateTime(String format) 
	{
	    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
	    
	    Date date = new Date();
	    
	    return dateFormat.format(date);
	}
	
	public static String getCurrentDateTime()
	{
		return getDateTime("yyyy-MM-dd HH:mm:ss");
	}
	
	public synchronized static void startFileLogging()
	{
		if (_logFile != null)
		{
			try 
			{
				_logFile.close();
				_logFile = null;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String filename = "reasoning_" + sdf.format(new Date()) + "_" + _uniqueNumber++ + ".txt";
		
		log("logs will be written to 'log/" + filename + "'.");
		
		try 
		{
			_logFile = new BufferedWriter(new FileWriter("log/" + filename));
		} 
		catch (IOException e) 
		{
			log("WARNING: could not open log at 'log/" + filename + "' for writing!");
			_logFile = null;
		}
	}
	
	public synchronized static void endFileLogging()
	{
		if (_logFile != null)
		{
			try 
			{
				_logFile.close();
				_logFile = null;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	public synchronized static void log(String msg)
	{
		logInline(msg + "\r\n");
	}
	public synchronized static void logInline(String msg)
	{
		Long threadId = Thread.currentThread().getId();
		
		if (_logMode == LogMode.VERBOSE)
			msg = getDateTime("yyyy-MM-dd HH:mm:ss") + ": semantic: " + threadId + ": " + msg;
		
		System.out.print(msg);
		
		if (_logFile != null)
		{
			try 
			{
				_logFile.write(msg);
				_logFile.flush();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	public synchronized static void log()
	{
		System.out.println();
		
		if (_logFile != null)
		{
			try 
			{
				_logFile.write("\r\n");
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	public synchronized static void logError(String msg)
	{
		System.err.println(getDateTime("yyyy-MM-dd HH:mm:ss") + ": " + msg);
	}
	
	/**
     * XML helper to get tag values
     * @param sTag Tag name
     * @param eElement The XML node
     * @return Tag value
     */
	public static String getTagValueRecursive(String sTag, Element eElement, int index)
    {
		NodeList nList = eElement.getElementsByTagName(sTag);
		
		if (nList == null)
			return null;
		
		Node node = nList.item(index);
		
		if (node == null)
			return null;
		
		NodeList children = node.getChildNodes();
		
		if (children == null)
			return null;
    	 
        Node nValue = (Node) children.item(0);
        
        if (nValue == null)
        	return null;
 
        return nValue.getNodeValue();
    }
	
	public static String getTagValue(String sTag, Element eElement, int index)
    {
		int seen = 0;
		
		Node node;
		String name;
		
		NodeList nList = eElement.getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++)
		{
			node = nList.item(i);
			name = node.getNodeName();
			
			if (name.equals(sTag))
			{
				if (index == seen)
				{
					node = node.getFirstChild();
					
					if (node != null)
						return node.getNodeValue();
					else
						return null;
				}
				
				seen++;
			}
		}
		
		return null;
    }
	
	/**
	 * XML helper to retrieve an attribute of a node 
	 * @param sTag Tag name
	 * @param sAtt Attribute name
	 * @param eElement The XML node
	 * @return Attribute value
	 */
	public static String getTagAttributeRecursive(String sTag, String sAtt, Element eElement)
    {
    	NodeList nList = eElement.getElementsByTagName(sTag);
    	
    	if (nList == null)
    		return null;
    	
		Node node = nList.item(0);
    	       
        if (node == null)
        	return null;
 
        Node attNode = node.getAttributes().getNamedItem(sAtt);
        
        if (attNode == null)
        	return null;
        
        return attNode.getNodeValue().toString();
    }
	
	public static String getTagAttribute(String sTag, String sAtt, Element eElement)
    {
		Node node;
		String name;
		
		NodeList nList = eElement.getChildNodes();
		
		for (int i = 0; i < nList.getLength(); i++)
		{
			node = nList.item(i);
			name = node.getNodeName();
			
			if (name.equals(sTag))
			{
				node = node.getAttributes().getNamedItem(sAtt);
				
				if (node != null)
					return node.getNodeValue().toString();
				else
					return null;
			}
		}
		
		return null;
    }
	
	
	/**
	 * Removes whatever it finds after the `#` character (sense info) in a (naturally) concept name
	 * 
	 * @param name The concept name
	 * @return The cleaned concept name without sense info
	 */
	public static String removeSenseInfo(String name)
	{
		int pos = name.indexOf("#");
		
		if (pos != -1)
			return name.substring(0, pos);
		
		pos = name.indexOf("§");
		
		if (pos != -1)
			return name.substring(0, pos);
		
		return name;
	}
	
	/**
	 * Determines whether a string is in English (alphabet + dash)
	 * 
	 * @param text Text to be examined
	 * @return True if it is an English string
	 */
	public static boolean isEnglish(String text)
	{
		 return text.matches("^[\\w:-]+$");
	}
	
	public static String removeDiacritic(String text)
	{
		String out = text.replaceAll("[ًٌٍَُِّ]", "");
		
		out = out.replaceAll(" ْ", "");
		
		return out;
	}
	
	public static String removePunctuations(String text)
	{
		return text.replaceAll("[«»:;\\.،؛\\?؟!\"]", "");
	}
	
	/**
	 * A simple string normalizer
	 * @param text input text
	 * @return normalized text
	 */
	public static String normalizeNotTokenized(String text)
	{
		//TODO: some concpets have '\r\n' and need them. find a way to remove 'replace("\r", " ").replace("\n", " ")'. known issues if do so: permamnet concept ids file
		text = text.replace("ك", "ک").replace("ي", "ی").replace("ى", "ی").replace("\r", " ").replace("\n", " ");
		
		text = text.replace("ي", "ی").replace("ی", "ی").replace("ى", "ی").replace("ك", "ک").replace("ک", "ک");
		
		text = text.replaceAll(String.valueOf(Character.toChars(8203)), new String(Character.toChars(8204)));
		text = text.replaceAll(String.valueOf(Character.toChars(1609)), "ی"); // arabic letter ye maksura
		
		text = replaceCorresponding(text, "۰۱۲۳۴۵۶۷۸۹", "0123456789");
		text = replaceCorresponding(text, "٠١٢٣٤٥٦٧٨٩", "0123456789");
		
		// correcting punctuation spacings, commented as it contradicts the tokenizer's output		
		//text = text.replaceAll(" ([;,،؛:])", "$1 ");
		//text = text.replaceAll("\\(", " \\(");
		//text = text.replaceAll("\\)", "\\) ");
		
		text = text.replace("  ", " ");
		text = Common.trimAll(text, "\" \u200C");
	
		return text;
	}
	
	 
	public static boolean isWordBoundary(Character character)
	{
		return character.toString().matches("[;,،؛:\\-!\\?؟@#\\$%\\^&\\*_\\+\\./]");
	}
	public static boolean containsAnyWordBoundary(String str)
	{
		return str.matches(".*[;,،؛:\\-!\\?؟@#\\$%\\^&\\*_\\+\\./].*");
	}
	
	/**
	 * Pads a string at the end
	 * @param s
	 * @param n
	 * @return
	 */
	public static String padAfter(String s, int n) 
	{
	     return String.format("%1$-" + n + "s", s);  
	}

	/**
	 * Pads a string at the beginning
	 * @param s
	 * @param n
	 * @return
	 */
	public static String padBefor(String s, int n) 
	{
	    return String.format("%1$" + n + "s", s);  
	}
	
	/**
	 * Breaks long names with HTML's br tag
	 * It is used when showing concepts with long names via JUNG library visualization tool
	 * @param text concept name
	 * @param width maximum width after which breaking happens
	 * @return broken concept name
	 */
	public static String breakLine(String text, int width)
	{
		if (text.length() <= width)
			return text;
		
		String out = "";
		int end = 0;
		
		while (text.length() > width)
		{
			end = width;
			
			while (end < text.length() && text.charAt(end) != ' ')
				end++;
			
			out += text.substring(0, end) + "<br>";
			text = text.substring(end);
		}
		
		out += text + "<br>";
		
		return out;
	}
	
	/**
	 * computes the intersection of two sets
	 * @param set1 first set
	 * @param set2 second set
	 * @return intersected set
	 */
	public static int getIntersection(Set<Long> set1, Set<Long> set2) 
	{
	    boolean set1IsLarger = set1.size() > set2.size();
	    
	    Set<Long> cloneSet = new HashSet<Long>(set1IsLarger ? set2 : set1);
	    cloneSet.retainAll(set1IsLarger ? set1 : set2);
	    
	    return cloneSet.size();
	}
	
	/**
	 * Converts a list of plausible answers to a list of strings
	 * @param answers list of plausible answers
	 * @return list of strings
	 */
	public static ArrayList<String> convertPlausibleAnswerToStringList(ArrayList<PlausibleAnswer> answers)
	{
		ArrayList<String> out = new ArrayList<String>();
		
		for (PlausibleAnswer answer: answers)
		{
			out.add(answer.answer.getName());
		}
		
		return out;
	}
	
	/**
	 * Converts a list of strings to plausible answers
	 * @param answers list of strings
	 * @return list of plausible answers
	 */
	public static ArrayList<PlausibleAnswer> convertStringToPlausibleAnswerList(ArrayList<String> answers)
	{
		ArrayList<PlausibleAnswer> out = new ArrayList<PlausibleAnswer>();
		
		HashSet<String> all = new HashSet<String>();
		
		for (String answer: answers)
		{
			if (all.contains(answer))
				continue;
			
			ir.ac.itrc.qqa.semantic.kb.Node node = new ir.ac.itrc.qqa.semantic.kb.Node(answer);
			PlausibleAnswer pa = new PlausibleAnswer(node);			
			out.add(pa);
			
			all.add(answer);
		}
		
		return out;
	}
	
	public static HashSet<ir.ac.itrc.qqa.semantic.kb.Node> convertPlausibleAsnwerArrayListToHashSet(ArrayList<PlausibleAnswer> ins)
	{
		HashSet<ir.ac.itrc.qqa.semantic.kb.Node> outs = new HashSet<ir.ac.itrc.qqa.semantic.kb.Node>();
		
		for (PlausibleAnswer in: ins)
		{
			outs.add(in.answer);
		}
		
		return outs;
	}
	
	/**
	 * Loads a whole file into a string 
	 * @param filename path to the file
	 * @return string containing the file content
	 */
	public static String getFileContent(String filename)
	{
		String out = null;
		byte[] encoded = null;
		
		try
		{
			encoded = Files.readAllBytes(Paths.get(filename));
		} 
		catch (IOException e)
		{
			MyError.exit(e.getMessage());
		}
		
		try
		{
			out = new String(encoded, "UTF8");
		} 
		catch (UnsupportedEncodingException e)
		{
			MyError.exit(e.getMessage());
		}
		
		return out;
    }
	
	
	public static ArrayList<String> getFileLines(String filename)
	{
		String content = getFileContent(filename);
		
		String[] outs = content.split("\n");
		
		for (int i = 0; i < outs.length; i++)
		{
			outs[i] = outs[i].trim();
		}
		
		return new ArrayList<String>(Arrays.asList(outs));
	}
		
	
	public static void putFileContent(String path, String payload)
	{
		BufferedWriter outFile = Common.openFileForWriting(path);
		
		try
		{
			outFile.write(payload);
		
			outFile.close();
		}
		catch(IOException e)
		{
			MyError.exit("Couldn't write to file '" + path + "'!");
		}
	}
	
	/**
	 * Lists all files in a directory with specified extension
	 * @param path directory
	 * @param extenstion the required extension
	 */
	public static ArrayList<String> listDirectoryEntries(String path, DirectoryEntryType type, Pattern pattern)
	{
		if (pattern == null)
			pattern = Pattern.compile(".*");
		
		Matcher matcher;
		
		File folder = new File(path);
		
		File[] listOfFiles = folder.listFiles();
		
		ArrayList<String> out = new ArrayList<String>();  
	 
		for (int i = 0; i < listOfFiles.length; i++) 
		{
		   if (type == DirectoryEntryType.ALL || (listOfFiles[i].isFile() && type == DirectoryEntryType.FILE) || (listOfFiles[i].isDirectory() && type == DirectoryEntryType.DIRECTORY)) 
		   {
			   String file = listOfFiles[i].getName();
			   
			   if (file.equals(".") || file.equals("..")) // ignoring current & up directories
				   continue;
		       
			   matcher = pattern.matcher(file);
			   
			   if (matcher.find())
				   out.add(file);				   
		   }
		}
		
		return out;
	}
	

	public static String replaceCorresponding(String text, String toFind, String toReplace)
	{
		MyError.assertTrue(toFind.length() == toReplace.length());
		
		for (int i = 0; i < toFind.length(); i++)
		{
			text = text.replace(toFind.charAt(i), toReplace.charAt(i));
		}
		
		return text;
	}
	
	public static String getCurrentTimeStamp() 
	{
	    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
	    Date now = new Date();
	    String strDate = sdfDate.format(now);
	    return strDate;
	}
	
	public static boolean fileExists(String path)
	{
		File file = new File(path);
		
		if (file.exists())
		{
			return true;
		}
		
		return false;
	}
	
	public static String getIp()
	{
		InetAddress ip;
		 
		try 
		{
			ip = InetAddress.getLocalHost();
			return ip.getHostAddress();
		} 
		catch (UnknownHostException e) 
		{
			e.printStackTrace();
		}
		 
		return "localhost";
	}
	
	public static void copyFile(String fromPath, String toPath)
	{
	    File from 	= new File(fromPath);
	    File to 	= new File(toPath);
		
		try 
	    {
			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} 
	    catch (IOException e) 
	    {
			e.printStackTrace();
		}
	}
	
	public static boolean isNumeric(String text)
	{
		if (text.matches("^((-|\\+)?[0-9]+(\\.[0-9]+)?)+$"))
			return true;

	    return false;
	}
	
	public static boolean isAlpha(String text)
	{
		if (text.matches("^[a-zA-Z'-:!_\\.\\s]+$"))
			return true;

	    return false;
	}
	
	
	private static HashSet<String> probeLexicalTransformation(String probe, HashSet<String> already)
	{
		HashSet<String> outs = new HashSet<String>();
		
		probe = probe.replace("  ", " ").trim();
		
		if (!already.contains(probe))
			outs.add(probe);

		return outs;
	}
	
	
	public static ArrayList<String> getLexicalTransformations(String original)
	{
		HashSet<String> outs = new HashSet<String>();
		
		if (original.isEmpty())
			return new ArrayList<String>(outs);
		
		HashSet<String> news = new HashSet<String>();
		
		String canonical = Common.canonicalizeString(original);
		
		outs.add(canonical);
		
		// now other things
		
		String parenthesisLess = Common.removeParenthesis(canonical);
		
		String halfSpaceLess = canonical.replace('\u200C', ' ');
		
		String spaceLess = canonical.replace(' ', '\u200C');
		
		String puncLess = Common.removePunctuations(canonical);
		
		
		outs.addAll(news);
		news.clear();
		
		
		for (String probe: outs)
		{
			probe =  probe.replaceAll("\\bعلیهالسلام\\b", "");
			probe =  probe.replaceAll("\\bعلیها السلام\\b", "");
			probe =  probe.replaceAll("\\bعلیه السلام\\b", "");
			probe =  probe.replaceAll("\\bعلیه‌السلام\\b", "");
			probe =  probe.replaceAll("\\(\\s*ع\\s*\\)", "");	
			probe =  probe.replaceAll("\\(\\s*س\\s*\\)", "");
			probe =  probe.replaceAll("\\(\\s*ص\\s*\\)", "");
			probe =  probe.replaceAll("\\bصلی الله علیه وآله\\b", "");
			probe =  probe.replaceAll("\\bصلی الله علیه و آله\\b", "");
			probe =  probe.replaceAll("\\bصلی الله علیه و آله و سلم\\b", "");
			probe =  probe.replaceAll("\\bصلی الله علیه وآله و سلم\\b", "");
			probe =  probe.replaceAll("\\bصلی الله علیه وآله وسلم\\b", "");		
			probe =  probe.replace("()", "");
			probe = probe.replace("(  )", ""); // tokenized version
			
			news.addAll(probeLexicalTransformation(probe, outs));
		}
		
		outs.addAll(news);
		news.clear();
		
		outs.remove(original);
		
		return new ArrayList<String>(outs);
	}
	
	
	public static String canonicalizeString(String text)
	{
		HashSet<String> outs = new HashSet<String>();
		
		if (text.isEmpty())
			return "";
		
		String original = text;
		
		text = Common.removeDiacritic(text);
		
		text = text.replaceAll("(\\(\\s*)?علیها.?لسلام(\\s*\\))?", "( ع )");
		text = text.replaceAll("(\\(\\s*)?علیها.?سلام(\\s*\\))?", "( ع )");
		text = text.replaceAll("(\\(\\s*)?علیها.?السلام(\\s*\\))?", "( ع )");
		text = text.replaceAll("(\\(\\s*)?علیه.?السلام(\\s*\\))?", "( ع )");
		text = text.replaceAll("(\\(\\s*)?سلام.?الله.?علیه(\\(\\s*)?(\\s*\\))?", "( س )");
		//text = text.replaceAll("(\\(\\s*)?صلی.?الله.?علیه.?و.?آله.?و.?سلم(\\s*\\))?", "( ص )");
		text = text.replaceAll("(\\(\\s*)?صلی.?الله.?علیه.?و.?آله(\\s*\\))?", "( ص )");
		text = text.replaceAll("(\\(\\s*)?صلی.?الله(\\s*\\))?", "( ص )");
		text = text.replace("()", "");
		text = text.replace("(  )", ""); // tokenized version
		
		// converting حضرت محمد (ص) and حضرت محمد to canonical form محمد (ص)
		
		text = text.replaceAll("حضرت ([^ ]+) \\(\\s*ص\\s*\\)", "حضرت $1");
		text = text.replaceAll("حضرت ([^ ]+) \\(\\s*ع\\s*\\)", "حضرت $1");
		text = text.replaceAll("حضرت ([^ ]+) \\(\\s*س\\s*\\)", "حضرت $1");
		text = text.replace("  ", " ");
				
		// either there is a stupid bug in regex or I am going bananas! the following regex does not match the last parenthesis which complicates the code 
		Pattern pattern = Pattern.compile("(?<!آن )\\s*(حضرت [^ ]+)");
		Matcher matcher = pattern.matcher(text);
		
		String modified = text;
		
		while (matcher.find())
		{
			String search = matcher.group(1);
			
			String name = search.substring("حضرت".length() + 1);
			
			if (name.equals("محمد"))
				modified = modified.replace(search, name + " ( ص )");
			else
				modified = modified.replace(search, name + " ( ع )");
		}
		
		text = modified;
		
		text = Common.removeParenthesisWithException(text, "ص", "ع", "س");
		
		text = text.replace('\u200C', ' ').replace("  ", " ").trim();		
		
		return text;
	}
	
	public static void fileTruncate(String path, int newSize)
	{
		FileChannel outChan;
		try 
		{
			outChan = new FileOutputStream(path, true).getChannel();
			outChan.truncate(newSize);
		    outChan.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts a PLP Toolkit to internal pos tags.
	 * Possible input tags are:
	 * - N_PLUR
	 * - N_INFI
	 * - N_SING
	 * - PREP
	 * - POSTP
	 * - ADJ
	 * - PR_1_PLUR
	 * - PR_1_SING
	 * - PR_2_PLUR
	 * - PR_2_SING
	 * - PR_3_PLUR
	 * - PR_3_SING
	 * - PR
	 * - PR_INTG
	 * - PR_SING
	 * - PR_PLUR 
	 * - NUM
	 * - PUNC
	 * - V_HA_1_PLUR
	 * - V_HA_1_SING
	 * - V_HA_2_PLUR
	 * - V_HA_2_SING
	 * - V_HA_3_PLUR
	 * - V_HA_3_SING
	 * - V_AY_1_PLUR
	 * - V_AY_1_SING
	 * - V_AY_2_PLUR
	 * - V_AY_2_SING
	 * - V_AY_3_PLUR
	 * - V_AY_3_SING
	 * - V_H_1_PLUR
	 * - V_H_1_SING
	 * - V_H_2_PLUR
	 * - V_H_2_SING
	 * - V_H_3_PLUR
	 * - V_H_3_SING
	 * - V_MODL_AY
	 * - V_MODL_H
	 * - V_MODL_G
	 * - V_G_1_PLUR
	 * - V_G_1_SING
	 * - V_G_2_PLUR
	 * - V_G_2_SING
	 * - V_G_3_PLUR
	 * - V_G_3_SING
	 * - ADV
	 * - ADR
	 * - CONJ
	 * @param pos
	 * @return
	 */
	public static POS convertPlpToInternalPos(String pos)
	{
		if (pos.startsWith("N_"))
		{
			return POS.NOUN;
		}
		else if (pos.startsWith("V_"))
		{
			return POS.VERB;
		}
		else if (pos.equals("ADJ"))
		{
			return POS.ADJECTIVE;
		}
		else if (pos.equals("ADV"))
		{
			return POS.ADVERB;
		}
		
		return POS.UNKNOWN;
	}
	
	public static LinkedHashSet<LinkedHashSet<Object>> cartesianProduct(Set<?>... sets) 
	{
	    if (sets.length < 2)
	        throw new IllegalArgumentException(
	                "Can't have a product of fewer than two sets (got " +
	                sets.length + ")");

	    return _cartesianProduct(0, sets);
	}

	private static LinkedHashSet<LinkedHashSet<Object>> _cartesianProduct(int index, Set<?>... sets) 
	{
		LinkedHashSet<LinkedHashSet<Object>> ret = new LinkedHashSet<LinkedHashSet<Object>>();
	    
	    if (index == sets.length) 
	    {
	        ret.add(new LinkedHashSet<Object>());
	    } 
	    else 
	    {
	        for (Object obj : sets[index]) 
	        {
	            for (LinkedHashSet<Object> set : _cartesianProduct(index + 1, sets)) 
	            {
	                set.add(obj);
	                ret.add(set);
	            }
	        }
	    }
	    return ret;
	}
	
	public static float caclulateCommonProduct(float x, float y)
	{
		return x + y - x * y; 
	}
	
	public static <T> ArrayList<T> mergeToArray(T... args)
	{
		ArrayList<T> out = new ArrayList<T>();
		
		out.addAll(Arrays.asList(args));
		
		return out;
	}
	
	public static <T> LinkedHashSet<T> mergeToSet(T... args)
	{
		LinkedHashSet<T> out = new LinkedHashSet<T>();
		
		out.addAll(Arrays.asList(args));
		
		return out;
	}
	
	public static boolean matchAtBoundries(String haystack, String needle)
	{
		if (haystack.length() < needle.length())
			return false;
		
		if (needle.equals(haystack))
			return true;
		
		String boundries = "[ ,;:!\\?،؛؟\\-_\\+\\*&%=\\(\\)]";
		
		if (haystack.startsWith(needle) && Character.toString(haystack.charAt(needle.length())).matches(boundries))
			return true;
		
		if (haystack.endsWith(needle) && Character.toString(haystack.charAt(haystack.length() - needle.length() - 1)).matches(boundries))
			return true;
		
		if (haystack.matches(".*" + boundries + Pattern.quote(needle) + boundries + ".*"))
			return true;
		
		return false;
	}
	
	public static <T> ArrayList<T> getFirstItem(ArrayList<T> list)
	{
		if (list == null)
			return null;
		
		if (list.isEmpty())
			return list;
		
		return new ArrayList<T>(list.subList(0, 1));
	}
	
	
	public static String convertHunderedsToStr(int num)  
	{  
		String s = "";  
		
		int d1;
		int d2;
		int d12 = num % 100;  
		int d3 = num / 100;  		
		
		if (d3 != 0)  
			s = _sadgan[d3] + " و ";  
		
		if (d12 >= 10 && d12 <= 19)  
		{  
			s += _dahyek[d12 - 10];  
		}  
		else  
		{  
			d2 = d12/10;  
		
			if (d2 != 0)  
				s += _dahgan[d2] + " و ";  
			
			d1 = d12 % 10;  
			
			if (d1 != 0)  
				s += _yakan[d1] + " و ";  
			
			s = s.substring(0, s.length() - 3);
		}
		return s;  
	}  


	// 90/11/09 -----------------
	public static String convertNumberToPerianString(Integer num)  
	{  
		String snum = num.toString();		
		String out = ""; 
		
		if (num == 0)  
		{  
			return _yakan[0];  
		}  
		else  
		{  
			snum = padBefor(snum, (snum.length()/3 + 1) * 3).replace(' ', '0'); 
		
			int groupLength = snum.length()/3 - 1;  
			
			for (int i = 0; i <= groupLength; i++)  
			{  
				String group = snum.substring(i * 3, i * 3 + 3);  
				
				if (Integer.parseInt(group) != 0)  
					out = out + convertHunderedsToStr(Integer.parseInt(group)) + " " + _basex[groupLength - i] + " و ";  
			}  
			
			out = out.substring(0, out.length() - 3);  
		}  
		
		return out.trim();
	}
	
	public static List<String[]> csvToArray(String path)
	{
		return csvToArray(path, ',');
	}
	public static List<String[]> csvToArray(String path, char separator)
	{
		List<String[]> list = null;
		CSVReader reader = null;
		
		try 
		{
			reader = new CSVReader(new InputStreamReader(new FileInputStream(path), "CP1256"), separator);
			list = reader.readAll();
			reader.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return list;
	}
	

	public static BufferedReader openFileForReading(String path)
	{
		return openFileForReading(path, "UTF8");
	}
	public static BufferedReader openFileForReading(String path, String encoding)
	{
		BufferedReader handler = null;
		
		try
		{
			handler = new BufferedReader(new InputStreamReader(new FileInputStream(path), encoding));
		}
		catch (IOException e)
		{
			MyError.exit("Couldn't open '" + path + "' for reading!");
		}
		
		return handler;
	}
	
	public static BufferedWriter openFileForWriting(String path)
	{
		return openFileForWriting(path, "UTF8");
	}
	public static BufferedWriter openFileForWriting(String path, String encoding)
	{
		BufferedWriter handler = null;
		
		try
		{
			handler = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), encoding));
		}
		catch (IOException e)
		{
			MyError.exit("Couldn't open '" + path + "' for writing!");
		}
		
		return handler;
	}
	
	public static BufferedWriter openFileForAppending(String path)
	{
		return openFileForWriting(path, "UTF8");
	}
	public static BufferedWriter openFileForAppending(String path, String encoding)
	{
		BufferedWriter handler = null;
		
		try
		{
			handler = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true), encoding));
		}
		catch (IOException e)
		{
			MyError.exit("Couldn't open '" + path + "' for appending!");
		}
		
		return handler;
	}
	


	public static Pos stringToPos(String text)
	{
		Pos pos = new Pos();
		
		String var = "";
		
		String[] parts = text.split("_");
		
		// coarse grained -------------------------------------
		
		switch (parts[0])
		{
			case "ADJ"	: pos.coarse = PosCoarse.ADJECTIVE; break;
			case "ADR"	: pos.coarse = PosCoarse.ADDRESSING_TERM; break;
			case "ADV"	: pos.coarse = PosCoarse.ADVERB; break;
			case "ARAB"	: pos.coarse = PosCoarse.ARABIC; break;
			case "CONJ"	: pos.coarse = PosCoarse.COORDINATING_CONJUNCTIION; break;
			case "EMPH"	: pos.coarse = PosCoarse.EMPHSIZER; break;
			case "LATIN": pos.coarse = PosCoarse.LATIN; break;
			case "N"	: pos.coarse = PosCoarse.NOUN; break;
			case "NUM"	: pos.coarse = PosCoarse.NUMBER; break;
			case "POSTP": pos.coarse = PosCoarse.POSTPOSITION; break;
			case "PR"	: pos.coarse = PosCoarse.PRONOUN; break;
			case "PREP"	: pos.coarse = PosCoarse.PREPOSITION; break;
			case "PSUS"	: pos.coarse = PosCoarse.PSEUDO_SENTENCE; break;
			case "PUNC"	: pos.coarse = PosCoarse.PUNCTUATION; break;
			case "START": pos.coarse = PosCoarse.START; break;
			case "SUBR"	: pos.coarse = PosCoarse.SUBORDINATING_CONJUNCTION; break;
			case "V"	: pos.coarse = PosCoarse.VERB; break;
		}
		
		// fine grained -------------------------------------
		
		var = "";
		
		if (pos.coarse == PosCoarse.NOUN)
			var = parts[1];
		else if (pos.coarse == PosCoarse.ADJECTIVE && parts.length == 2)
			var = parts[1];
		else if (pos.coarse == PosCoarse.ADVERB && parts.length == 2)
			var = parts[1];
		else if (pos.coarse == PosCoarse.VERB)
			var = parts[1];
		else if (pos.coarse == PosCoarse.PRONOUN  && parts.length == 2)
			var = parts[1];

		switch (var)
		{
			case "INTG": pos.fine = PosFine.INTEROGATIVE; break;
			case "INFI": pos.fine = PosFine.INFINITIVE; break;
			case "MODL": pos.fine = PosFine.MODAL; break;
		}

		// person -------------------------------------
		
		var = "";
		
		if (pos.coarse == PosCoarse.PRONOUN && parts.length > 1)
			var = parts[1];
		else if (pos.coarse == PosCoarse.VERB && parts.length > 2)
			var = parts[2];
		
		switch (var)
		{
			case "1": pos.person = PosPerson.FIRST; break;
			case "2": pos.person = PosPerson.SECOND; break;
			case "3": pos.person = PosPerson.THIRD; break;
		}
		
		// number ----------------------------------------
		
		var = "";
		
		if (pos.coarse == PosCoarse.NOUN)
			var = parts[1];
		else if (pos.coarse == PosCoarse.VERB && parts.length > 3)
			var = parts[3];
		else if (pos.coarse == PosCoarse.PRONOUN && parts.length > 2)
			var = parts[2];
		
		switch (var)
		{
			case "SING": pos.number = PosNumber.SINGULAR; break;
			case "PLUR": pos.number = PosNumber.PLURAL; break;
		}
		
		// verb tense -------------------------------------
		
		var = "";
		
		if (pos.coarse == PosCoarse.VERB)
		{
			if (pos.fine == PosFine.MODAL && parts.length == 3)
				var = parts[2];
			else
				var = parts[1];
		}
		
		switch (var)
		{
			case "G"	: pos.tense = PosTense.PAST; break;
			case "H"	: pos.tense = PosTense.PRESENT; break;
			case "AY"	: pos.tense = PosTense.FUTURE; break;
			case "HA"	: pos.tense = PosTense.IMPERATIVE; break;
		}
		
		// verb polarity
		
		if (pos.coarse == PosCoarse.VERB && parts.length == 5 && parts[5].equals("NEG"))
		{
			pos.polarity = PosPolarity.NEGATIVE;
		}
		
		return pos;
	}
	
	public synchronized void setKnowledgebase(KnowledgeBase kb)
	{
		_kb = kb;
	}
	
	
	public static void applyCertaintyToAll(ArrayList<PlausibleAnswer> answers, float certainty)
	{
		for (PlausibleAnswer answer: answers)
		{
			answer.parameters.certainty *= certainty;
		}
	}
	

	public static String removeLastParenthesis(String text)
	{
		int end = text.lastIndexOf(')');
		
		if (end == -1)
			return text;
		
		int start = text.lastIndexOf('(', end);
		
		if (start == -1)
			return text;
		
		return text.substring(0, start);
	}
	
	public static void setLogMode(LogMode lm)
	{
		log("setting log level to " + lm);
		
		_logMode = lm;
	}
}