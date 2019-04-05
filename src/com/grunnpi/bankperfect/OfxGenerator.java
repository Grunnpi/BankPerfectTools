package com.grunnpi.bankperfect;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfxGenerator
{
    public static void generateOfx(final String targetFilename, Map<String,List<Statement>> statementPerAccount) throws IOException, TemplateException
    {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        cfg.setClassForTemplateLoading(statementPerAccount.getClass(), "/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        Template ofxTemplate = cfg.getTemplate("ofx.ftl");

        Map<String,Object> input = new HashMap<String,Object>();
        input.put("myData",statementPerAccount);


        try (StringWriter out = new StringWriter(); Writer fileWriter = new FileWriter(new File(targetFilename));) {
            ofxTemplate.process(input, out);
            ofxTemplate.process(input, fileWriter);

            System.out.println(out.getBuffer().toString());
            out.flush();
            fileWriter.flush();
        }
    }
}
