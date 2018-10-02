package com.grunnpi.bankperfect;

public class RBCLineBean
{
    private String type = "Versement";
    private String dateOperation = "";
    private String description = "";
    private String montant = "";
    private boolean forInformation;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getDescription()
    {

        return description;
    }

    public void setDescription(String description)
    {

        this.description = description;
    }

    public String getMontant()
    {
        return montant;
    }

    public Double getMontantAsDouble()
    {

        String stringMontant = montant.replace("\"", "");

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
        double montantDouble = Double.parseDouble(stringMontantForDoubleParsing);

        return montantDouble;
    }

    public void setMontant(String montant)
    {
        this.montant = montant;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(type).append(';').append(dateOperation).append(';').append(description).append(';')
                .append(montant);
        return builder.toString();
    }

    /**
     * @return the dateOperation
     */
    public String getDateOperation()
    {
        return dateOperation;
    }

    /**
     * @param dateOperation the dateOperation to set
     */
    public void setDateOperation(String dateOperation)
    {
        this.dateOperation = dateOperation;
    }

    /**
     * @return the forInformation
     */
    public boolean isForInformation()
    {
        return forInformation;
    }

    /**
     * @param forInformation the forInformation to set
     */
    public void setForInformation(boolean forInformation)
    {
        this.forInformation = forInformation;
    }
}
