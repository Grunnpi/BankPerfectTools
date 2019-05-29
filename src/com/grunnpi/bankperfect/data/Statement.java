package com.grunnpi.bankperfect.data;

import java.time.LocalDate;

public class Statement
{

    private String tier;
    private String statementDateVariable;
    private String bank;
    private String branch;
    private String account;
    private Double amount;
    private String description;
    private boolean valid = false;
    private LocalDate statementDate;
    private String rawLine;

    public String getBPKey()
    {
        return bank + "#" + branch + "#" + account;
    }

    public void setAccountID(final AccountID accountID)
    {
        setBank(accountID.getBank());
        setBranch(accountID.getBranch());
        setAccount(accountID.getAccount());
    }

    public String getTier()
    {
        return tier;
    }

    public void setTier(String tier)
    {
        this.tier = tier;
    }

    public String getStatementDateVariable()
    {
        return statementDateVariable;
    }

    public void setStatementDateVariable(String statementDateVariable)
    {
        this.statementDateVariable = statementDateVariable;
    }

    public String getBank()
    {
        return bank;
    }

    public void setBank(String bank)
    {
        this.bank = bank;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public Double getAmount()
    {
        return amount;
    }

    public void setAmount(Double amount)
    {
        this.amount = amount;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid)
    {
        this.valid = valid;
    }

    @Override
    public String toString()
    {
        return "Statement{" + "amount=" + amount + ", description='" + description + '\'' + ", valid=" + valid
                + ", statementDate=" + statementDate + ", rawLine='" + rawLine + '\'' + '}';
    }

    public LocalDate getStatementDate()
    {
        return statementDate;
    }

    public void setStatementDate(LocalDate statementDate)
    {
        this.statementDate = statementDate;
    }

    public String getRawLine()
    {
        return rawLine;
    }

    public void setRawLine(String rawLine)
    {
        this.rawLine = rawLine;
    }
}
