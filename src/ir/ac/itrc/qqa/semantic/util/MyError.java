/* ErrorChecking.java
 * Created on May 24, 2010
 */

package ir.ac.itrc.qqa.semantic.util;

/**
 * This file does simple error checking and produces error messages and stack traces. 
 * @author Ehsan Darrudi
 *
 */
public class MyError
{
	/**
	 * Simple assertion method
	 * @param result
	 */
	public static void assertTrue(boolean result)
	{
		if (!result)
		{
			exit("An assertion failed!");
		}
	}
	
	/**
	 * Simple assertion method
	 * @param result
	 * @param message
	 */
	public static void assertTrue(boolean result, String message)
	{
		if (!result)
		{
			exit(message);
		}
	}
	
	/**
	 * asserts whether an object is null and ends the application if that's the case 
	 * @param object to be checked
	 */
	public static void assertNotNull(Object object)
	{
		if (object == null)
		{
			exit("Unexpected null object!");	
		}
	}
	
	/**
	 * Simple assertion method
	 * @param object
	 * @param message
	 */
	public static void assertNotNull(Object object, String message)
	{
		if (object == null)
		{
			exit(message);	
		}
	}
	
	/**
	 * prints the provided message and exits
	 * @param msg message to be printed
	 */
	public static void exit(String message)
	{
		try 
		{
			throw new Exception("\r\n" + message);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		System.exit(-1);
	}
}

