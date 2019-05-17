package com.grunnpi.bankperfect;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AbstractParser
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractParser.class);

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

    protected String accountSignature;
    protected File directoryToFetch;
    protected File exclude;
    protected File mapping;
    protected String[] fileExtention;
    protected boolean layoutStripper;

    List<BankFile> listBankFiles = new ArrayList<BankFile>();

    public void setContext(final String accountSignature, final String directoryToFetch, final File exclude,
            final File mapping, final String fileExtention,final boolean layoutStripper){
        this.accountSignature = accountSignature;
        this.directoryToFetch = new File(directoryToFetch);
        this.exclude = exclude;
        this.mapping = mapping;
        this.fileExtention = new String[] { fileExtention };
        this.layoutStripper = layoutStripper;
    };

    public boolean hasFile(){
        return listBankFiles.size() > 0;
    }

    public void fetchFiles() {

        Collection<File> files = FileUtils
                .listFiles(this.getDirectoryToFetch(), this.getFileExtention(), false);

        for ( File file : files ) {
            BankFile bankFile = new BankFile();
            bankFile.setFile(file);
            listBankFiles.add(bankFile);
        }
    }


    protected AccountID getAccount(final String accountSignature, final String cartType ) {

        AccountID accountID = new AccountID();

        boolean foundIt = false;
        String[] accountPerCardIdList = accountSignature.split(";");
        for ( String accountPerCardId : accountPerCardIdList ) {
            String[] accountIdKeySplit = accountPerCardId.split("#");
            if ( accountIdKeySplit[0].equals(cartType)) {
                String[] accountIdString = accountIdKeySplit[1].split(",");
                accountID.setBank(accountIdString[0]);
                accountID.setBranch(accountIdString[1]);
                accountID.setAccount(accountIdString[2]);
                foundIt = true;
                break;
            }
        }

        if ( !foundIt) {
            throw new RuntimeException("Cannot find signature for [" + cartType + "] in [" + accountSignature + "]");
        }

        return accountID;
    }

    protected static boolean areEqualByThreeDecimalPlaces(double a, double b)
    {
        DecimalFormat df = new DecimalFormat("####.##;-####.##");
        df.setRoundingMode(RoundingMode.UP);
        String as  = df.format(a);
        String bs  = df.format(b);
//        LOG.info("[{}].equals[{}]={} <while input value are {} and {}>",as,bs,as.equals(bs),a,b);
        return as.equals(bs);
    }
}
