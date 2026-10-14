
const TECHNICAL_SHEETS = ['GrunnaCompte','Comptes','Logs','Totaux','Recherche','Evenement','Import','Categories','Rules','Portefeuille','Carnet d\'Ordres','Root']

function onOpen(e) {
  var ui = SpreadsheetApp.getUi();

  ui.createMenu("🤖Grunna")
    .addItem("▶️ Transfert", "createTransfert")
    .addItem('⚙️ Category', 'autoCategory')
    .addItem("⚠️ clear Logs","clearLogs")
    .addItem("⬇ trier Liquide","triageLiquide")
    .addSeparator()
    .addItem('⏏ Importer un fichier…', 'showUploadDialog')
    .addToUi();
}

/**
 * Fenêtre d'import unique : le choix de l'origine (Crédit Mutuel, BIL, Output, Boursorama)
 * se fait dans la fenêtre. Les origines sont déclarées dans IMPORT_ORIGINS (b_upload.gs).
 */
function showUploadDialog() {
  var template = HtmlService.createTemplateFromFile('dialog_upload');
  template.origins = getImportOrigins();
  template.lastOrigin = PropertiesService.getUserProperties().getProperty("lastImportOrigin") || "";

  var html = template.evaluate()
      .setWidth(440)
      .setHeight(270);
  SpreadsheetApp.getUi()
      .showModalDialog(html, 'Importer un fichier');
}

function triageLiquide() {
  var targetSheet = SpreadsheetApp.getActive().getSheetByName("Liquide")
  targetSheet.sort(1,false)
}
