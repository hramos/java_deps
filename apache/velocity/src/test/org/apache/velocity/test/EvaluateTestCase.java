package org.apache.velocity.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.RuntimeConstants;

/**
 * Test #evaluate directive.
 * 
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain</a>
 * @version $Id: EvaluateTestCase.java 685287 2008-08-12 20:06:31Z nbubna $
 */
public class EvaluateTestCase extends BaseTestCase
{
    
    /**
    * VTL file extension.
    */
   private static final String TMPL_FILE_EXT = "vm";

   /**
    * Comparison file extension.
    */
   private static final String CMP_FILE_EXT = "cmp";

   /**
    * Comparison file extension.
    */
   private static final String RESULT_FILE_EXT = "res";

   /**
    * Path for templates. This property will override the
    * value in the default velocity properties file.
    */
   private final static String FILE_RESOURCE_LOADER_PATH = TEST_COMPARE_DIR + "/evaluate";

   /**
    * Results relative to the build directory.
    */
   private static final String RESULTS_DIR = TEST_RESULT_DIR + "/evaluate";

   /**
    * Results relative to the build directory.
    */
   private static final String COMPARE_DIR = TEST_COMPARE_DIR + "/evaluate/compare";

    /**
     * Default constructor.
     * @param name
     */
    public EvaluateTestCase(String name)
    {
        super(name);
    }

    public void setUp()
    {
        assureResultsDirectoryExists(RESULTS_DIR);
    }

    public static Test suite()
    {
       return new TestSuite(EvaluateTestCase.class);
    }

    /**
     * Test basic functionality.
     * @throws Exception
     */
    public void testEvaluate()
    throws Exception
    {
        testFile("eval1", new HashMap());
    }

    /**
     * Test evaluate directive preserves macros (VELOCITY-591)
     * @throws Exception
     */
    public void testEvaluateMacroPreserve()
    throws Exception
    {
        Map properties = new HashMap();
        properties.clear();
        properties.put(RuntimeConstants.VM_CONTEXT_LOCALSCOPE,"false");
        testFile("eval2", properties);

        properties.clear();
        properties.put(RuntimeConstants.VM_CONTEXT_LOCALSCOPE,"true");
        testFile("eval2", properties);

        properties.clear();
        properties.put(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL,"false");
        testFile("eval2", properties);


    }

    /**
     * Test in a macro context.
     * @throws Exception
     */
    public void testEvaluateVMContext()
    throws Exception
    {
        testFile("evalvmcontext", new HashMap());
    }

    /**
     * Test #stop (since it is attached to context).
     * @throws Exception
     */
    public void testStop()
    throws Exception
    {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        
        Context context = new VelocityContext();        
        StringWriter writer = new StringWriter();
        ve.evaluate(context, writer, "test","test #stop test2 #evaluate('test3')");
        assertEquals("test ", writer.toString());
        
        context = new VelocityContext();        
        writer = new StringWriter();
        ve.evaluate(context, writer, "test","test test2 #evaluate('test3 #stop test4') test5");
        assertEquals("test test2 test3  test5", writer.toString());
        
    }

    /**
     * Test that the event handlers work in #evaluate (since they are
     * attached to the context).  Only need to check one - they all 
     * work the same.
     * @throws Exception
     */
    public void testEventHandler()
    throws Exception
    {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION, EscapeHtmlReference.class.getName());
        ve.init();
        
        Context context = new VelocityContext();
        context.put("lt","<");
        context.put("gt",">");
        StringWriter writer = new StringWriter();
        ve.evaluate(context, writer, "test","${lt}test${gt} #evaluate('${lt}test2${gt}')");
        assertEquals("&lt;test&gt; &lt;test2&gt;", writer.toString());
        
    }
    
    
    /**
     * Test errors are thrown
     * @throws Exception
     */
    public void testErrors()
    throws Exception
    {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        
        Context context = new VelocityContext();
        
        // no arguments
        StringWriter writer = new StringWriter();
        try 
        {
            ve.evaluate(context, writer, "test",
                              "#evaluate()");
            fail("Expected exception");
        }
        catch (ParseErrorException e)
        {
            assertEquals("test",e.getTemplateName());
            assertEquals(1,e.getLineNumber());
            assertEquals(1,e.getColumnNumber());
        }
        
        // too many arguments
        writer = new StringWriter();
        try 
        {
            ve.evaluate(context, writer, "test",
                              "#evaluate('aaa' 'bbb')");
            fail("Expected exception");
        }
        catch (ParseErrorException e)
        {
            assertEquals("test",e.getTemplateName());
            assertEquals(1,e.getLineNumber());
            assertEquals(17,e.getColumnNumber());
        }
        
        // argument not a string or reference
        writer = new StringWriter();
        try 
        {
            ve.evaluate(context, writer, "test",
                              "#evaluate(10)");
            fail("Expected exception");
        }
        catch (ParseErrorException e)
        {
            assertEquals("test",e.getTemplateName());
            assertEquals(1,e.getLineNumber());
            assertEquals(11,e.getColumnNumber());
        }
        
        // checking line/col for parse error
        writer = new StringWriter();
        try 
        {
            String eval = "this is a multiline\n\n\n\n\n test #foreach() with an error";
            context.put("eval",eval);
            ve.evaluate(context, writer, "test",
                              "first line\n second line: #evaluate($eval)");
            fail("Expected exception");
        }
        catch (ParseErrorException e)
        {
            // should be start of #evaluate
            assertEquals("test",e.getTemplateName());
            assertEquals(2,e.getLineNumber());
            assertEquals(15,e.getColumnNumber());
        }        
    }
    
    /**
     * Test a file parses with no errors and compare to existing file.
     * @param basefilename
     * @throws Exception
     */
    private void testFile(String basefilename, Map properties)
    throws Exception
    {
        VelocityEngine ve = new VelocityEngine();
        ve.addProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, FILE_RESOURCE_LOADER_PATH);
     
        for (Iterator i = properties.keySet().iterator(); i.hasNext();)
        {
            String key = (String) i.next();
            String value = (String) properties.get(key);
            ve.addProperty(key, value);
        }
        
        ve.init();
        
        Template template;
        FileOutputStream fos;
        Writer fwriter;
        Context context;
        
        template = ve.getTemplate( getFileName(null, basefilename, TMPL_FILE_EXT) );
        
        fos = new FileOutputStream (
                getFileName(RESULTS_DIR, basefilename, RESULT_FILE_EXT));
        
        fwriter = new BufferedWriter( new OutputStreamWriter(fos) );
        
        context = new VelocityContext();
        setupContext(context);
        template.merge(context, fwriter);
        fwriter.flush();
        fwriter.close();
        
        if (!isMatch(RESULTS_DIR, COMPARE_DIR, basefilename, RESULT_FILE_EXT, CMP_FILE_EXT))
        {
            String result = getFileContents(RESULTS_DIR, basefilename, RESULT_FILE_EXT);
            String compare = getFileContents(COMPARE_DIR, basefilename, CMP_FILE_EXT);

            String msg = "Output was incorrect\n"+
                "-----Result-----\n"+ result +
                "----Expected----\n"+ compare +
                "----------------";
            
            fail(msg);
        }
    }
        
    public void setupContext(Context context)
    {
    } 
    
    
}
