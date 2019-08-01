package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalaryParser extends AbstractParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(SalaryParser.class);

    private static final String REPORT_DEJA_EFFECTUES = "Reports déjà effectués";
    private static final String DECOMPTE_REMUNERATION_START = "Décompte de rémunération de ";
    private static final String PRESTATION_DESIGNATION_START = "Prestation D";
    private static final String TOTAL_NET_A_VIRER = "Total net à virer";
    private static final String CALCULE_EN = "Calculé en ";

    private static boolean addAllPayroll = false;

    public List<Statement> prepare(BankFile bankFile, List<String> lines, Map<String, String> mapping,
            final String accountSignature)
    {
        LOG.info("Prepare[{}] lines", lines.size());
        int nbLine = 0;

        String addComment = "";
        String theCalculationDate = "";
        String totalPayrollExpected = "0";

        List<List<Statement>> allPayroll = new ArrayList<List<Statement>>();
        List<Statement> currentPayroll = null;

        // loop on all lines, but may have several payroll...
        for (String line : lines)
        {
            nbLine++;
            String fullLine = line;
            LOG.debug("#{} [{}]", nbLine, fullLine);
            if (line.startsWith(DECOMPTE_REMUNERATION_START))
            {
                addComment = line;
                final String statementRawDate = addComment.replace(DECOMPTE_REMUNERATION_START, "").substring(0, 7);
                final String statementYear = statementRawDate.substring(statementRawDate.length() - 4);
                final String statementMonth = statementRawDate.substring(0, 2);
                LocalDate localDate = LocalDate.of(Integer.valueOf(statementYear), Integer.valueOf(statementMonth), 14);

                // now date is defined, let's set it for all statement in current payroll
                for (Statement stmt : currentPayroll)
                {
                    stmt.setStatementDate(localDate);
                }
            }
            else if (line.startsWith(CALCULE_EN))
            {
                line = line.replace(CALCULE_EN, "");
                line = StringUtils.substringBefore(line, " ");
                theCalculationDate = line;

                // checksum total
                double amountTotalComputed = 0.0;
                for (Statement stmt : currentPayroll)
                {

                    // last chance to put an amount
                    if (stmt.getAmount() == null)
                    {
                        String desc = stmt.getRawLine();
                        String couldBeAmount = StringUtils.substringAfterLast(desc.trim(), " ");
                        couldBeAmount = couldBeAmount.replace(",", "");
                        double amountRaw = Double.parseDouble(couldBeAmount);
                        LOG.warn("Last change amount[{}]", amountRaw, desc);
                        stmt.setAmount(amountRaw);
                    }

                    // sum
                    if (stmt.getAmount() != null)
                    {
                        //                        LOG.info("++amount[{}] for [{}]",stmt.getAmount(),stmt.getDescription());
                        amountTotalComputed += stmt.getAmount();
                    }
                    else
                    {
                        LOG.error("Amount null for [{}]", stmt.getDescription());
                    }
                }

                Double amountTotal = Double.parseDouble(totalPayrollExpected);
                DecimalFormat df = new DecimalFormat("#.00");
                if (areEqualByThreeDecimalPlaces(amountTotal, amountTotalComputed))
                {
                    LOG.info("Total [{}]==[{}] is OK [{}][{}]", df.format(amountTotal), df.format(amountTotalComputed),
                            currentPayroll.get(0).getStatementDate(), theCalculationDate);
                }
                else
                {
                    LOG.error("Total [{}]<>[{}] is NOT OK [{}][{}]", df.format(amountTotal),
                            df.format(amountTotalComputed), currentPayroll.get(0).getStatementDate(),
                            theCalculationDate);
                }

                // and then push it to payroll list
                allPayroll.add(currentPayroll);
                currentPayroll = null;
            }
            else if (line.startsWith(PRESTATION_DESIGNATION_START))
            {
                // new payroll detected
                currentPayroll = new ArrayList<Statement>();
            }
            else if (line.startsWith(TOTAL_NET_A_VIRER))
            {
                // new payroll detected
                totalPayrollExpected = line.replace(TOTAL_NET_A_VIRER, "").trim().replace(",", "");
            }
            else
            {
                // prepare a new statement
                Statement newStatement = new Statement();

                newStatement.setRawLine(line);
                newStatement.setValid(true);

                boolean montantSiNegatifAMettrePositif = false;
                if (line.contains(REPORT_DEJA_EFFECTUES))
                {
                    line = line.replace("( ", "").replace(" )", "");
                    montantSiNegatifAMettrePositif = true;
                }

                // virer les "(-)" en fin de ligne pour A reporter
                if (line.contains(" (-)"))
                {
                    line = line.replace(" (-)", "");
                    montantSiNegatifAMettrePositif = true;
                }

                String description = line;
                String montant = "0";
                int firstSpace = line.indexOf(" ");
                int lastSpace = line.lastIndexOf(" ");
                if (lastSpace > 0)
                {
                    montant = line.substring(lastSpace).trim();
                }

                if (StringUtils.isNumeric("" + line.charAt(0)))
                {
                    if ((firstSpace > 0) && (lastSpace > 0))
                    {
                        if (firstSpace != lastSpace)
                        {
                            description = line.substring(firstSpace + 1, lastSpace - 1);
                        }
                        else
                        {
                            description = line.substring(0, firstSpace);
                        }

                        int otherLast = description.lastIndexOf(" ");
                        if (otherLast > 0)
                        {
                            description = description.substring(0, otherLast);
                        }
                        otherLast = description.lastIndexOf(" ");
                        if (otherLast > 0)
                        {
                            description = description.substring(0, otherLast);
                        }
                    }
                }
                else
                {
                    if (lastSpace < 0)
                    {
                        // skip this line !
                    }
                    else
                    {
                        try
                        {
                            description = line.substring(0, lastSpace);
                            description = description.trim();
                            description = description.replaceAll(" +", " ");
                        }
                        catch (Exception e)
                        {
                            LOG.error("Substring for lastSpace [{}][{}]", line, lastSpace);
                        }

                        String[] array = { "Maladie Soins 2.80 %", "Maladie Soins NP 2.80 %", "Maladie Espèces 0.25 %",
                                "Caisse de Pension 8.00 %", "Caisse de Pension NP 8.00 %",
                                "Assurance dépendance 1.40 %", "Assurance dépendance NP 1.40 %",
                                "Impôt d'équi budg temp 0.50 %", "Impôt d'équi budg temp NP 0.50 %" };

                        boolean specialDebit = false;
                        for (String check : array)
                        {
                            if (description.contains(check))
                            {
                                specialDebit = true;
                                break;
                            }
                        }

                        if (specialDebit)
                        {
                            int otherLast = description.lastIndexOf(" ");
                            if (otherLast > 0)
                            {
                                description = description.substring(0, otherLast);
                            }
                            montant = "-" + montant;
                        }
                        else
                        {
                            if (description.equals("Impôt") || description.equals("Impôt NP"))
                            {
                                montant = "-" + montant;
                            }
                            else if (description.equals("CIS - CIM - CIP"))
                            {
                                if (montant.charAt(0) == '-')
                                {
                                    montant = montant.substring(1);
                                }
                                else
                                {
                                    montant = "-" + montant;
                                }
                            }
                        }
                    }
                }

                // amount computed (hopefully)
                montant = montant.replace(",", "");
                try
                {
                    double amountComputed = Double.parseDouble(montant);
                    if ( fullLine.equals(description)) {
                        LOG.debug("skip : desc==line [{}]", fullLine);
                        newStatement.setValid(false);
                    }
                    else {
                        LOG.debug("**Amount[{}]/[{}] for line [{}]/[{}]",amountComputed,montant,fullLine,description);
                        newStatement.setAmount(amountComputed);
                    }
                }
                catch (Exception e)
                {
                    LOG.debug("skip : no amount there [{}]", fullLine);
                    newStatement.setValid(false);
                }

                // Mapping desc
                for (Map.Entry<String, String> entry : mapping.entrySet())
                {
                    if (description.startsWith(entry.getKey()))
                    {
                        description = entry.getValue();
                        break;
                    }
                }
                newStatement.setDescription(description);

                // end of statement fetching
                if (currentPayroll != null && newStatement.isValid())
                {
                    currentPayroll.add(newStatement);
                }
            }
        }

        List<Statement> statements = new ArrayList<Statement>();
        if (addAllPayroll)
        {
            for (List<Statement> payroll : allPayroll)
            {
                LOG.info("Add payroll[{}] (all)", payroll.get(0).getStatementDate());
                statements.addAll(payroll);
            }
        }
        else
        {
            List<Statement> payroll = allPayroll.get(allPayroll.size() - 1);
            LOG.info("Add payroll[{}] (only last)", payroll.get(0).getStatementDate());
            statements.addAll(payroll);
        }

        String[] accountId = accountSignature.split(",");
        for (Statement statement : statements)
        {
            statement.setBank(accountId[0]);
            statement.setBranch(accountId[1]);
            statement.setAccount(accountId[2]);
        }

        bankFile.setToMoveToArchive(true);
        bankFile.setTargetName(getArchiveDir() + "/" + bankFile.getFile().getName());
        return statements;
    }
}
