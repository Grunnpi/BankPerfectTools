const IDX_RULE_MOT_CLEF                 = 0
const IDX_RULE_TIER                     = 1
const IDX_RULE_LOGIQUE_MOT_CLEF         = 2
const IDX_RULE_MOT_CLEF_CASE_SENSITIVE  = 3
const IDX_RULE_LOGIQUE_TIER             = 4
const IDX_RULE_STATUS                   = 5
const IDX_RULE_CATEGORIE_ID             = 6
const IDX_RULE_CATEGORIE_VAL            = 7
const IDX_RULE_CATEGORIE_SOUS_VAL       = 8

const MOT_CLEF_CASE_SENSITIVE           = "sensitive" // else "insensitive" = default

function getRulesValues() {
  var rulesSheet = SpreadsheetApp.getActive().getSheetByName("Rules")
  var rulesValues = rulesSheet.getDataRange().getDisplayValues();
  return rulesValues
}

function rowMatchRule(rulesValues, the_row) {
  var the_rule_row_number = -1
  for (var i=1;i<rulesValues.length;i++){
    if ( isRowMatchRule(the_row,rulesValues[i]) )  {
      the_rule_row_number = i
      break
    }
  }
  return the_rule_row_number
}

// Mémo du dernier libellé passé en majuscules : pour une même ligne, on boucle sur toutes
// les règles avec le même libellé -> un seul toUpperCase() par ligne au lieu d'un par règle.
var _memoLabel = null
var _memoUpper = null
function _upperLabel(label) {
  if (label !== _memoLabel) {
    _memoLabel = label
    _memoUpper = label.toUpperCase()
  }
  return _memoUpper
}

// Pré-calcul (une seule fois par règle et par exécution) : mots-clés découpés et en majuscules
function _compileRule(the_rule) {
  if (the_rule._compiled) return the_rule._compiled
  var caseSensitive = ( the_rule[IDX_RULE_MOT_CLEF_CASE_SENSITIVE] == MOT_CLEF_CASE_SENSITIVE )
  var logic = the_rule[IDX_RULE_LOGIQUE_MOT_CLEF]
  var raw = String(the_rule[IDX_RULE_MOT_CLEF])
  var keywords = ( logic == "libellé" ) ? [raw] : raw.split("+")
  if ( !caseSensitive ) {
    keywords = keywords.map(function (k) { return k.toUpperCase() })
  }
  the_rule._compiled = { caseSensitive: caseSensitive, logic: logic, keywords: keywords }
  return the_rule._compiled
}

/*
  Rules matcher :
  - label contains rule flag
  - if rule flag contains "+", label contains all "+" separated values
*/
function isRowMatchRule(the_row, the_rule) {
  var row_label = the_row[3].toString()

  if ( the_rule[IDX_RULE_STATUS] != "active" ) {
    return false
  }

  var c = _compileRule(the_rule)
  var label = c.caseSensitive ? row_label : _upperLabel(row_label)
  var contains = function (keyword) { return label.indexOf(keyword) >= 0 }

  if ( c.logic == "libellé" ) {
    return contains(c.keywords[0])
  }
  if ( c.logic == "et+" ) {
    return c.keywords.every(contains)
  }
  if ( c.logic == "ou+" ) {
    return c.keywords.some(contains)
  }

  log_error("(" + row_label + ") ? Rules[" + the_rule[IDX_RULE_MOT_CLEF] + "] type[" + the_rule[IDX_RULE_LOGIQUE_MOT_CLEF] + "] unknown")
  return false
}


function assignRuleToRow(currentSheet, row_line, the_rule, batchBuffer) {
  // Mode batch : on stocke dans le buffer sans toucher l'API
  if (batchBuffer) {
    batchBuffer.push({ row_line, the_rule });
    return;
  }
  // Mode direct (fallback) : comportement original
  _applyRuleToSheet(currentSheet, row_line, the_rule);
}

function _applyRuleToSheet(currentSheet, row_line, the_rule) {
  if (the_rule[IDX_RULE_LOGIQUE_TIER] !== "fait_rien") {
    if (the_rule[IDX_RULE_LOGIQUE_TIER] === "si_vide") {
      const prev = currentSheet.getRange("C" + row_line).getValue();
      if (prev === "") currentSheet.getRange("C" + row_line).setValue(the_rule[IDX_RULE_TIER]);
    } else {
      currentSheet.getRange("C" + row_line).setValue(the_rule[IDX_RULE_TIER]);
    }
  }
  currentSheet.getRange("E" + row_line).setValue(the_rule[IDX_RULE_CATEGORIE_VAL]);
  currentSheet.getRange("F" + row_line).setValue(the_rule[IDX_RULE_CATEGORIE_SOUS_VAL]);
  currentSheet.getRange("H" + row_line).setValue("-");
}

/**
 * Vide le buffer batch en un minimum d'appels API
 */
function flushRuleBatch(currentSheet, batchBuffer, allSheetValues) {
  if (batchBuffer.length === 0) return;

  // allSheetValues : résultat de getDataRange().getValues() (0-based)
  // On n'écrit QUE les colonnes C, E:F et H, sur la plage de lignes touchée :
  // les autres colonnes (formules incluses) ne sont jamais réécrites.
  const lines = batchBuffer.map(b => b.row_line);
  const first = Math.min.apply(null, lines);
  const last = Math.max.apply(null, lines);
  const n = last - first + 1;

  const colC = [], colEF = [], colH = [];
  for (let r = first; r <= last; r++) {
    const v = allSheetValues[r - 1];
    colC.push([v[2]]);
    colEF.push([v[4], v[5]]);
    colH.push([v[7]]);
  }

  batchBuffer.forEach(({ row_line, the_rule }) => {
    const idx = row_line - first;
    const logic = the_rule[IDX_RULE_LOGIQUE_TIER];

    // Colonne C : tier
    if (logic === "si_vide") {
      if (colC[idx][0] === "") colC[idx][0] = the_rule[IDX_RULE_TIER];
    } else if (logic !== "fait_rien") {
      colC[idx][0] = the_rule[IDX_RULE_TIER];
    }

    colEF[idx] = [the_rule[IDX_RULE_CATEGORIE_VAL], the_rule[IDX_RULE_CATEGORIE_SOUS_VAL]]; // E, F
    colH[idx] = ["-"];                                                                      // H
  });

  currentSheet.getRange(first, 3, n, 1).setValues(colC);
  currentSheet.getRange(first, 5, n, 2).setValues(colEF);
  currentSheet.getRange(first, 8, n, 1).setValues(colH);
}

// ─── autoCategory optimisée ────────────────────────────────────────────────

function autoCategory() {
  try {
    log_debug("AutoCategory - début batch");

    var currentSheet = SpreadsheetApp.getActiveSheet();

    // 1 lecture
    var allValues = currentSheet.getDataRange().getValues();
    var rulesValues = getRulesValues();

    var batchBuffer = [];
    var nbAutoMatchLines = 0;

    for (let i = 1; i < allValues.length; i++) {
      const row = allValues[i];
      if (row[7] !== "N") continue;

      for (let r = 1; r < rulesValues.length; r++) {
        if (isRowMatchRule(row, rulesValues[r])) {
          assignRuleToRow(null, i + 1, rulesValues[r], batchBuffer);
          nbAutoMatchLines++;
          break;
        }
      }
    }

    log_debug(" Lignes matchées [" + nbAutoMatchLines + "]");

    // 1 écriture pour tout
    flushRuleBatch(currentSheet, batchBuffer, allValues);

    log_debug(" AutoCategory terminée");
  } finally {
    flushLogs();
  }
}
