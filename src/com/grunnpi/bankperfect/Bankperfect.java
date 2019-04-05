package com.grunnpi.bankperfect;

import freemarker.template.TemplateException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

public class Bankperfect
{
    /** The original PDF that will be parsed. */
    public static final String PREFACE = "P://tmppierre/ya.pdf";
    /** The resulting text file. */
    public static final String RESULT = "P://tmppierre//preface.txt";

    private static final Logger LOG = LoggerFactory.getLogger(Bankperfect.class);


    private static final String CSV_C_FILE = "file";
    private static final String CSV_C_BANK = "bank";
    private static final String CSV_C_BRANCH = "branch";
    private static final String CSV_C_ACCOUNT = "account";
    private static final String CSV_C_DATE = "date";
    private static final String CSV_C_DESCRIPTION = "description";
    private static final String CSV_C_AMOUNT = "amount";

    private String[] CSV_HEADERS = { CSV_C_FILE, CSV_C_BANK, CSV_C_BRANCH, CSV_C_ACCOUNT, CSV_C_DATE, CSV_C_DESCRIPTION, CSV_C_AMOUNT};

    // Config file
    Configuration properties;
    private static final String INPUT_DIRECTORY_ROOT = "input_directory_root";
    private static final String SETUP_DIRECTORY_ROOT = "setup_directory_root";

    private static final String OUTPUT_DIRECTORY_ROOT = "output_directory_root";

    private static final String SALARY_DIR = "salary_dir";
    private static final String SALARY_ACCOUNT_ID = "salary_accountId";
    private static final String SALARY_EXCLUDE = "salary_exclude";
    private static final String SALARY_MAPPING = "salary_mapping";

    private static final String CB_ACCOUNT_ID = "CB_accountId";
    private static final String CB_DIR = "CB_dir";
    private static final String CB_EXCLUDE = "CB_exclude";
    private static final String CB_MAPPING = "CB_mapping";


    private static final String INPUT_DIRECTORY_CREDIT_CARD = "input_directory_credit_card";
    private static final String INPUT_DIRECTORY_RECCURENT = "input_directory_reccurent";

    private  String getSalaryAccount() {
        return properties.getString(SALARY_ACCOUNT_ID);
    }

    private  String getCreditCardAccount() {
        return properties.getString(CB_ACCOUNT_ID);
    }

    private  String getSalaryDir()
    {
        String root = properties.getString(INPUT_DIRECTORY_ROOT);
        String sub = properties.getString(SALARY_DIR);

        return root + "/" + sub;
    }

    private  String getCreditCardDir()
    {
        String root = properties.getString(INPUT_DIRECTORY_ROOT);
        String sub = properties.getString(CB_DIR);

        return root + "/" + sub;
    }

    private File getCreditCardExclude() {
        return new File(properties.getString(SETUP_DIRECTORY_ROOT),properties.getString(CB_EXCLUDE));
    }
    private File getCreditCardMapping() {
        return new File(properties.getString(SETUP_DIRECTORY_ROOT),properties.getString(CB_MAPPING));
    }

    private File getSalaryExclude() {
        return new File(properties.getString(SETUP_DIRECTORY_ROOT),properties.getString(SALARY_EXCLUDE));
    }
    private File getSalaryMapping() {
        return new File(properties.getString(SETUP_DIRECTORY_ROOT),properties.getString(SALARY_MAPPING));
    }

