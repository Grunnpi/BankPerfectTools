package com.grunnpi.bankperfect.tool;

import com.grunnpi.bankperfect.data.Statement;
import freemarker.template.*;
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
        cfg.setClassForTemplateLoading(OfxGenerator.class, "/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);


        Template ofxTemplate = null;
        try {
            ofxTemplate = cfg.getTemplate("ofx.ftl");
        } catch (TemplateNotFoundException e) {
            // When running from an IDE like IntelliJ, class loading resources may fail for some reason (Gradle is OK)
            // Working dir is module dir
            File dir = new File("src/main/resources/");
            if (!dir.exists()) {
                // Working dir is base module dir
                dir = new File("target/classes/");
            }
            if (dir.exists() && new File(dir, "ofx.ftl").exists()) {
                cfg.setDirectoryForTemplateLoading(dir);
                ofxTemplate =cfg.getTemplate("ofx.ftl");
            } else {
                throw e;
            }
        }


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
