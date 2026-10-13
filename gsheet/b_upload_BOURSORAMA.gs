function importBOURSORAMA(sheet) {
  log_debug("importBOURSORAMA[" + sheet.getName() + "]")
  var data = sheet.getDataRange().getValues();
  var rowNumber = 0
  var tableIsOver = false

  var account_rows = []

  data.forEach(function (row) {
    rowNumber++
    if ( !tableIsOver ) {
      if (rowNumber >= 2 ) {
        if ( row[0] == "" ) {
          log_debug("row ends here " + rowNumber + " = " + row)
          tableIsOver = true
        } else {
          var new_date = row[0]
          if ( !(new_date instanceof Date) ) {
            log_error("ligne ignorée ligne " + rowNumber + " : [" + new_date + "] pas une date : " + row)
            return
          }
          new_date.setHours(0,0,0,0)

          // le montant (nombre ou texte) est normalisé par addAccountRow -> parseAmount
          addAccountRow(account_rows,new_date,"Boursorama-Courant-Magali","",row[2],"","",row[6],"")
        }
      }
    }
  });

  if ( account_rows.length > 0) {
    log_debug("[" + account_rows.length + "] rows to import")
    importRowsToSheet(account_rows,"Boursorama-Courant-Magali",true,true)
  } else {
    displayToastAlert("No rows detected to import")
  }
}