    public String getCsvCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.csv";
    }

    public String getOfxCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.ofx";
    }

    // Process type
    public enum fileType
    {
        cb, rbc, doenst, old
    };

   public static void main(String[] args) throws ParseException, IOException, TemplateException
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

    private List<Statement>  processFiles(final String accountSignature, final String directoryToFetch, final File exclude, final File mapping, final String fileExtention, IStatementPreparator iStatementPreparator)
            throws IOException
    {
        List<Statement> statements = null;
        Collection<File> files = FileUtils.listFiles(new File(directoryToFetch), new String[] { fileExtention }, false);
        for ( File file : files )  {
            LOG.info("process[{}]",file.getName());
            List<String> lines = readFullPdf(file,exclude);
            Map<String,String> mappingArray = this.readFileMap(mapping);

            statements = iStatementPreparator.prepare(lines,mappingArray);
        }
        return statements;
    }

    private void saveAsCsv(final String accountSignature, List<Statement> statements) throws IOException
    {

        // save to .CSV stuff
        FileWriter out = new FileWriter(getCsvCacheFilename());
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS));

        String[] accountId = accountSignature.split(",");

        for (Statement statement : statements)
        {
            LOG.info("{}",statement);
            printer.printRecord("xxx",accountId[0],accountId[1],accountId[2],statement.getStatementDate(),statement.getDescription(),statement.getAmount());
        }
        printer.close();
    }

    private void runMe(String[] args) throws ParseException, IOException, TemplateException
    {
        // process only if ok
        if (args.length > 0)
        {

            if (loadConfig(args[0]))
            {
                List<Statement> allStatements = new ArrayList<Statement>();
                // prepare statements
                // * for salary
                SalaryParser salaryParser = new SalaryParser();
                List<Statement> salaryStatements = processFiles(getSalaryAccount(),getSalaryDir(), getSalaryExclude(), getSalaryMapping(), "pdf", salaryParser);
                if ( salaryStatements != null ) {
                    allStatements.addAll(salaryStatements);
                }

                // prepare statements
                // * for Credit Card
                CreditCardParser creditCardParser = new CreditCardParser();
                List<Statement> cbStatements = processFiles(getCreditCardAccount(),getCreditCardDir(), getCreditCardExclude(), getCreditCardMapping(), "pdf", creditCardParser);

                // read CSV and prepare .ofx file
                csvCacheToOfx();
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

    // read csv file, and generate .ofx file
    private void csvCacheToOfx() throws IOException, TemplateException
    {
        Reader in = new FileReader(getCsvCacheFilename());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withHeader(CSV_HEADERS)
                .withFirstRecordAsHeader()
                .parse(in);

        Map<String,List<Statement>> statementPerAccount = new HashMap<String,List<Statement>>();
        for (CSVRecord record : records) {
            Statement statement = new Statement();

            statement.setBank(record.get(CSV_C_BANK));
            statement.setBranch(record.get(CSV_C_BRANCH));
            statement.setAccount(record.get(CSV_C_ACCOUNT));

            statement.setDescription(record.get(CSV_C_DESCRIPTION));

            String statementDateString = record.get(CSV_C_DATE);
            LocalDate statementDate = LocalDate.parse(statementDateString);

            statement.setStatementDate(statementDate);

            String amountString = record.get(CSV_C_AMOUNT);
            double amount = Double.parseDouble(amountString);
            statement.setAmount(amount);

            // get account ID
            if ( !statementPerAccount.containsKey(statement.getBPKey())) {
                statementPerAccount.put(statement.getBPKey(),new ArrayList<Statement>());
            }
            statementPerAccount.get(statement.getBPKey()).add(statement);
        }

        // generate .ofx
        for ( Map.Entry<String,List<Statement>> accountStatement : statementPerAccount.entrySet()) {
            // generate account ID
            LOG.info("BPKey[{}]",accountStatement.getKey());

            // loop statements
            for( Statement statement : accountStatement.getValue() ) {
                LOG.info("{}",statement);
            }
        }

        OfxGenerator.generateOfx(getOfxCacheFilename(),statementPerAccount);
    }

    public static String[] readFileArray(File file) {
        List<String> excludeLines = null;
        try
        {
            if ( FileUtils.sizeOf(file) >  0 ) {
                excludeLines = FileUtils.readLines(file,"UTF-8");
            }
            else {
                LOG.warn("No exclude found [{}]",file.getName());
                excludeLines = new ArrayList<String>();
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error",file.getName(),e);
        }

        String[] strings = {};
        if ( excludeLines.size()> 0 ) {
            strings = (String[]) excludeLines.toArray(new String[0]);
        }
        return strings;
    }

    public static Map<String,String> readFileMap(File file) {
        Map<String,String> myMap = null;
        try
        {
            myMap = new HashMap<String,String>();
            if ( FileUtils.sizeOf(file) >  0 ) {
                List<String> excludeLines = FileUtils.readLines(file,"UTF-8");
                for( String line : excludeLines ) {
                    if ( line.contains("=")) {
                        final String key = StringUtils.substringBefore(line,"=");
                        final String value = StringUtils.substringAfter(line,"=");
                        myMap.put(key,value);
//                        LOG.info("[{}]=[{}]",key,value);
                    }
                }
                LOG.info("Map[{}].size={}",file.getAbsolutePath(),myMap.size());
            }
            else {
                LOG.warn("No map found [{}]",file.getName());
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error",file.getName(),e);
        }

        return myMap;
    }


    private List<String> readFullPdf(File pdfFile, File exclude){
        List<String> lines = new ArrayList<String>();
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        String parsedText;
        try {
            PDFParser parser = new PDFParser(new RandomAccessFile(pdfFile,"r"));
            parser.parse();
            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

            PDFTextStripper pdfStripper = new PDFTextStripper();
            parsedText = pdfStripper.getText(pdDoc);

            // lines to ignore
            String[] strings = this.readFileArray(exclude);

            StringTokenizer stringTokenizer = new StringTokenizer(parsedText);
            while ( stringTokenizer.hasMoreTokens() ) {
                String s = stringTokenizer.nextToken("\r\n");
                if (!StringUtils.startsWithAny(s,strings) ) {
                    //LOG.info("{}",s);
                    lines.add(s);
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
