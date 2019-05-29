package com.grunnpi.bankperfect;

import com.grunnpi.bankperfect.data.Statement;
import com.grunnpi.bankperfect.parser.CreditCardParser;
import com.grunnpi.bankperfect.parser.IStatementPreparator;
import com.grunnpi.bankperfect.parser.ImmoParser;
import com.grunnpi.bankperfect.parser.SalaryParser;
import com.grunnpi.bankperfect.tool.ConsoleHelper;
import com.grunnpi.bankperfect.tool.FileHelper;
import com.grunnpi.bankperfect.tool.OfxGenerator;
import freemarker.template.TemplateException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class Bankperfect
{
    /** The resulting text file. */
    private static final Logger LOG = getLogger(Bankperfect.class);

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
    private static final String KEY_SALARY = "SALARY";
    private static final String KEY_CREDITCARD = "CREDITCARD";
    private static final String KEY_IMMO = "IMMO";
    // Config file
    private Configuration properties;
    private Map<String, IStatementPreparator> statementPreparators;
    private String[] CSV_HEADERS = { CSV_C_FILE, CSV_C_BANK, CSV_C_BRANCH, CSV_C_ACCOUNT, CSV_C_DATE, CSV_C_TIER,
            CSV_C_DESCRIPTION, CSV_C_AMOUNT };

    public static void main(String[] args) throws IOException, TemplateException
    {
        Bankperfect bankperfect = new Bankperfect();
        if (ConsoleHelper.readConsole("Debug ?", "1/0", "1"))
        {
            Resource log4jResource = new ClassPathResource("log4j.debug.xml");
            try
            {
                DOMConfigurator.configure(log4jResource.getURL());
            }
            catch (Exception e)
            {
                LOG.error("Something wrong append with log4j configuration, please check", e);
            }
        }
        bankperfect.runMe(args);
    }

    private Map<String, IStatementPreparator> getStatementPreparators()
    {
        if (statementPreparators == null)
        {
            statementPreparators = new HashMap<>();
        }
        return statementPreparators;
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

    private String getCsvCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.csv";
    }

    private String getOfxCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.ofx";
    }

    private void addParser(final String parserName, IStatementPreparator parser, final String accountSignature,
            final String directoryToFetch, final File exclude, final File mapping, final String fileExtention,
            final boolean layoutStripper)
    {
        parser.setContext(accountSignature, directoryToFetch, exclude, mapping, fileExtention, layoutStripper);

        getStatementPreparators().put(parserName, parser);
    }

    private void runMe(String[] args) throws IOException, TemplateException
    {
        // process only if ok
        if (args.length > 0)
        {
            if (loadConfig(args[0]))
            {
                // check all file type available or not
                addParser(KEY_SALARY, new SalaryParser(), getAccount(SALARY_ACCOUNT_ID), getDir(SALARY_DIR),
                        getSetupFile(SALARY_EXCLUDE), getSetupFile(SALARY_MAPPING), "pdf", false);
                addParser(KEY_CREDITCARD, new CreditCardParser(), getAccount(CB_ACCOUNT_ID), getDir(CB_DIR),
                        getSetupFile(CB_EXCLUDE), getSetupFile(CB_MAPPING), "pdf", false);
                addParser(KEY_IMMO, new ImmoParser(), getAccount(IMMO_ACCOUNT_ID), getDir(IMMO_DIR),
                        getSetupFile(IMMO_EXCLUDE), getSetupFile(IMMO_MAPPING), "pdf", true);

                // fetch out list of files per preparators
                for (Map.Entry<String, IStatementPreparator> preparatorEntry : getStatementPreparators().entrySet())
                {
                    preparatorEntry.getValue().fetchFiles();
                }

                List<String> responsesList = new ArrayList<>();
                responsesList.add("1. Full");
                responsesList.add("2. Step by step");
                responsesList.add("3. Parse & dump recurrent");

                //                String[] responses = {
                //                            "1. Full"
                //                        ,   "2. Step by step"
                //                        ,   "3. Parse & dump recurrent"
                //                        ,   "4. Parse & dump Immo"
                //                        ,   "5. Parse & dump CB"
                //                        ,   "6. Parse & dump Salary"
                //                        ,   "9. Give up"
                //                };

                if (getParser(KEY_IMMO).hasFile())
                {
                    responsesList.add("4. Parse & dump Immo");
                }
                if (getParser(KEY_CREDITCARD).hasFile())
                {
                    responsesList.add("5. Parse & dump CB");
                }
                if (getParser(KEY_SALARY).hasFile())
                {
                    responsesList.add("6. Parse & dump Salary");
                }
                responsesList.add("9. Give up");

                String[] responses = new String[responsesList.size()];
                responses = responsesList.toArray(responses);

                String processChoice = ConsoleHelper.readConsoleMultipleChoice("Processing ?", responses);

                if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && ConsoleHelper
                        .readConsole("Parse and dump CSV ?", "Y/N", "Y")))
                {

                    if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("New CSV ?", "Y/N", "Y")))
                    {
                        FileUtils.deleteQuietly(new File(getCsvCacheFilename()));
                    }

                    List<Statement> allStatements = new ArrayList<>();
                    // prepare statements
                    // * for salary
                    if (processChoice.matches("1|6") || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("Salaray ?", "Y/N",
                            "Y")))
                    {
                        List<Statement> salaryStatements = processFiles(KEY_SALARY);
                        if (salaryStatements != null)
                        {
                            allStatements.addAll(salaryStatements);
                        }
                    }

                    // prepare statements
                    // * for Credit Card
                    if (processChoice.matches("1|5") || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("Credit Card ?", "Y/N", "Y")))
                    {
                        List<Statement> cbStatements = processFiles(KEY_CREDITCARD);
                        if (cbStatements != null)
                        {
                            allStatements.addAll(cbStatements);
                        }
                    }

                    // prepare statements
                    // * for Immo
                    if (processChoice.matches("1|4") || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("Immo stuff ?", "Y/N", "Y")))
                    {
                        List<Statement> cbStatements = processFiles(KEY_IMMO);
                        if (cbStatements != null)
                        {
                            allStatements.addAll(cbStatements);
                        }
                    }

                    // prepare recurrent stuff
                    if (processChoice.matches("1|3") || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("Recurrent stuff ?",
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
                if (processChoice.matches("1|3|4|5|6") || (processChoice.matches("2") && ConsoleHelper.readConsole(
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
    }

    private IStatementPreparator getParser(String keySalary)
    {
        return getStatementPreparators().get(keySalary);
    }

    private List<Statement> processFiles(final String key)
    {
        IStatementPreparator preparator = getStatementPreparators().get(key);

        List<Statement> allStatements = new ArrayList<>();
        Collection<File> files = FileUtils
                .listFiles(preparator.getDirectoryToFetch(), preparator.getFileExtention(), false);
        for (File file : files)
        {
            List<Statement> statements;
            LOG.info("process[{}]", file.getName());
            List<String> lines = FileHelper.readFullPdf(file, preparator.getExclude(), preparator.isLayoutStripper());
            Map<String, String> mappingArray = FileHelper.readFileMap(preparator.getMapping());

            statements = preparator.prepare(lines, mappingArray, preparator.getAccountSignature());
            if (statements != null && statements.size() > 0)
            {
                LOG.info("[{}] statements for [{}]", statements.size(), file.getName());
                allStatements.addAll(statements);
            }
            else
            {
                LOG.warn("No statement for [{}]", file.getName());
            }
        }
        return allStatements;
    }

    private List<Statement> processRecurrent(final String directoryToFetch, final String fileExtention)
            throws IOException
    {
        List<Statement> statements = new ArrayList<>();

        Collection<File> files = FileUtils.listFiles(new File(directoryToFetch), new String[] { fileExtention }, false);
        Map<String, List<Statement>> statementPerAccount = new HashMap<>();
        for (File file : files)
        {
            Map<String, List<Statement>> statementForThisAccount = readCsv(file.getAbsolutePath());
            if (statementForThisAccount != null)
            {
                LOG.info("process.r[{}].nbAccount[{}]", file.getName(), statementForThisAccount.size());
                for (Map.Entry<String, List<Statement>> e : statementForThisAccount.entrySet())
                {
                    List<Statement> statements1;
                    if (!statementPerAccount.containsKey(e.getKey()))
                    {
                        statements1 = new ArrayList<>();
                        statementPerAccount.put(e.getKey(), statements1);
                    }
                    else
                    {
                        statements1 = statementPerAccount.get(e.getKey());
                    }
                    statements1.addAll(e.getValue());
                    statementPerAccount.put(e.getKey(), statements1);
                }
            }
            else
            {
                LOG.info("process.r[{}] no stuff here", file.getName());
            }
        }

        if (statementPerAccount.size() > 0)
        {
            // collect single date
            Map<String, LocalDate> mapVariableDate = new HashMap<>();
            for (Map.Entry<String, List<Statement>> mapStatements : statementPerAccount.entrySet())
            {
                for (Statement statement : mapStatements.getValue())
                {
                    LOG.info("Statement[{}]", statement.getDescription());
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
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(getCsvCacheFilename(), true), StandardCharsets.UTF_8));
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS));

        for (Statement statement : statements)
        {
            printer.printRecord("xxx", statement.getBank(), statement.getBranch(), statement.getAccount(),
                    statement.getStatementDate(), statement.getTier(), statement.getDescription(),
                    statement.getAmount());
        }
        printer.close();
        LOG.info("CSV dumped {} lines", statements.size());
    }

    private Map<String, List<Statement>> readCsv(final String filename) throws IOException
    {
        String tempFilename = filename;
        if (filename.contains(".convertme."))
        {
            // convert file
            tempFilename = FileHelper.fileConvert(filename, "windows-1252", "UTF8");
        }

        Reader in = new InputStreamReader(new FileInputStream(tempFilename), Charset.forName("UTF8"));
        //Reader in = new FileReader(getCsvCacheFilename());
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(CSV_HEADERS).withFirstRecordAsHeader()
                .withIgnoreEmptyLines().parse(in);

        Map<String, List<Statement>> statementPerAccount = new HashMap<>();
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
                LocalDate statementDate = LocalDate.parse(statementDateString, formatter);
                statement.setStatementDate(statementDate);
            }

            String amountString = record.get(CSV_C_AMOUNT);
            double amount = Double.parseDouble(amountString);
            statement.setAmount(amount);

            // get account ID
            if (!statementPerAccount.containsKey(statement.getBPKey()))
            {
                statementPerAccount.put(statement.getBPKey(), new ArrayList<>())
            }
            statementPerAccount.get(statement.getBPKey()).add(statement);
        }

        // here we can close stream
        in.close();

        if (filename.contains(".convertme."))
        {
            LOG.info("Delete temp file [{}] delete-{}", tempFilename, FileUtils.deleteQuietly(new File(tempFilename)));
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
}
