package com.grunnpi.bankperfect;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import com.grunnpi.bankperfect.parser.*;
import com.grunnpi.bankperfect.tool.ConsoleHelper;
import com.grunnpi.bankperfect.tool.FileHelper;
import com.grunnpi.bankperfect.tool.OfxGenerator;
import freemarker.template.TemplateException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class Bankperfect
{
    /** The resulting text file. */
    private static final Logger LOG = getLogger(Bankperfect.class);

    private static final String INPUT_DIRECTORY_ROOT = "input_directory_root";
    private static final String SETUP_DIRECTORY_ROOT = "setup_directory_root";
    private static final String OUTPUT_DIRECTORY_ROOT = "output_directory_root";

    private static final String KEY_SALARY = "salary";
    private static final String KEY_CREDITCARD = "CB";
    private static final String KEY_IMMO = "Immo";
    private static final String KEY_RECURRENT = "Recurrent";


    private static final String APPLICATION_NAME = "AuPairTool";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "AuPair.json";

    // Config file
    private Configuration properties;
    private Map<String, IStatementPreparator> statementPreparators;



    public static void main(String[] args) throws IOException, TemplateException, GeneralSecurityException {
        Bankperfect bankperfect = new Bankperfect();

        if (args.length > 0)
        {
            for ( String arg : args ) {
                if ( "--debug=yes".equals(arg) ) {
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
                }
                if ( "--debug=force".equals(arg) ) {
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

    private String getKey(final String key)
    {
        return properties.getString(key);
    }

    private File getSetupFile(final String key)
    {
        if (!StringUtils.isBlank(properties.getString(key)))
        {
            return new File(properties.getString(SETUP_DIRECTORY_ROOT), properties.getString(key));
        }
        else
            return null;
    }

    private String getCsvCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.csv";
    }

    private String getOfxCacheFilename()
    {
        return properties.getString(OUTPUT_DIRECTORY_ROOT) + "/output.ofx";
    }

    private void addParser(final String parserName, IStatementPreparator parser)
    {
        parser.setContext(getKey(parserName + "_ask"), getKey(parserName + "_askParseAndDump"),
                getKey(parserName + "_accountId"), getKey(parserName + "_dir"), getSetupFile(parserName + "_exclude"),
                getSetupFile(parserName + "_mapping"), getKey(parserName + "_fileExtention"),
                Boolean.valueOf(getKey(parserName + "_layoutStripper")), getKey(parserName + "_archive"));
        getStatementPreparators().put(parserName, parser);
    }
    static int readTimeout = 60000;
    static int connectTimeout = 60000;
    public static HttpRequestInitializer getRequestInitializer(final GoogleCredential requestInitializer) {
        return httpRequest -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(readTimeout);
            httpRequest.setReadTimeout(connectTimeout);
        };
    }
    private void runMe(String[] args) throws IOException, TemplateException, GeneralSecurityException {

        // TODO(developer): Replace these variables before running the sample.
        String projectId = "AuPairTool";
        File credentialsPath = new File("AuPair.json");

        // Load credentials from JSON key file. If you can't set the GOOGLE_APPLICATION_CREDENTIALS
        // environment variable, you can explicitly load the credentials file to construct the
        // credentials.
//        GoogleCredentials credentials;
//        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
//            credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
//        }
//
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getRequestInitializer(credentials))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//
//        // Print the names and IDs for up to 10 files.
//        FileList result = service.files().list()
//                .setPageSize(10)
//                .setFields("nextPageToken, files(id, name)")
//                .execute();
//        List<com.google.api.services.drive.model.File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (com.google.api.services.drive.model.File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }
//        System.exit(-1);


        // process only if ok
        if (args.length > 0)
        {
            if (loadConfig(args[0]))
            {
                // check all file type available or not
                addParser(KEY_RECURRENT, new RecurrentParser());
                addParser(KEY_IMMO, new ImmoParser());
                addParser(KEY_CREDITCARD, new CreditCardParser());
//                addParser(KEY_SALARY, new SalaryParser());

                // fetch out list of files per preparators
                List<String> responsesList = new ArrayList<>();
                responsesList.add("1. Full & archive");
                responsesList.add("2. Step by step");

                String fullChoice = "1";
                for (Map.Entry<String, IStatementPreparator> preparatorEntry : getStatementPreparators().entrySet())
                {
                    preparatorEntry.getValue().fetchFiles();
                    if (preparatorEntry.getValue().hasFile())
                    {
                        responsesList.add(preparatorEntry.getValue().getAskParseAndDump() + ". Parse & Dump "
                                + preparatorEntry.getValue().getAsk() + " ?");
                        fullChoice += "|" + preparatorEntry.getValue().getAskParseAndDump();
                    }
                }

                responsesList.add("9. Give up");

                // start processing workflow

                String processChoice = ConsoleHelper.readConsoleMultipleChoice("Processing ?", responsesList);
                if (processChoice.matches(fullChoice) || (processChoice.matches("2") && ConsoleHelper
                        .readConsole("Parse and dump stuff ?", "1/0", "1")))
                {
                    if (processChoice.matches(fullChoice) || (processChoice.matches("2") && ConsoleHelper
                            .readConsole("New CSV cache file ?", "1/0", "1")))
                    {
                        FileUtils.deleteQuietly(new File(getCsvCacheFilename()));
                    }

                    List<Statement> allStatements = new ArrayList<>();

                    // loop each parser
                    for (Map.Entry<String, IStatementPreparator> preparatorEntry : getStatementPreparators().entrySet())
                    {
                        String auto = "1|" + preparatorEntry.getValue().getAskParseAndDump();
                        if (processChoice.matches(auto) || (processChoice.matches("2") && ConsoleHelper
                                .readConsole(preparatorEntry.getValue().getAsk() + " ?", "1/0", "1")))
                        {
                            List<Statement> salaryStatements = preparatorEntry.getValue().processFiles();
                            if (salaryStatements != null)
                            {
                                allStatements.addAll(salaryStatements);
                            }
                        }
                    }

                    // dump all what have been prepared...
                    FileHelper.saveAsCsv(getCsvCacheFilename(), allStatements);
                }

                // read CSV and prepare .ofx file
                if (processChoice.matches(fullChoice) || (processChoice.matches("2") && ConsoleHelper.readConsole(
                        "Read CSV and dump OFX ?", "1/0", "1")))
                {
                    csvCacheToOfx();
                }

                boolean hasSomethingToArchive = false;
                for (Map.Entry<String, IStatementPreparator> preparatorEntry : getStatementPreparators().entrySet())
                {
                    if (preparatorEntry.getValue().hasFile())
                    {
                        for (BankFile bankFile : preparatorEntry.getValue().getListBankFiles())
                        {

                            if (bankFile.isToMoveToArchive())
                            {
                                hasSomethingToArchive = true;
                                break;
                            }
                            else if (bankFile.isToRename())
                            {
                                hasSomethingToArchive = true;
                                break;
                            }
                        }
                    }
                }

                // Archive files
                if (hasSomethingToArchive && ConsoleHelper.readConsole("Archive files ?", "1/0", "1"))
                {
                    // fetch out list of files per preparators
                    for (Map.Entry<String, IStatementPreparator> preparatorEntry : getStatementPreparators().entrySet())
                    {
                        if (preparatorEntry.getValue().hasFile())
                        {
                            for (BankFile bankFile : preparatorEntry.getValue().getListBankFiles())
                            {

                                if (bankFile.isToMoveToArchive()) {
                                    LOG.info("Parser[{}] > Archive [{}]:[{}]", preparatorEntry.getKey(),
                                            bankFile.getFile().getName(), bankFile.getTargetName());
                                    FileUtils.moveFile(bankFile.getFile(), new File(bankFile.getTargetName()));
                                    bankFile.getFile().delete();
                                } else if (bankFile.isToRename()) {
                                    LOG.info("Parser[{}] > Rename [{}]:[{}]", preparatorEntry.getKey(),
                                            bankFile.getFile().getName(), bankFile.getTargetName());
                                    FileUtils.moveFile(bankFile.getFile(), new File(bankFile.getTargetName()));
                                }
                            }
                        }
                    }
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

    private List<Statement> processFiles(final String key) throws IOException
    {
        IStatementPreparator preparator = getStatementPreparators().get(key);
        return preparator.processFiles();
    }

    // read csv file, and generate .ofx file
    private void csvCacheToOfx() throws IOException, TemplateException
    {
        Map<String, List<Statement>> statementPerAccount = FileHelper.readCsv(getCsvCacheFilename());

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
