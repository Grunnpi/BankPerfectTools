package com.grunnpi.bankperfect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

public class RBCImportSalaireOld
{

    private static final String REPORT_DEJA_EFFECTUES = "Reports déjà effectués";
    // private static final CharSequence SEPARATEUR_MILLIER_MONTANT = ".";

    private List<RBCLineBean> listRBC;
    private final String path = "D:/Documents/[Mes Documents]/Gestion/Salaires Pierre/";

    public void readFile(String filename, CharSequence SEPARATEUR_MILLIER_MONTANT)
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
            for (String line; (line = br.readLine()) != null;)
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
                        if (montant.equals("-"))
                        {
                            line = StringUtils.substring(line, 0, lastSpace);
                            lastSpace = line.lastIndexOf(" ");
                            if (lastSpace > 0)
                            {
                                montant = line.substring(lastSpace).trim();
                            }
                        }
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

                        String[] array =
                        { "Maladie Soins 2.80 %", "Maladie Soins NP 2.80 %", "Maladie Espèces 0.25 %",
                                "Caisse de Pension 8.00 %", "Caisse de Pension NP 8.00 %",
                                "Assurance dépendance 1.40 %", "Assurance dépendance NP 1.40 %",
                                "Impôt d'équi budg temp 0.50 %", "Impôt d'équi budg temp NP 0.50 %",
                                "Assurance maladie soins", "Assurance maladie espèce", "Assurance pension" };

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

                    montant = '"' + montant.replace(SEPARATEUR_MILLIER_MONTANT, "") + '"';

                    String[] arrayForInformation =
                    { "Net", "Total net à virer", "Brut total", "Cotisations totales", "Abattement",
                            "Heures supplémentaires", "Suppléments DNF", "Frais de déplacement (FD)",
                            "Autres exemptions", "Total net", "Avantage prêt immobilier", "Exemption prêt immobilier",
                            "Brut EUR", "Déductions légales Bases", "Eléments nets", "Modération d’impôts",
                            "Semi-net EUR", "Exemptions heures suppl.", "Avantage prêt immobilier" };

