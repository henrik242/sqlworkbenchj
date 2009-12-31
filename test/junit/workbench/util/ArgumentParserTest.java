/*
 * ArgumentParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Collection;
import junit.framework.*;


/**
 *
 * @author Thomas Kellerer
 */
public class ArgumentParserTest
	extends TestCase
{
	public ArgumentParserTest(String testname)
	{
		super(testname);
	}
	
	public void testAllowedValues()
	{
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("type", StringUtil.stringToList("text,xml,sql"));
		Collection c = arg.getAllowedValues("type");
		assertTrue(c.contains("text"));
		assertTrue(c.contains("TEXT"));
		assertTrue(c.contains("Text"));
	}
	
	public void testParser()
	{
		String cmdline = "-delimiter=',' -otherbool=1 -nosettings -table='\"MIND\"' -boolarg=true -profile='test-prof' -script=bla.sql -arg2=\"with space and quote\"";
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("profile");
		arg.addArgument("script");
		arg.addArgument("arg2");
		arg.addArgument("nosettings");
		arg.addArgument("boolarg");
		arg.addArgument("otherbool");
		arg.addArgument("table");
		arg.addArgument("delimiter");
		arg.parse(cmdline);
		assertEquals("profile not retrieved", "test-prof", arg.getValue("profile"));
		assertEquals("script not retrieved", "bla.sql", arg.getValue("script"));
		assertEquals("double quoted value not retrieved", "with space and quote", arg.getValue("arg2"));
		assertEquals("argument without parameter not found", true, arg.isArgPresent("noSettings"));
		assertEquals("boolean argument not retrieved", true, arg.getBoolean("boolArg", false));
		assertEquals("numeric boolean argument not retrieved", true, arg.getBoolean("otherBool", false));
		assertEquals("Embedded quotes were removed", "\"MIND\"", arg.getValue("TABLE"));
		assertEquals("Delimiter not retrieved", ",", arg.getValue("delimiter"));
		
		arg = new ArgumentParser();
		arg.addArgument("delimiter");
		arg.addArgument("altdelimiter");
		arg.addArgument("type");
		
		cmdline = "-delimiter='\" \"' -altdelimiter=\"/;nl\" -type=text";
		arg.parse(cmdline);
		
		assertEquals("Blank as delimiter not retrieved", " ", StringUtil.trimQuotes(arg.getValue("delimiter")));
		assertEquals("Wrong altDelimiter", "/;nl", arg.getValue("altDelimiter"));
		assertEquals("Wrong type", "text", arg.getValue("type"));
	}

}
