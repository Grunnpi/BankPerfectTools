package com.grunnpi.bankperfect;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Bankperfect
{
    /** The original PDF that will be parsed. */
    public static final String PREFACE = "P://tmppierre/ya.pdf";
    /** The resulting text file. */
    public static final String RESULT = "P://tmppierre//preface.txt";

    private static final Logger LOG = LoggerFactory.getLogger(Bankperfect.class);


    // Config file
    Configuration properties;
    private static final String INPUT_DIRECTORY_ROOT = "input_directory_root";
    private static final String INPUT_DIRECTORY_SALARY = "input_directory_salary";
    private static final String SALARY_EXCLUDE = "salary_exclude";
    private static final String INPUT_DIRECTORY_CREDIT_CARD = "input_directory_credit_card";
    private static final String INPUT_DIRECTORY_RECCURENT = "input_directory_reccurent";

    private  String getSalaryDir()
    {
        String root = properties.getString(INPUT_DIRECTORY_ROOT);
        String sub = properties.getString(INPUT_DIRECTORY_SALARY);

        return root + "/" + sub;
    }

    private File getSalaryExclude() {
        return new File(properties.getString(INPUT_DIRECTORY_ROOT),properties.getString(SALARY_EXCLUDE));
    }

    // Process type
    public enum fileType
    {
        cb, rbc, doenst, old
    };

   public static void main(String[] args) throws ParseException, IOException
    {
        Bankperfect bankperfect = new Bankperfect();
        bankperfect.runMe(args);
    }


    private boolean loadConfig(String configFilePath) {
        boolean configIsOk = true;
        Configurations readConfig = new Configurations();
        try
        {
            properties = readConfig.properties(new File(configFilePath));
            LOG.info("Input={}",properties.getString(INPUT_DIRECTORY_ROOT));
        }
        catch (ConfigurationException e)
        {
            LOG.error("Load config[{}] failed",configFilePath,e);
            configIsOk = false;
        }
        return configIsOk;
    }

    private void processFiles(final String directoryToFetch, final File exclude, final String fileExtention, IStatementPreparator iStatementPreparator) {
        Collection<File> files = FileUtils.listFiles(new File(directoryToFetch), new String[] { fileExtention }, false);
        for ( File file : files )  {
            LOG.info("process[{}]",file.getName());
            List<String> lines = readFullPdf(file,exclude);
            iStatementPreparator.prepare(lines);
        }
    }

    private void runMe(String[] args) throws ParseException
    {
        // process only if ok
        if (args.length > 0)
        {
            if (loadConfig(args[0]))
            {
                // prepare statements

                // for salary
                SalaryParser salaryParser = new SalaryParser();
                processFiles(getSalaryDir(), getSalaryExclude(), "pdf", salaryParser);
            }
        }
        else
        {
            LOG.error("1 parameter as filePath to config.properties");
            System.exit(-1);
        }

        System.exit(0);


        fileType myFileType = fileType.rbc;
        switch (myFileType)
        {
        case cb:
            BILImportCB bilImportCB = new BILImportCB();
            String filenameCB = "resource/CB-BIL.txt";
            bilImportCB.readFile(filenameCB);
            break;
        case old:
            RBCImportSalaireOld rbcImportSalaireOld = new RBCImportSalaireOld();
            String filenameRBCold = "resource/RBC-SalaireOld.txt";
            rbcImportSalaireOld.readFile(filenameRBCold, ".");
            break;
        case rbc:
            SalaryParser salaryParser = new SalaryParser();
            String filenameRBC = "resource/RBC-Salaire.txt";
            salaryParser.readFile(filenameRBC, ".");
            break;
        case doenst:
            DoenstImport doenstImport = new DoenstImport();
            String filenameDoenst = "resource/Doenst_Beethoven.txt";
            doenstImport.readFile(filenameDoenst);
            break;
        }
    }

    private List<String> readFullPdf(File pdfFile, File exclude){

        List<String> lines = new ArrayList<String>();

        PDFParser parser = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        PDFTextStripper pdfStripper;

        String parsedText;
        String fileName = PREFACE;

        List<String> excludeLines = null;
        try
        {
            if ( FileUtils.sizeOf(exclude) >  0 ) {
                excludeLines = FileUtils.readLines(exclude,"UTF-8");
            }
            else {
                LOG.warn("No exclude found [{}]",exclude.getName());
                excludeLines = new ArrayList<String>();
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error",exclude.getName(),e);
        }

        try {
            parser = new PDFParser(new RandomAccessFile(pdfFile,"r"));
            parser.parse();

            pdfStripper = new PDFTextStripper();

            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

            parsedText = pdfStripper.getText(pdDoc);

            String[] strings = {};
            if ( excludeLines.size()> 0 ) {
                strings = Arrays

                        (String[]) excludeLines.toArray();
            }

            //System.out.println(parsedText.replaceAll("[^A-Za-z0-9. ]+", ""));
            StringTokenizer stringTokenizer = new StringTokenizer(parsedText);
            while ( stringTokenizer.hasMoreTokens() ) {
                String s = stringTokenizer.nextToken("\r\n");
                //System.out.println("<" + s + ">");
                if (!StringUtils.startsWithAny(s,strings) ) {
                    lines.add(s);
                }
                else {
                    LOG.warn("Ignore line[{}]",s);
                }
            }
            //System.out.println(StringUtils.replace(parsedText,"\r","###\r"));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (cosDoc != null)
                    cosDoc.close();
                if (pdDoc != null)
                    pdDoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return lines;
    }

}
