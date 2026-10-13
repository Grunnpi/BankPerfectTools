function createTransfert(type) {

  const ss = SpreadsheetApp.getActive();
  const sh = ss.getActiveSheet();
  const a = sh.getActiveRangeList().getRanges().map(r => ({
    sheet: sh.getName(),
    first: r.getRow(),
    last: r.getLastRow()
  }));

  // cache par utilisateur + JSON (robuste aux ',' et ';' dans les noms d'onglets)
  CacheService.getUserCache().put("selected_stuff", JSON.stringify(a), 1800)

  var html = HtmlService.createTemplateFromFile('dialog_transfert')
  html.data = a

  
  SpreadsheetApp.getUi() // Or DocumentApp or SlidesApp or FormApp.
      .showModalDialog(html.evaluate().setWidth(400).setHeight(200), 'Transfert');
}

function appendRowFromFormSubmit(compte_cible) {
  try {
    const cached = CacheService.getUserCache().get("selected_stuff")
    if (!cached) {
      log_warn("Transfert : aucune sélection en cache (expirée ?) - stop")
      return
    }
    const selection = JSON.parse(cached)
    log_debug("Transfert vers [" + compte_cible + "] sélection = " + cached)

    const ss = SpreadsheetApp.getActive()
    const transfert_rows = []

    selection.forEach(function (sel) {
      const compte_source = sel.sheet
      const sourceSheet = ss.getSheetByName(compte_source)
      const nbRows = sel.last - sel.first + 1
      log_debug("Onglet[" + compte_source + "] lignes [" + sel.first + "]à[" + sel.last + "]")

      // lecture groupée des colonnes A:H (D = libellé)
      const rows = sourceSheet.getRange(sel.first, 1, nbRows, 8).getValues()
      const labels_source = []

      rows.forEach(function (oneRow) {
        let mot_liaison_source = "vers "
        let mot_liaison_cible = "depuis "
        if (oneRow[6] > 0) {
          mot_liaison_source = "depuis "
          mot_liaison_cible = "vers "
        }
        const label_transfert_cible = "[transfert " + mot_liaison_cible + compte_source + "] " + oneRow[3]
        const label_transfert_source = "[transfert " + mot_liaison_source + compte_cible + "] " + oneRow[3]

        labels_source.push([label_transfert_source])

        const montant_inverse = oneRow[6] * -1
        addAccountRow(transfert_rows, oneRow[0], compte_cible, oneRow[2], label_transfert_cible, oneRow[4], oneRow[5], montant_inverse, oneRow[7])
      })

      // 1 seule écriture des libellés par plage
      sourceSheet.getRange(sel.first, 4, nbRows, 1).setValues(labels_source)
    })

    log_debug("transfert_rows = " + transfert_rows.length)
    if (transfert_rows.length > 0) {
      importRowsToSheet(transfert_rows, compte_cible, true)
    }
  } finally {
    flushLogs()
  }
}
