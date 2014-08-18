package ir.ac.itrc.qqa.semantic.enums;

import ir.ac.itrc.qqa.semantic.util.MyError;

public enum PovElement
{
	PROPERTY,
	OBJECT,
	VALUE,
	NONE;
	
	public static PovElement str2pov(String str)
	{
		if (str.equals("_"))
			return NONE;
		
		switch (str)
		{
			case "P": return PROPERTY; 
			case "O": return OBJECT; 
			case "V": return VALUE;
			default	: MyError.exit("Invalid POV element '" + str + "' given!"); return NONE;	
		}
	}
}
