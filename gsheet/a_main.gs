
const TECHNICAL_SHEETS = ['GrunnaCompte','Comptes','Logs','Totaux','Recherche','Evenement','Import','Categories','Rules','Portefeuille','Carnet d\'Ordres','Root']

function onOpen(e) {
  var ui = SpreadsheetApp.getUi();

  ui.createMenu("🤖Grunna")
    .addItem("▶️ Transfert", "createTransfert")
    .addItem('⚙️ Category', 'autoCategory')
    .addItem("⚠️ clear Logs","clearLogs")
    .addItem("⬇ trier Liquide","triageLiquide")
    .addSeparator()
    .addItem('⏏ CreditMutuel.xls', 'showUploadDialogCM')
    .addItem('⏏ BIL.csv', 'showUploadDialogBIL')
    .addItem('⏏ output.csv', 'showUploadDialogOUTPUT')
    .addItem('⏏ Boursorama.csv', 'showUploadDialogBOURSORAMA')
    .addToUi();
}


function showUploadDialogCM() {
  showUploadDialog("cm")
}
function showUploadDialogBIL() {
  showUploadDialog("bil")
}
function showUploadDialogOUTPUT() {
  showUploadDialog("output")
}
function showUploadDialogBOURSORAMA() {
  showUploadDialog("BOURSORAMA")
}

function showUploadDialog(type) {
  var html = HtmlService.createHtmlOutputFromFile('dialog_upload_' + type)
      .setWidth(400)
      .setHeight(80);      
  SpreadsheetApp.getUi()
      .showModalDialog(html, 'Charger un fichier');
}

function triageLiquide() {
  var targetSheet = SpreadsheetApp.getActive().getSheetByName("Liquide")
  targetSheet.sort(1,false)
}