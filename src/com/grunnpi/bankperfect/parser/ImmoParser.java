package com.grunnpi.bankperfect.parser;

import com.grunnpi.bankperfect.data.BankFile;
import com.grunnpi.bankperfect.data.Statement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ImmoParser extends AbstractParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(ImmoParser.class);

    public static String upperCaseFirst(String value)
    {
        if (!StringUtils.isEmpty(value))
        {
            // Convert String to char array.
            char[] array = value.toCharArray();
            // Modify first element in array.
            array[0] = Character.toUpperCase(array[0]);
            // Return string.
            return new String(array);
        }
        else
            return "";
    }

    public List<Statement> prepare(BankFile bankFile, List<String> lines, Map<String, String> mapping,
            final String accountSignature)
    {
        List<Statement> listStatement = new ArrayList<Statement>();
        {
            int nbLine = 0;

            String theBien = "";
            String theBail = "";
            String theTypeLot = "";
            String theOperation = "";
            boolean theSignePositive = true;
            String theDescription = "";
            boolean isRealLine = false;
            String theDateTransaction = "";
            String theDateTransactionLast = "";
            String theDateTransactionLastFormatted = "";
            String theMontant = "";

            String theDecompte = "";
            String theDateTraitement = "";

            String theSolde = "";

            boolean isEcritureLot = false;
            boolean isStuffBail = false;

            String theOwner = "";
            boolean immoOwnerFound = false;
            for (String line : lines)
            {
                nbLine++;
                isRealLine = false;
                String fullLine = line;
                LOG.debug("#{} [{}]", nbLine, line);
                String theAdditionalComment = "";

                if (!immoOwnerFound)
                {
                    // 1er ligne, num décompte + date
                    if (line.contains("SCI JEMASARAEL") && (line.contains("Relevé") || line.contains("Principal")))
                    {
                        line = StringUtils.substringAfterLast(line, "Décompte n°");
                        line = line.trim();
                        theOwner = "SCI JEMASARAEL";
                        immoOwnerFound = true;
                    }
                    else if ((line.contains("Mr & Mme GRUNNAGEL") || line.contains("Mr & Mme   GRUNNAGEL")) && line
                            .contains("Relevé"))
                    {
                        line = StringUtils.substringAfterLast(line, "n°");
                        line = line.trim();
                        theOwner = "Mr & Mme GRUNNAGEL";
                        immoOwnerFound = true;
                    }

                    if (immoOwnerFound)
                    {
                        String[] lineSplit = line.split("du ");
                        if (lineSplit.length > 1)
                        {
                            theDecompte = lineSplit[0].trim();
                            theDateTraitement = lineSplit[1];

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            LocalDate statementDate = LocalDate.parse(theDateTraitement, formatter);

                            DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);
                            theDateTransactionLast = statementDate.format(formatterDate);
                            LOG.info("Owner[{}]Number[{}]-Date[{}]({})", theOwner, theDecompte, theDateTraitement,
                                    theDateTransactionLast);
                        }
                        else
                        {
                            // skip this line
                            LOG.info(">> skip this line [{}]", line);
                        }
                    }
                }
                else if (StringUtils.isEmpty(line.trim()))
                {
                    // empty line
                }
                else if (line.contains("Sous-totaux"))
                {
                    isEcritureLot = false;
                    isStuffBail = false;
                }
                else if (line.startsWith("Bail"))
                {
                    theBail = line.replace("Bail ", "");
                    isStuffBail = true;
                }
                else if (line.startsWith("Immeuble 36 rue Beethoven"))
                {
                    theBien = "Beethoven";
                    isStuffBail = false;
                    LOG.info("Bien[{}]", theBien);
                }
                else if (line.startsWith("Maison 6, rue Bellevue"))
                {
                    theBien = "Bellevue";
                    isStuffBail = false;
                    LOG.info("Bien[{}]", theBien);
                }
                else if (line.startsWith("Ecritures de l'immeuble"))
                {
                    if (theBien.equals("Bellevue"))
                    {
                        theBail = "Gestion Maison";
                        theTypeLot = "Maison";
                    }
                    else
                    {
                        theBail = "Gestion Immeuble";
                        theTypeLot = "Immeuble";
                    }

                    theSignePositive = false;
                    isStuffBail = false;
                }
                else if (line.startsWith("Lot 36 rue Beethoven, "))
                {
                    theTypeLot = line.replace("Lot 36 rue Beethoven, ", "");

                    if (theTypeLot.contains("Appartement Type 3"))
                    {
                        theTypeLot = "Appartement";
                    }
                    theSignePositive = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("Lot 6, rue Bellevue, Maison Type 7 et +"))
                {
                    theBail = "Gestion Maison";
                    theTypeLot = "Maison";
                    theSignePositive = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("Ecritures du lot"))
                {
                    theSignePositive = false;
                    isEcritureLot = true;
                    isStuffBail = false;
                }
                else if (line.startsWith("ORPI DOENST IMMO"))
                {
                    isEcritureLot = false;
                    isStuffBail = false;
                }
                else if (line.contains("Solde en votre faveur au "))
                {
                    theSolde = line.replace("Solde en votre faveur au ", "@");
                    theSolde = StringUtils.substringAfter(theSolde, " ");
                    LOG.info("Solde[{}] fetch from line [{}]", theSolde, fullLine);
                    isStuffBail = false;
                }
                else if (line.contains("Solde nul au "))
                {
                    theSolde = line.replace("Solde nul au ", "@");
                    theSolde = StringUtils.substringAfter(theSolde, " ");
                    LOG.info("Solde[{}] fetch from line [{}]", theSolde, fullLine);
                    isStuffBail = false;
                }
                else if (line.startsWith("Loyer ") || line.startsWith("Provision charges courantes ") || line
                        .startsWith("Garantie loyers impayés (GLI)") || line.startsWith("Honoraires de gestion ")
                        || line.startsWith("Assurance loyers impayés") || line.startsWith("Garantie des loyers")
                        || isStuffBail)
                {
                    boolean oneTimeNegativeSign = false;
                    boolean oneTimePositiveSign = false;
                    if (line.startsWith("Provision charges courantes "))
                    {
                        theOperation = "Charge";
                        line = line.replace("Provision charges courantes ", "");
                    }
                    else if (line.startsWith("Loyer "))
                    {
                        theOperation = "Loyer";
                        line = line.replace("Loyer ", "");
                    }
                    else if (line.startsWith("Garantie loyers impayés (GLI)"))
                    {
                        theOperation = "Assurance";
                        line = line.replace("Garantie loyers impayés (GLI) ", "");
                        line = StringUtils.substringAfter(line, ") ");
                        line = theDateTransactionLast + " 5,84 " + line;
                        theAdditionalComment = " (garantie des loyers impayés (GLI))";
                    }
                    else if (line.startsWith("Assurance loyers impayés"))
                    {
                        theOperation = "Assurance";
                        line = line.replace("Assurance loyers impayés ", theDateTransactionLast + " 5,84 ");
                        theAdditionalComment = " (assurance loyer impayés)";
                    }
                    else if (line.startsWith("Garantie des loyers"))
                    {
                        theOperation = "Assurance";
                        line = line.replace("Garantie des loyers ", theDateTransactionLast + " 5,84 ");
                        theAdditionalComment = " (garantie des loyers)";
                    }
                    else if (line.startsWith("Honoraires de gestion 5,84% "))
                    {
                        theOperation = "Frais";
                        line = theDateTransactionLast + " 5,84 " + StringUtils.substringAfterLast(line, " ");
                    }
                    else if (line.startsWith("Honoraires de gestion 7,00% "))
                    {
                        theOperation = "Frais";
                        line = theDateTransactionLast + " 5,84 " + StringUtils.substringAfterLast(line, " ");
                    }
                    else
                    {
                        theOperation = "Loyer";
                        line = line.replace("Loyer ", "");
                    }

                    // Split on % commission/tva applied
                    if (line.contains(" 5,84"))
                    {
                        line = line.replace(" 5,84", "@");
                    }
                    else if (line.contains(" 7,00"))
                    {
                        line = line.replace(" 7,00", "@");
                    }
                    else
                    {
                        LOG.error("line [{}] without <5,84 or 7,00> as separator.", line);
                    }

                    theDateTransaction = line;
                    theDateTransaction = theDateTransaction.replace(" (solde)", "");
                    theDateTransaction = theDateTransaction.replace(" (partiel)", "");
                    theDateTransaction = theDateTransaction.replace(" (reliquat)", "");
                    // theDateTransaction = theDateTransaction.toLowerCase();

                    //String[] dateSplit = theDateTransaction.split(" ");
                    String dateSplited = StringUtils.substringBeforeLast(theDateTransaction, "@");
                    String afterDateSplited = StringUtils.substringAfterLast(theDateTransaction, "@").trim();

                    //                    LOG.info("Split [{}] > [{}]//[{}]",theDateTransaction,dateSplited,afterDateSplited);

                    theDateTransactionLast = dateSplited;
                    if (StringUtils.isEmpty(dateSplited))
                    {
                        LOG.warn("No date here [{}] > [{}]", line, dateSplited);
                    }
                    try
                    {
                        String[] dateSplit = dateSplited.split(" ");

                        String mois = dateSplit[0];
                        if (mois.equals("AOUT"))
                        {
                            mois = "Août";
                        }
                        else if (mois.equals("DECEMBRE"))
                        {
                            mois = "Décembre";
                        }
                        Date date = new SimpleDateFormat("MMMM", Locale.FRANCE).parse(mois);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        String annee = dateSplit[1];
                        if (annee.length() == 2)
                        {
                            annee = "20" + annee;
                        }

                        theDateTransaction = String.format("01/%02d/%s", cal.get(Calendar.MONTH) + 1, annee);

                        theDateTransactionLastFormatted = theDateTransaction;
                    }
                    catch (Exception e)
                    {
                        LOG.error("Creating date[{}]>[{}]", theDateTransaction, dateSplited, e);
                        //System.err.println("Date = " + theDateTransaction);
                    }

                    // TODO: faut virer le nom du bail dans la description
                    theDescription = dateSplited;
                    theDescription = theDescription.replace(" MME GASPARINI CECIL", "");
                    theDescription = theDescription.replace(" MLLE MICHELE NGATOU", "");
                    theDescription = theDescription.replace(" MME JEANNINE FRIGOL", "");

                    if (oneTimeNegativeSign)
                    {
                        theMontant = "-" + afterDateSplited;
                    }
                    else if (oneTimePositiveSign)
                    {
                        theMontant = afterDateSplited;
                    }
                    else
                    {
                        theMontant = theSignePositive ? afterDateSplited : "-" + afterDateSplited;
                    }

                    isRealLine = true;
                    LOG.info("realLine.loyerOuAutre [{}] [{}]", fullLine, theMontant);
                }
                else if (isEcritureLot)
                {
                    // Ligne de travaux
                    theDescription = StringUtils.substringBeforeLast(line, " ");

                    theMontant = StringUtils.substringAfterLast(line, " ");
                    theMontant = theSignePositive ? theMontant : "-" + theMontant;

                    isRealLine = true;
                    LOG.info("realLine.ecritureLot [{}] [{}]", fullLine, theMontant);

                    if (theDescription.contains("CONTRAT ENTRETIEN"))
                    {
                        theOperation = "Entretien";
                    }
                    else
                    {
                        theOperation = "Travaux";
                    }

                    theDateTransaction = theDateTransactionLastFormatted;
                }

                if (isRealLine)
                {
                    String description =
                            theBien + "/Decompte_" + theDecompte + "/" + upperCaseFirst(theDescription.toLowerCase())
                                    + "/" + theTypeLot + "/" + theOperation + theAdditionalComment;

                    Statement statement = new Statement();

                    statement.setTier(theBail);
                    statement.setDescription(description);
                    Double amount = null;
                    try
                    {
                        amount = Double.parseDouble(theMontant.replace(",", ".").replace(" ", ""));
                        //                        LOG.info("Amount s[{}] > d[{}]",theMontant,amount);
                    }
                    catch (Exception e)
                    {
                        LOG.error("Amout cast[{}] error for line [{}]", theMontant, fullLine, e);
                    }
                    statement.setAmount(amount);

                    // DD/MM/YYYY > YYYY-MM-DD
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate statementDate = LocalDate.parse(theDateTraitement, formatter);

                    statement.setStatementDate(statementDate);
                    listStatement.add(statement);
                }
            }

            double amountTotalComputed = 0.0;
            for (Statement statement : listStatement)
            {
                // sum
                if (statement.getAmount() != null)
                {
                    LOG.debug("++amount[{}] for [{}]", statement.getAmount(), statement.getDescription());
                    amountTotalComputed += statement.getAmount();
                }
                else
                {
                    LOG.error("Amount null for [{}]", statement.getDescription());
                }

                // add account
                statement.setAccountID(getAccount(accountSignature, theBien));
            }

            theSolde = theSolde.replace(" ", "").replace(",", ".");
            if (StringUtils.isEmpty(theSolde))
            {
                LOG.error("No solde found ! Computed[{}]", amountTotalComputed);
            }
            else
            {
                theSolde = theSolde.replace(" ", "").replace(",", ".");
                Double amountTotal = Double.parseDouble(theSolde);
                DecimalFormat df = new DecimalFormat("#.00");
                if (areEqualByThreeDecimalPlaces(amountTotal, amountTotalComputed))
                {
                    LOG.info("Total [{}]==[{}] is OK [{}]", df.format(amountTotal), df.format(amountTotalComputed),
                            theOwner);
                }
                else
                {
                    LOG.error("Total [{}]<>[{}] is NOT OK [{}]", df.format(amountTotal), df.format(amountTotalComputed),
                            theOwner);
                }
            }
        }
        return listStatement;
    }
}
