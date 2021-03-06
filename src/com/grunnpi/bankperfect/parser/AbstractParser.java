package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.AccountID;
import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import com.grunnpi.bankperfect.tool.FileHelper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractParser.class);
    protected String accountSignature;
    protected File directoryToFetch;
    protected File exclude;
    protected File mapping;
    protected String[] fileExtention;
    protected boolean layoutStripper;

    protected String archiveDir;

    public String getArchiveDir() {
        return archiveDir;
    }

    List<BankFile> listBankFiles = new ArrayList<BankFile>();
    private String askParseAndDump;
    private String ask;

    protected static boolean areEqualByThreeDecimalPlaces(double a, double b)
    {
        DecimalFormat df = new DecimalFormat("####.##;-####.##");
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        String as = df.format(a);
        String bs = df.format(b);
        if ( !as.equals(bs) ) {
            LOG.info("[{}].equals[{}]={} <while input value are {} and {}>",as,bs,as.equals(bs),a,b);
        }
        return as.equals(bs);
    }

    public String getAccountSignature()
    {
        return accountSignature;
    }

    public void setAccountSignature(String accountSignature)
    {
        this.accountSignature = accountSignature;
    }

    public File getDirectoryToFetch()
    {
        return directoryToFetch;
    }

    public void setDirectoryToFetch(File directoryToFetch)
    {
        this.directoryToFetch = directoryToFetch;
    }

    public File getExclude()
    {
        return exclude;
    }

    public void setExclude(File exclude)
    {
        this.exclude = exclude;
    }

    public File getMapping()
    {
        return mapping;
    }

    public void setMapping(File mapping)
    {
        this.mapping = mapping;
    }

    public String[] getFileExtention()
    {
        return fileExtention;
    }

    public void setFileExtention(String[] fileExtention)
    {
        this.fileExtention = fileExtention;
    }

    public boolean isLayoutStripper()
    {
        return layoutStripper;
    }

    public void setLayoutStripper(boolean layoutStripper)
    {
        this.layoutStripper = layoutStripper;
    }

    public List<BankFile> getListBankFiles()
    {
        return listBankFiles;
    }

    public String getAsk()
    {
        return ask;
    }

    public String getAskParseAndDump()
    {
        return askParseAndDump;
    }

    public List<Statement> processFiles() throws IOException
    {
        List<Statement> allStatements = new ArrayList<>();
        List<BankFile> listBankFiles = getListBankFiles();
        for (BankFile bankFile : listBankFiles)
        {

            List<Statement> statements;
            LOG.info("process[{}]", bankFile.getFile().getName());

            List<String> lines = FileHelper.readFullPdf(bankFile.getFile(), getExclude(), isLayoutStripper());
            Map<String, String> mappingArray = FileHelper.readFileMap(getMapping());

            statements = prepare(bankFile, lines, mappingArray, getAccountSignature());
            if (statements != null && statements.size() > 0)
            {
                LOG.info("[{}] statements for [{}]", statements.size(), bankFile.getFile().getName());
                allStatements.addAll(statements);
            }
            else
            {
                LOG.warn("No statement for [{}]", bankFile.getFile().getName());
            }
        }
        return allStatements;
    }

    public void setContext(final String ask, final String askParseAndDump, final String accountSignature,
                           final String directoryToFetch, final File exclude, final File mapping, final String fileExtention,
                           final boolean layoutStripper, final String archiveDir)
    {
        this.ask = ask;
        this.askParseAndDump = askParseAndDump;
        this.archiveDir = archiveDir;
        this.accountSignature = accountSignature;
        this.directoryToFetch = new File(directoryToFetch);
        this.exclude = exclude;
        this.mapping = mapping;
        this.fileExtention = new String[] { fileExtention };
        this.layoutStripper = layoutStripper;
    }

    public boolean hasFile()
    {
        return listBankFiles.size() > 0;
    }

    public void fetchFiles()
    {

        Collection<File> files = FileUtils.listFiles(this.getDirectoryToFetch(), this.getFileExtention(), false);
        for (File file : files)
        {
            BankFile bankFile = new BankFile();
            bankFile.setFile(file);
            listBankFiles.add(bankFile);
        }
    }

    protected AccountID getAccount(final String accountSignature, final String cartType)
    {

        AccountID accountID = new AccountID();

        boolean foundIt = false;
        String[] accountPerCardIdList = accountSignature.split(";");
        for (String accountPerCardId : accountPerCardIdList)
        {
            String[] accountIdKeySplit = accountPerCardId.split("#");
            if (accountIdKeySplit[0].equals(cartType))
            {
                String[] accountIdString = accountIdKeySplit[1].split(",");
                accountID.setBank(accountIdString[0]);
                accountID.setBranch(accountIdString[1]);
                accountID.setAccount(accountIdString[2]);
                foundIt = true;
                break;
            }
        }

        if (!foundIt)
        {
            throw new RuntimeException("Cannot find signature for [" + cartType + "] in [" + accountSignature + "]");
        }

        return accountID;
    }
}
