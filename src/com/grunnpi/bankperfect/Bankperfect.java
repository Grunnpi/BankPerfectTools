package com.grunnpi.bankperfect;

import freemarker.template.TemplateException;
import io.github.jonathanlink.PDFLayoutTextStripper;
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private static final String CSV_C_TIER = "tier";
    private static final String CSV_C_DESCRIPTION = "description";
    private static final String CSV_C_AMOUNT = "amount";
    private static final String INPUT_DIRECTORY_ROOT = "input_directory_root";
    private static final String SETUP_DIRECTORY_ROOT = "setup_directory_root";
    private static final String OUTPUT_DIRECTORY_ROOT = "output_directory_root";
    private static final String SALARY_DIR = "salary_dir";
    private static final String SALARY_ACCOUNT_ID = "salary_accountId";
    private static final String SALARY_EXCLUDE = "salary_exclude";
    private static final String SALARY_MAPPING = "salary_mapping";
    private static final String CB_DIR = "CB_dir";
    private static final String CB_ACCOUNT_ID = "CB_accountId";
    private static final String CB_EXCLUDE = "CB_exclude";
    private static final String CB_MAPPING = "CB_mapping";
    private static final String IMMO_DIR = "Immo_dir";
    private static final String IMMO_ACCOUNT_ID = "Immo_accountId";
    private static final String IMMO_EXCLUDE = "Immo_exclude";
    private static final String IMMO_MAPPING = "Immo_mapping";
    private static final String RECURRENT_DIR = "Recurrent_dir";
    // Config file
    Configuration properties;
    private String[] CSV_HEADERS = { CSV_C_FILE, CSV_C_BANK, CSV_C_BRANCH, CSV_C_ACCOUNT, CSV_C_DATE, CSV_C_TIER, CSV_C_DESCRIPTION,
            CSV_C_AMOUNT };

    public static void main(String[] args) throws ParseException, IOException, TemplateException
    {
        Bankperfect bankperfect = new Bankperfect();
        bankperfect.runMe(args);
    }

    public static String[] readFileArray(File file)
    {
        List<String> excludeLines = null;
        try
        {
            if (file.exists() && FileUtils.sizeOf(file) > 0)
            {
                excludeLines = FileUtils.readLines(file, "UTF-8");
            }
            else
            {
                LOG.debug("No exclude found [{}]", file.getName());
                excludeLines = new ArrayList<String>();
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error", file.getName(), e);
        }

        String[] strings = {};
        if (excludeLines.size() > 0)
        {
            strings = (String[]) excludeLines.toArray(new String[0]);
        }
        return strings;
    }

    public static Map<String, String> readFileMap(File file)
    {
        Map<String, String> myMap = null;
        try
        {
            myMap = new HashMap<String, String>();
            if (file.exists() && FileUtils.sizeOf(file) > 0)
            {
                List<String> excludeLines = FileUtils.readLines(file, "UTF-8");
                for (String line : excludeLines)
                {
                    if (line.contains("="))
                    {
                        final String key = StringUtils.substringBefore(line, "=");
                        final String value = StringUtils.substringAfter(line, "=");
                        myMap.put(key, value);
                        //                        LOG.info("[{}]=[{}]",key,value);
                    }
                }
                LOG.debug("Map[{}].size={}", file.getAbsolutePath(), myMap.size());
            }
            else
            {
                LOG.debug("No map found [{}]", file.getName());
            }
        }
        catch (IOException e)
        {
            LOG.error("Exclude [{}] error", file.getName(), e);
        }

        return myMap;
    }

    private String getAccount(final String key)
    {
        return properties.getString(key);
    }

    private String getDir(final String key)
    {
//        String root = properties.getString(INPUT_DIRECTORY_ROOT);
//        String sub = properties.getString(key);
//        return root + "/" + sub;
        return properties.getString(key);
    }

    private File getSetupFile(final String key)
    {
        return new File(properties.getString(SETUP_DIRECTORY_ROOT), properties.getString(key));
    }

    ;

    public String getCsvCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.csv";
    }

    public String getOfxCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.ofx";
    }

    private void runMe(String[] args) throws ParseException, IOException, TemplateException
    {
        // process only if ok
        if (args.length > 0)
        {
            if (loadConfig(args[0]))
            {
                String[] responses = {
                            "1. Full"
                        ,   "2. Step by step"
                        ,   "3. Parse & dump recurrent"
                        ,   "4. Parse & dump Immo"
                        ,   "5. Parse & dump CB"
                        ,   "6. Parse & dump Salary"
                        ,   "9. Give up"
                };
                String processChoice = readConsoleMultipleChoice("Processing ?", responses);

                if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && readConsole("Parse and dump CSV ?",
                        "Y/N", "Y")))
                {

                    if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && readConsole("New CSV ?", "Y/N",
                            "Y")))
                    {
                        FileUtils.deleteQuietly(new File(getCsvCacheFilename()));
                    }

                    List<Statement> allStatements = new ArrayList<Statement>();
                    // prepare statements
                    // * for salary
                    if (processChoice.matches("1|6") || (processChoice.matches("2") && readConsole("Salaray ?", "Y/N",
                            "Y")))
                    {
                        SalaryParser salaryParser = new SalaryParser();
                        List<Statement> salaryStatements = processFiles(getAccount(SALARY_ACCOUNT_ID),
                                getDir(SALARY_DIR), getSetupFile(SALARY_EXCLUDE), getSetupFile(SALARY_MAPPING), "pdf",
                                salaryParser,false);
                        if (salaryStatements != null)
                        {
                            allStatements.addAll(salaryStatements);
                        }
                    }

                    // prepare statements
                    // * for Credit Card
                    if (processChoice.matches("1|5") || (processChoice.matches("2") && readConsole("Credit Card ?", "Y/N",
                            "Y")))
                    {
                        CreditCardParser creditCardParser = new CreditCardParser();
                        List<Statement> cbStatements = processFiles(getAccount(CB_ACCOUNT_ID), getDir(CB_DIR),
                                getSetupFile(CB_EXCLUDE), getSetupFile(CB_MAPPING), "pdf", creditCardParser,false);
                        if (cbStatements != null)
                        {
                            allStatements.addAll(cbStatements);
                        }
                    }

                    // prepare statements
                    // * for Immo
                    if (processChoice.matches("1|4") || (processChoice.matches("2") && readConsole("Immo stuff ?", "Y/N",
                            "Y")))
                    {

                        ImmoParser immoParser = new ImmoParser();
                        List<Statement> cbStatements = processFiles(getAccount(IMMO_ACCOUNT_ID), getDir(IMMO_DIR),
                                getSetupFile(IMMO_EXCLUDE), getSetupFile(IMMO_MAPPING), "pdf", immoParser,true);
                        if (cbStatements != null)
                        {
                            allStatements.addAll(cbStatements);
                        }
                    }

                    // prepare recurrent stuff
                    if (processChoice.matches("1|3") || (processChoice.matches("2") && readConsole("Recurrent stuff ?",
                            "Y/N", "Y")))
                    {
                        List<Statement> recurrentStatements = processRecurrent(getDir(RECURRENT_DIR), "csv");
                        if (recurrentStatements != null)
                        {
                            allStatements.addAll(recurrentStatements);
                        }
                    }

                    // dump all what have been prepared...
                    saveAsCsv(allStatements);
                }

                // read CSV and prepare .ofx file
                if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && readConsole(
                        "Read CSV and dump OFX ?", "Y/N", "Y")))
                {
                    csvCacheToOfx();
                }
            }
        }
        else
        {
            LOG.error("1 parameter as filePath to config.properties");
            System.exit(-1);
        }

        LOG.info("end of program");
        System.exit(0);
        LOG.info("behond end ?!");

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
            //salaryParser.readFile(filenameRBC, ".");
            break;
        case doenst:
            DoenstImport doenstImport = new DoenstImport();
            String filenameDoenst = "resource/Doenst_Beethoven.txt";
            doenstImport.readFile(filenameDoenst);
            break;
        }
    }

    private List<Statement> processFiles(final String accountSignature, final String directoryToFetch,
            final File exclude, final File mapping, final String fileExtention,
            IStatementPreparator iStatementPreparator,final boolean layoutStripper) throws IOException
    {
        List<Statement> allStatements = new ArrayList<Statement>();
        Collection<File> files = FileUtils.listFiles(new File(directoryToFetch), new String[] { fileExtention }, false);
        for (File file : files)
        {
            List<Statement> statements = null;
            LOG.info("process[{}]", file.getName());
            List<String> lines = readFullPdf(file, exclude,layoutStripper);
            Map<String, String> mappingArray = this.readFileMap(mapping);

            statements = iStatementPreparator.prepare(lines, mappingArray, accountSignature);
            if ( statements != null && statements.size() > 0 ) {
                LOG.info("[{}] statements for [{}]",statements.size(),file.getName());
                allStatements.addAll(statements);
            }
            else {
                LOG.warn("No statement for [{}]",file.getName());
            }
        }
        return allStatements;
    }

    private List<Statement> processRecurrent(final String directoryToFetch, final String fileExtention)
            throws IOException
    {
        List<Statement> statements = new ArrayList<Statement>();

        Collection<File> files = FileUtils.listFiles(new File(directoryToFetch), new String[] { fileExtention }, false);
        Map<String, List<Statement>> statementPerAccount = new HashMap<String,List<Statement>>();
        for (File file : files)
        {
            Map<String, List<Statement>> statementForThisAccount = readCsv(file.getAbsolutePath());
            if (statementForThisAccount != null)
            {
                LOG.info("process.r[{}].nbAccount[{}]", file.getName(), statementForThisAccount.size());
                for (Map.Entry<String,List<Statement>> e : statementForThisAccount.entrySet()) {
                    List<Statement> statements1 = null;
                    if (!statementPerAccount.containsKey(e.getKey()))
                    {
                        statements1 = new ArrayList<Statement>();
                        statementPerAccount.put(e.getKey(),statements1);
                    }
                    else
                    {
                        statements1 = statementPerAccount.get(e.getKey());
                    }
                    statements1.addAll(e.getValue());
                    statementPerAccount.put(e.getKey(),statements1);
                }
            }
            else
            {
                LOG.info("process.r[{}] no stuff here", file.getName());
            }
        }

        if ( statementPerAccount != null && statementPerAccount.size() > 0 ) {
            // collect single date
            Map<String, LocalDate> mapVariableDate = new HashMap<String, LocalDate>();
            for (Map.Entry<String, List<Statement>> mapStatements : statementPerAccount.entrySet())
            {
                for (Statement statement : mapStatements.getValue())
                {
                    LOG.info("Statement[{}]",statement.getDescription());
                    if (!StringUtils.isEmpty(statement.getStatementDateVariable()))
                    {
                        if (!mapVariableDate.containsKey(statement.getStatementDateVariable()))
                        {
                            mapVariableDate.put(statement.getStatementDateVariable(), null);
                        }
                    }
                    statements.add(statement);
                }
            }

            // input each
            if (mapVariableDate.size() > 0)
            {

                for (Map.Entry<String, LocalDate> entry : mapVariableDate.entrySet())
                {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Input [" + entry.getKey() + "] (YYYY-MM-DD)");
                    String statementDateString = br.readLine();
                    try
                    {
                        LocalDate statementDate = LocalDate.parse(statementDateString);
                        mapVariableDate.put(entry.getKey(), statementDate);
                    }
                    catch (Exception e)
                    {
                        LOG.error("Date cannot be cast [{}]", statementDateString, e);
                    }
                }

                // assign it
                for (Statement statement : statements)
                {
                    if (!StringUtils.isEmpty(statement.getStatementDateVariable()))
                    {
                        statement.setStatementDate(mapVariableDate.get(statement.getStatementDateVariable()));
                    }
                }
            }
        }

        return statements;
    }

    private void saveAsCsv(List<Statement> statements) throws IOException
    {
        // save to .CSV stuff
        FileWriter out2 = new FileWriter(getCsvCacheFilename());

        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(getCsvCacheFilename(), true), StandardCharsets.UTF_8));
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS));

        for (Statement statement : statements)
        {
            printer.printRecord("xxx", statement.getBank(), statement.getBranch(), statement.getAccount(),
                    statement.getStatementDate(), statement.getTier(), statement.getDescription(), statement.getAmount());
        }
        printer.close();
        LOG.info("CSV dumped {} lines", statements.size());
    }

    private boolean readConsole(final String question, final String response, final String positive) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(question + " [" + response + "] : ");
        String s = br.readLine();
        return s.equalsIgnoreCase(positive);
    }

    private String readConsoleMultipleChoice(final String question, final String[] response) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(question);
        for (String r : response)
        {
            System.out.println(" " + r);
        }
        String s = br.readLine();
        return s;
    }

    private String renameConverted(final String filename){
        return filename.replace(".convertme.",".converted.") + ".tmp";
    }

    private String fileConvert(final String filename, final String fromCharset, final String toCharset) throws IOException
    {
        String tempFilename = renameConverted(filename);
        try {
            FileInputStream fis = new FileInputStream(filename);
            byte[] contents = new byte[fis.available()];
            fis.read(contents, 0, contents.length);
            String asString = new String(contents, fromCharset);
            byte[] newBytes = asString.getBytes(toCharset);

            FileOutputStream fos = new FileOutputStream(tempFilename);
            fos.write(newBytes);
            fos.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        LOG.info("Converted[{}] from [{}] to [{}]",filename,fromCharset,toCharset);
        return tempFilename;
    }



    private Map<String, List<Statement>> readCsv(final String filename) throws IOException
    {
        String tempFilename = filename;
        if ( filename.contains(".convertme.")){
            // convert file
            tempFilename  = fileConvert(filename,"windows-1252","UTF8");
        }

        Reader in = new InputStreamReader(new FileInputStream(tempFilename), Charset.forName("UTF8"));
        //Reader in = new FileReader(getCsvCacheFilename());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(CSV_HEADERS).withFirstRecordAsHeader().withIgnoreEmptyLines().parse(in);


        Map<String, List<Statement>> statementPerAccount = new HashMap<String, List<Statement>>();
        for (CSVRecord record : records)
        {
            Statement statement = new Statement();

            statement.setBank(record.get(CSV_C_BANK));
            statement.setBranch(record.get(CSV_C_BRANCH));
            statement.setAccount(record.get(CSV_C_ACCOUNT));

            statement.setTier(record.get(CSV_C_TIER));
            statement.setDescription(record.get(CSV_C_DESCRIPTION));

            String statementDateString = record.get(CSV_C_DATE);

            if (statementDateString.startsWith("#"))
            {
                statement.setStatementDateVariable(statementDateString);
            }
            else
            {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate statementDate = LocalDate.parse(statementDateString,formatter);
                statement.setStatementDate(statementDate);
            }

            String amountString = record.get(CSV_C_AMOUNT);
            double amount = Double.parseDouble(amountString);
            statement.setAmount(amount);

            // get account ID
            if (!statementPerAccount.containsKey(statement.getBPKey()))
            {
                statementPerAccount.put(statement.getBPKey(), new ArrayList<Statement>());
            }
            statementPerAccount.get(statement.getBPKey()).add(statement);
        }

        // here we can close stream
        in.close();

        if ( filename.contains(".convertme."))
        {
            LOG.info("Delete temp file [{}] delete-{}",tempFilename,FileUtils.deleteQuietly(new File(tempFilename)));
        }

        return statementPerAccount;
    }

    // read csv file, and generate .ofx file
    private void csvCacheToOfx() throws IOException, TemplateException
    {
        Map<String, List<Statement>> statementPerAccount = readCsv(getCsvCacheFilename());

        // generate .ofx
        OfxGenerator.generateOfx(getOfxCacheFilename(), statementPerAccount);
        LOG.info("OFX generated !");
    }

    private List<String> readFullPdf(File pdfFile, File exclude, final boolean layoutStripper)
    {
        List<String> lines = new ArrayList<String>();
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        String parsedText;
        try
        {
            PDFParser parser = new PDFParser(new RandomAccessFile(pdfFile, "r"));
            parser.parse();
            cosDoc = parser.getDocument();
            pdDoc = new PDDocument(cosDoc);

            PDFTextStripper pdfStripper = new PDFTextStripper();

            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();

            if ( layoutStripper ) {
                parsedText = pdfTextStripper.getText(pdDoc);
            }
            else {
                parsedText = pdfStripper.getText(pdDoc);
            }

            // lines to ignore
            String[] strings = this.readFileArray(exclude);

            StringTokenizer stringTokenizer = new StringTokenizer(parsedText);
            while (stringTokenizer.hasMoreTokens())
            {
                String s = stringTokenizer.nextToken("\r\n");
                s = StringUtils.trimToEmpty(s);
                if (!StringUtils.startsWithAny(s, strings) && !StringUtils.isEmpty(s))
                {
                    if ( layoutStripper ) {
                        s = s.trim().replaceAll(" +", " ");
                    }
//                    LOG.info("{}",s);
                    lines.add(s);
                }
            }
            cosDoc.close();
            //System.out.println(StringUtils.replace(parsedText,"\r","###\r"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                if (cosDoc != null)
                    cosDoc.close();
                if (pdDoc != null)
                    pdDoc.close();
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
        }
        return lines;
    }

    private boolean loadConfig(String configFilePath)
    {
        boolean configIsOk = true;
        Configurations readConfig = new Configurations();
        try
        {
            properties = readConfig.properties(new File(configFilePath));
            LOG.info("Input={}", properties.getString(INPUT_DIRECTORY_ROOT));
        }
        catch (ConfigurationException e)
        {
            LOG.error("Load config[{}] failed", configFilePath, e);
            configIsOk = false;
        }
        return configIsOk;
    }

    // Process type
    public enum fileType
    {
        cb, rbc, doenst, old
    }
}
