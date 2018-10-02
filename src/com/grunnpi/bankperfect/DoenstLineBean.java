package com.grunnpi.bankperfect;

public class DoenstLineBean
{
    private String type = "Carte";
    private String dateTransaction = "";
    private String dateTraitement = "";
    private String description = "";
    private String montant = "";
    private String bail = "";
    private String operation = "";
    private String lot = "";

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

    /**
     * @return the bail
     */
    public String getBail()
    {
        return bail;
    }

    /**
     * @param bail the bail to set
     */
    public void setBail(String bail)
    {
        this.bail = bail;
    }

    /**
     * @return the operation
     */
    public String getOperation()
    {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    /**
     * @return the lot
     */
    public String getLot()
    {
        return lot;
    }

    /**
     * @param lot the lot to set
     */
    public void setLot(String lot)
    {
        this.lot = lot;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("DoenstLineBean [type=").append(type).append("; dateTransaction=").append(dateTransaction)
                .append("; dateTraitement=").append(dateTraitement).append("; description=").append(description)
                .append("; montant=").append(montant).append("; bail=").append(bail).append("; operation=")
                .append(operation).append("; lot=").append(lot).append("]");
        return builder.toString();
    }
}
