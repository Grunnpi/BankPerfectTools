package com.grunnpi.bankperfect.tool;

import com.grunnpi.bankperfect.data.Statement;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfxGenerator
{
    public static void generateOfx(final String targetFilename, Map<String,List<Statement>> statementPerAccount) throws IOException, TemplateException
    {
        FileUtils.deleteQuietly(new File(targetFilename));

        Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        cfg.setClassForTemplateLoading(statementPerAccount.getClass(), "/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        Template ofxTemplate = cfg.getTemplate("ofx.ftl");

        Map<String,Object> input = new HashMap<String,Object>();
        input.put("myData",statementPerAccount);


        try (StringWriter out = new StringWriter(); BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFilename, true), StandardCharsets.UTF_8))) {
//        try (StringWriter out = new StringWriter(); Writer fileWriter = new FileWriter(new File(targetFilename));) {
            ofxTemplate.process(input, fileWriter);
            //ofxTemplate.process(input, out);
            //System.out.println(out.getBuffer().toString());
            //out.flush();
            fileWriter.flush();
        }
    }
}
