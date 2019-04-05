package com.grunnpi.bankperfect;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalaryParser implements IStatementPreparator
{
    private static final Logger LOG = LoggerFactory.getLogger(SalaryParser.class);

    private static final String REPORT_DEJA_EFFECTUES = "Reports déjà effectués";
    private static final String DECOMPTE_REMUNERATION_START = "Décompte de rémunération de ";
    private static final String PRESTATION_DESIGNATION_START = "Prestation D";
    private final String path = "D:/Documents/[Mes Documents]/Gestion/Salaires Pierre/";
    private List<RBCLineBean> listRBC;

    public List<Statement> prepare(List<String> lines,Map<String,String> mapping)
    {
        LOG.info("Prepare[{}] lines",lines.size());
        int nbLine = 0;

        String addComment = "";

        List<List<Statement>> allPayroll = new ArrayList<List<Statement>>();
        List<Statement> currentPayroll = null;

        // loop on all lines, but may have several payroll...
        for (String line : lines)
        {
            nbLine++;
            if (line.startsWith(DECOMPTE_REMUNERATION_START))
            {
                addComment = line;
                final String statementRawDate = addComment.replace(DECOMPTE_REMUNERATION_START,"").substring(0,7);
                final String statementYear = statementRawDate.substring(statementRawDate.length() - 4);
                final String statementMonth = statementRawDate.substring(0, 2);
                LocalDate localDate = LocalDate.of(Integer.valueOf(statementYear), Integer.valueOf(statementMonth), 14);

                // now date is defined, let's set it for all statement in current payroll
                for ( Statement stmt : currentPayroll ) {
                    stmt.setStatementDate(localDate);
                }

                // and then push it to payroll list
                allPayroll.add(currentPayroll);
                currentPayroll = null;
            }
            else if ( line.startsWith(PRESTATION_DESIGNATION_START)) {
                // new payroll detected
                currentPayroll = new ArrayList<Statement>();
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
                    if ( lastSpace < 0 ) {
                        // skip this line !
                    }
                    else {
                        try {
                            description = line.substring(0, lastSpace);
                        }catch(Exception e) {
                            LOG.error("Substring for lastSpace [{}][{}]",line,lastSpace);
                        }

                        String[] array = { "Maladie Soins 2.80 %", "Maladie Soins NP 2.80 %", "Maladie Espèces 0.25 %",
                                "Caisse de Pension 8.00 %", "Caisse de Pension NP 8.00 %", "Assurance dépendance 1.40 %",
                                "Assurance dépendance NP 1.40 %", "Impôt d'équi budg temp 0.50 %",
                                "Impôt d'équi budg temp NP 0.50 %" };

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



                // Mapping desc
                for ( Map.Entry<String,String> entry : mapping.entrySet() ) {
                    if ( description.startsWith(entry.getKey()) ) {
                        description = entry.getValue();
                        break;
                    }
                }
                newStatement.setDescription(description);

                // end of statement fetching
                if ( currentPayroll != null && newStatement.isValid() ) {
                    currentPayroll.add(newStatement);
                }
            }
        }

        List<Statement> statements = new ArrayList<Statement>();
        for ( List<Statement> payroll : allPayroll ) {
            LOG.info("Payroll++");
            statements.addAll(payroll);
        }

        for ( Statement statement : statements) {
            String desc = statement.getRawLine();
            String couldBeAmount = StringUtils.substringAfterLast(desc.trim()," ");
            couldBeAmount = couldBeAmount.replace(",","");
            double amount = Double.parseDouble(couldBeAmount);
            statement.setAmount(amount);
        }

        return statements;
    }

    public void readFile(String filename, CharSequence USELES)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
        {
            int nbLine = 0;
            int nbLineOk = 0;
            int nbLineNok = 0;
            String addComment = "";
            String salaireAnnee = "";
            String salaireMois = "";
            String salaireJour = "14";
            String salaireDate = "";
            listRBC = new ArrayList<RBCLineBean>();
            RBCLineBean lineBean = null;
            for (String line; (line = br.readLine()) != null; )
            {
                nbLine++;
                // System.out.println(line);

                if (nbLine == 1)
                {
                    // 1er ligne, c'est le libellé commun
                    addComment = line;

                    String releveDate = addComment.substring(addComment.length() - 7);

                    salaireAnnee = releveDate.substring(releveDate.length() - 4);
                    salaireMois = releveDate.substring(0, 2);

                    salaireDate = salaireJour + "/" + salaireMois + "/" + salaireAnnee;
                    System.out.println("Date=" + salaireDate);
                }
                else if (nbLine == 2)
                {
                    // 2eme ligne, en tête colonne
                }
                else
                {

                    // ligne salaire bloc1
                    if (lineBean != null)
                    {
                        listRBC.add(lineBean);
                    }
                    lineBean = new RBCLineBean();
                    lineBean.setDateOperation(salaireDate);

                    boolean montantSiNegatifAMettrePositif = false;
                    if (line.contains(REPORT_DEJA_EFFECTUES))
                    {
                        line = line.replace("( ", "").replace(" )", "");
                        montantSiNegatifAMettrePositif = true;
                    }

                    // virer les "(-)" en fin de ligne pour A reporter
                    if (line.contains(" (-)"))
                    {
                        System.out.println("oooh ?");
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
                        // System.out.println("bloc1 [" + line + "]");
                        if ((firstSpace > 0) && (lastSpace > 0))
                        {
                            // System.out.println("firstSpace:" + firstSpace + ",lastSpace:" + lastSpace);
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
                        // System.out.println("blocN [" + line + "]");
                        description = line.substring(0, lastSpace);

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
                                    // System.out.println("line " + line + " / montant=[" + montant + "]");
                                    // nbLineNok++;
                                }
                            }
                        }

                    }

                    // System.out.println(">desc:[" + description + "]");
                    // if (montant.charAt(0) == '-')
                    // {
                    // System.err.println(">montant:[" + montant + "]");
                    // }
                    // else
                    // {
                    // System.out.println(">montant:[" + montant + "]");
                    // }
                    montant = '"' + montant.replace(",", "") + '"';

                    String[] arrayForInformation = { "Imposable", "Imposable NP", "Net", "Total net à virer",
                            "Brut total", "Cotisations totales", "Abattement", "Heures supplémentaires",
                            "Suppléments DNF", "Frais de déplacement (FD)", "Autres exemptions", "Total net",
                            "Avantage prêt immobilier", "Exemption prêt immobilier" };

                    boolean forInformation = false;
                    for (String check : arrayForInformation)
                    {
                        if (description.equals(check))
                        {
                            forInformation = true;
                            break;
                        }
                    }

                    lineBean.setForInformation(forInformation);
                    lineBean.setDescription("RBC/" + description);

                    if (montantSiNegatifAMettrePositif)
                    {
                        if (montant.contains("-"))
                        {
                            montant = montant.replace("-", "");
                        }
                        else
                        {
                            montant = montant.replaceFirst("\"", "\"-");
                        }
                    }
                    lineBean.setMontant(montant);
                }

            }
            if (lineBean != null)
            {
                listRBC.add(lineBean);
            }

            System.out.println("Nb Ligne RBC = " + listRBC.size());
            for (RBCLineBean cbBean : listRBC)
            {
                String description = cbBean.getDescription();
                if (cbBean.isForInformation())
                {

                }
                else
                {
                    System.out.println(cbBean);
                }
                // nbLineNok++;
            }

            if (nbLineNok == 0)
            {

                File fout = new File(path + "Salaires-" + salaireAnnee + "/" + salaireAnnee + "-" + salaireMois
                        + "-Fiche-de-salaire-RBC.csv");

                System.out.println(fout.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(fout);

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "Cp1252"));

                // Float total = Float.valueOf(0);
                double total = 0.0;

                bw.write("Mode;Date;Details;Montant");
                bw.newLine();

                for (RBCLineBean cbBean : listRBC)
                {

                    if (!cbBean.isForInformation())
                    {
                        String stringMontant = cbBean.getMontant().replace("\"", "");

                        // Float montant = Float.parseFloat(stringMontant);
                        // DecimalFormat df = new DecimalFormat("0.00");
                        // df.setMaximumFractionDigits(2);

                        double montant = Double.parseDouble(stringMontant);
                        total += montant;

                        cbBean.setMontant(cbBean.getMontant().replace(".", ","));
                        bw.write(cbBean.toString());
                        bw.newLine();
                    }
                }
                bw.close();
                System.out.println("Total = " + new DecimalFormat("#.##").format(total));
            }
            else
            {
                System.err.println("NbErr = " + nbLineNok);
            }
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return the listRBC
     */
    public List<RBCLineBean> getListRBC()
    {
        return listRBC;
    }

    /**
     * @param listRBC the listRBC to set
     */
    public void setListRBC(List<RBCLineBean> listRBC)
    {
        this.listRBC = listRBC;
    }
}