                    boolean forInformation = false;
                    for (String check : arrayForInformation)
                    {
                        if (description.startsWith(check))
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

            Map<String, String> mapLabelToReplace = new HashMap<String, String>();

            mapLabelToReplace.put("RBC/Salaire mensuel", "RBC/Salaire Mensuel Employés");
            mapLabelToReplace.put("RBC/Prime d’ancienneté B", "RBC/Prime Ancienneté B");
            mapLabelToReplace.put("RBC/PDD Interventions", "RBC/EUR Intervention");
            mapLabelToReplace.put("RBC/Assurance maladie soins", "RBC/Maladie Soins 2.80 %");
            mapLabelToReplace.put("RBC/Assurance maladie espèce", "RBC/Maladie Espèces 0.25 %");
            mapLabelToReplace.put("RBC/Assurance pension", "RBC/Caisse de Pension 8.00 %");
            mapLabelToReplace.put("RBC/Imposable NP", "RBC/Impôt NP");
            mapLabelToReplace.put("RBC/Imposable", "RBC/Impôt");

            mapLabelToReplace.put("RBC/Advantage Plan - EN", "RBC/RBC Intl Savings Plan");
            mapLabelToReplace.put("RBC/Treizième mois", "RBC/13ième mois");

            mapLabelToReplace.put("RBC/Cotisable ass. dépendance", "RBC/Assurance dépendance 1.40 %");
            mapLabelToReplace.put("RBC/Crédit Impôts Salarié", "RBC/CIS - CIM - CIP");
            mapLabelToReplace.put("RBC/Chèques repas EUR", "RBC/Chèques repas");
            mapLabelToReplace.put("RBC/Parking EUR", "RBC/Parking");
            mapLabelToReplace.put("RBC/Régime compl. de pension EUR", "RBC/Régime compl.de pension");

            mapLabelToReplace.put("RBC/A reporter EUR", "RBC/A Reporter");
            mapLabelToReplace.put("RBC/Report mois antérieur", "RBC/Recalcul");

            mapLabelToReplace.put("RBC/Prest.Normal 100%", "RBC/Paiement Heures 100%");
            mapLabelToReplace.put("RBC/Prest.HS à 50%", "RBC/Paiement HS +50%");
            mapLabelToReplace.put("RBC/Heures complémentaires", "RBC/Paiement Heures compl.");
            mapLabelToReplace.put("RBC/Cotisation CSL", "RBC/Cotisation CSL");
            mapLabelToReplace.put("RBC/Assurance maladie NP", "RBC/Maladie Soins NP 2.80 %");
            mapLabelToReplace.put("RBC/Cotisable ass. dépend. NP", "RBC/Assurance dépendance NP 1.40 %");

            mapLabelToReplace.put("RBC/Bonus", "RBC/STI");

            List<String> finalLabelShouldBeNegative = new ArrayList<String>();

            finalLabelShouldBeNegative.add("RBC/Maladie Soins 2.80 %");
            finalLabelShouldBeNegative.add("RBC/Maladie Soins NP 2.80 %");

            finalLabelShouldBeNegative.add("RBC/Maladie Espèces 0.25 %");
            finalLabelShouldBeNegative.add("RBC/Caisse de Pension 8.00 %");
            finalLabelShouldBeNegative.add("RBC/Assurance dépendance 1.40 %");
            finalLabelShouldBeNegative.add("RBC/Assurance dépendance NP 1.40 %");

            finalLabelShouldBeNegative.add("RBC/Impôt");
            finalLabelShouldBeNegative.add("RBC/Cotisation CSL");

            finalLabelShouldBeNegative.add("RBC/Parking");
            finalLabelShouldBeNegative.add("RBC/Régime compl.de pension");
            finalLabelShouldBeNegative.add("RBC/Chèques repas");

            List<String> notInSemiNet = new ArrayList<String>();
            notInSemiNet.add("RBC/Parking");
            notInSemiNet.add("RBC/Régime compl.de pension");
            notInSemiNet.add("RBC/Chèques repas");
            notInSemiNet.add("RBC/A Reporter");
            notInSemiNet.add("RBC/Cotisation CSL");

            if (lineBean != null)
            {
                listRBC.add(lineBean);
            }

            // cleanup lines
            List<RBCLineBean> newListRBC = new ArrayList<RBCLineBean>();
            for (RBCLineBean oneLine : listRBC)
            {
                if (!oneLine.isForInformation())
                {
                    // System.out.println("Search [" + oneLine.getDescription() + "]");
                    // Replace & standard label
                    for (Entry<String, String> map : mapLabelToReplace.entrySet())
                    {
                        if (oneLine.getDescription().startsWith(map.getKey()))
                        {
                            // System.out.println(
                            // "Replace [" + oneLine.getDescription() + "] label by [" + map.getValue() + "]");
                            oneLine.setDescription(map.getValue());
                            break;
                        }
                    }
                    boolean shouldBeNeg = false;
                    for (String shouldBeNegative : finalLabelShouldBeNegative)
                    {
                        if (oneLine.getDescription().equals(shouldBeNegative))
                        {
                            shouldBeNeg = true;
                        }
                    }
                    if (oneLine.getMontantAsDouble() < 0)
                    {
                        if (!shouldBeNeg)
                        {
                            System.err.println(
                                    oneLine.getDescription() + " should be POSITIVE ! " + oneLine.getMontantAsDouble());
                        }
                    }
                    else
                    {
                        if (shouldBeNeg)
                        {
                            System.err.println(
                                    oneLine.getDescription() + " should be neg ! " + oneLine.getMontantAsDouble());

                            String montant = oneLine.getMontant().replaceFirst("\"", "\"-");
                            oneLine.setMontant(montant);
                        }

                    }
                    newListRBC.add(oneLine);
                }

            }
            System.out.println("Nb Ligne RBC final = " + newListRBC.size());

            for (RBCLineBean cbBean : newListRBC)
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
                double totalSemiNet = 0.0;

                bw.write("Mode;Date;Details;Montant");
                bw.newLine();

                for (RBCLineBean cbBean : newListRBC)
                {

                    if (!cbBean.isForInformation())
                    {
                        String stringMontant = cbBean.getMontant().replace("\"", "");

                        // Float montant = Float.parseFloat(stringMontant);
                        // DecimalFormat df = new DecimalFormat("0.00");
                        // df.setMaximumFractionDigits(2);
                        String stringMontantForDoubleParsing = "";
                        if (stringMontant.contains(","))
                        {
                            stringMontantForDoubleParsing = stringMontant.replace(",", ".");
                        }
                        else
                        {
                            stringMontantForDoubleParsing = stringMontant;
                        }
                        double montant = Double.parseDouble(stringMontantForDoubleParsing);
                        total += montant;

                        boolean notInSemiNetSo = false;
                        for (String notInSemiNetEle : notInSemiNet)
                        {
                            if (cbBean.getDescription().equals(notInSemiNetEle))
                            {
                                notInSemiNetSo = true;
                            }
                        }
                        if (!notInSemiNetSo)
                        {
                            totalSemiNet += montant;
                        }

                        cbBean.setMontant(cbBean.getMontant().replace(".", ","));
                        bw.write(cbBean.toString());
                        bw.newLine();
                    }
                }
                bw.close();
                System.out.println("Semi Net = " + new DecimalFormat("#.##").format(totalSemiNet));
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
