function importBIL(sheet) {
  log_debug("importBIL[" + sheet.getName() + "]")
  var data = sheet.getDataRange().getValues();
  var rowNumber = 0
  var tableIsOver = false

  var account_rows = []

  data.forEach(function (row) {
    rowNumber++
    if ( !tableIsOver ) {
      if (rowNumber >= 6 ) {
        if ( row[1] == "" ) {
          //log_debug("row ends here " + rowNumber + " = " + row)
          tableIsOver = true
        } else {
          var new_date = row[0]
          if ( new_date instanceof Date && new_date != undefined) {
            new_date.setHours(0,0,0,0)
          } else {
            log_error("ligne erreur [" + new_date + "] pas une date : " + row)
          }

          // le montant (nombre ou texte) est normalisé par addAccountRow -> parseAmount
          addAccountRow(account_rows,new_date,"BIL-Courant","",row[2] + "|" + row[3],"","",row[4],"")
        }
      }
    }
  });

  if ( account_rows.length > 0) {
    log_debug("[" + account_rows.length + "] rows to import")
    importRowsToSheet(account_rows,"BIL-Courant",true,true)
    //importRowsToSheet(account_rows,"Import",true)
  } else {
    displayToastAlert("No rows detected to import")
  }
}
