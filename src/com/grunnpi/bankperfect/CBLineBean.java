package com.grunnpi.bankperfect;

public class CBLineBean
{
    private String type = "Carte";
    private String dateTransaction = "";
    private String dateTraitement = "";
    private String description = "";
    private String montant = "";

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getDateTransaction()
    {
        return dateTransaction;
    }

    public void setDateTransaction(String dateTransaction)
    {

        this.dateTransaction = dateTransaction;
    }

    public String getDateTraitement()
    {
        return dateTraitement;
    }

    public void setDateTraitement(String dateTraitement)
    {

        this.dateTraitement = dateTraitement;
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

    public void setMontant(String montant)
    {
        this.montant = montant;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(type).append(';').append(dateTransaction).append(';').append(dateTraitement).append(';')
                .append(description).append(';').append(montant);
        return builder.toString();
    }
}
