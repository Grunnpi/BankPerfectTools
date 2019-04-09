#1.0
#1.1 Gestion des doublons
# coding = iso-8859-15
import BP


min_date = (9999, 12, 31)
max_date = (0, 0, 0)
duplicates = []
result_duplicates = []
text_height = 15
account_names = [str(n) for n in BP.AccountName]
#EB
account_numbers = [str(n) for n in BP.AccountNumber]
account_banks_branches = []
for n in BP.AccountBranchNumber:
    n1, n2 = n.split(" ")
    account_banks_branches.append("%s-%s" %(n1, n2))
#eof EB
width_col_0 = 0
width_col_2 = 0
width_col_3 = 0
width_col_6 = 0

def to_tuple_date(bp_date):
    return int(bp_date[6:], 10), int(bp_date[3:5], 10), int(bp_date[:2], 10)

def to_bp_date(tuple_date):
    return "%02d-%02d-%04d" %(tuple_date[2], tuple_date[1], tuple_date[0])

def set_column_widths():
    if Grid.DefaultRowHeight * Grid.RowCount >= Grid.Height: Grid.DefaultColWidth = (Grid.Width - 35) / 6
    else: Grid.DefaultColWidth = (Grid.Width - 5) / 6

def resize_form(Sender):
    margin = 20
    cW = FormImport.ClientWidth
    cH = FormImport.ClientHeight
    
    edit_file.Width = cW - margin * 2 - button_browse.Width - 10
    button_browse.Left = cW - button_browse.Width - margin
    
    Grid.Top = edit_file.Top + edit_file.Height + margin
    Grid.Width = cW - margin * 2
    Grid.Height = cH - Grid.Top - margin * 2 - button_import.Height
    
    button_import.Left = (cW - button_import.Width) / 2
    button_import.Top = cH - margin - button_import.Height
    
    set_column_widths()

def str_to_float(s):
    s = s.replace("$", "").replace(" ", "").replace("�", "").replace("F", "").replace(",", ".")
    if "." in s:
        l = s.split(".")
        s = "%s.%s" %("".join(l[:-1]), l[-1] )
    try:
        f = float(s)
    except:
        return None
    return f

def fmt_float(s):
    if "-" in s: neg = "-"
    else: neg = ""
    s = s.replace(",", ".").replace("-", "").split(".")
    if s == "": return "0,00"
    if len(s) == 1:
        ipart = s[0]
        fpart = "00"
    else:
        ipart = "".join(s[:-1])
        fpart = s[-1] + "00"
    fpart = fpart[:2]
    if ipart == "": ipart = "0"
    if len(ipart) > 3:
        s = "0" * (3 - len(ipart) % 3) + ipart
        l = [s[i:i+3] for i in range(0, len(s), 3)]
        ipart = " ".join(l).lstrip("0 ")
    return neg + ipart + "," + fpart

def draw(Sender, ACol, ARow, R, State):
    cv = Sender.Canvas
    if ARow % 2 == 0: cv.Brush.Color = 0x00ffffff
    else: cv.Brush.Color = 0x00f0f0f0
    cv.FillRect(R)

    try:
        if ARow == 0:
            s = ["Compte", "Date", "Mode", "Tiers", "D�tail", "Montant"][ACol]
            cv.Font.Style = ["fsBold"]
        else:
            if ACol == 0: s = data[ARow - 1][ACol][0]
            elif ACol == 1: s = to_bp_date(data[ARow - 1][ACol])
            else: s = str(data[ARow - 1][ACol])
    except:
        s = ""

    cv.Font.Color = 0x00000000
    if s is None or s == "None":
        cv.Font.Color = 0x000000AA
    if s != "":
        if ACol == 5 and ARow > 0:
            #Montants
            fValue = str_to_float(s)
            if fValue is None: cv.Font.Color = 0x000000AA
            else:
                s = fmt_float(s)
                if not "-" in s: cv.Font.Color = 0x00007700
            cv.TextRect(R, R.Right - 4 - cv.TextWidth(s), R.Top + 5, s)
        else:
            cv.TextRect(R, R.Left + 3, R.Top + 5, s)

    cv.Brush.Style = 1
    
def extract_accounts(data):
    d = {}
    for l in data: d[l[0]] = 1
    l = d.keys()
    l.sort()
    return l

def show_mapping(data, file_accounts):
    FormMapping = CreateComponent("TForm", None)
    FormMapping.SetProps(Position = "poMainFormCenter", Width=600, Height=400, Caption = "Correspondance des comptes")

    l1 = CreateComponent("TLabel", FormMapping)
    l1.SetProps(Parent=FormMapping, Left=20, Top=20, Caption="Pour chaque compte sp�cifi� dans le fichier OFX, merci de choisir le compte BankPerfect\nauquel il correspond")
    l1.Font.Style = ["fsBold"]

    l2 = CreateComponent("TLabel", FormMapping)
    l2.SetProps(Parent=FormMapping, Left=20, Top=60, Caption="Compte sp�cifi�\ndans le fichier OFX")

    l4 = CreateComponent("TLabel", FormMapping)
    l4.SetProps(Parent=FormMapping, Left=200, Top=60, Caption="Nombre d'op�rations\n� ins�rer")

    l3 = CreateComponent("TLabel", FormMapping)
    l3.SetProps(Parent=FormMapping, Left=400, Top=60, Caption="Compte BankPerfect\no� ins�rer les op�rations")
    
    Bevel = CreateComponent("TBevel", FormMapping)
    W = FormMapping.ClientWidth - 40
    Bevel.SetProps(Parent=FormMapping, Left=20, Top=85, Width=W, Height=9, Shape="bsBottomLine")

    accounts = ["Ne pas ins�rer"]
#EB    for acc in account_names: accounts.append(acc)
#EB    current_account = BP.AccountCurrent()
    for i in range(len(account_names)):
        accounts.append("%s %s" %(account_names[i],account_numbers[i]))
#eof EB
    
    combos = {}
    y = 105
    for i in range(len(file_accounts)):
        file_account = file_accounts[i]
        l1 = CreateComponent("TLabel", FormMapping)
        l1.SetProps(Parent=FormMapping, Left=20, Top=y, Caption=file_account[0])

        l2 = CreateComponent("TLabel", FormMapping)
        l2.SetProps(Parent=FormMapping, Left=20, Top=y + l1.Height + 1, Caption=file_account[1])

        s = [line for line in data if line[0] == file_account]
        len_s = len(s)
        if len_s == 0: continue
        if len_s == 1: s = "Une op�ration"
        else: s = "%d op�rations" %len_s
        l = CreateComponent("TLabel", FormMapping)
        l.SetProps(Parent=FormMapping, Left=200, Top=y, Caption=s)

        cb = CreateComponent("TComboBox", FormMapping)
        cb.SetProps(Parent=FormMapping, Left=400, Top=y, Width=170, Style="csDropDownList")
        cb.Items.Text = "\n".join(accounts)
        current_account = len(account_numbers)
        while current_account > 0:
           current_account -= 1
           if "n� " + account_banks_branches[current_account]+"-"+account_numbers[current_account] == file_account[1]:
               current_account += 1
               break
        cb.ItemIndex = current_account
        combos[file_account] = cb
        y += cb.Height + 15

    y += 10
    button_cancel = CreateComponent("TButton", FormMapping)
    L = (FormMapping.ClientWidth - 180) / 2
    button_cancel.SetProps(Parent=FormMapping, Left=L, Top=y, Width=80, Height=25, Caption="Annuler", Cancel=1, ModalResult=2)

    button_ok = CreateComponent("TButton", FormMapping)
    button_ok.SetProps(Parent=FormMapping, Left=L + 100, Top=y, Width=80, Height=25, Caption="OK", Default=1, ModalResult=1)
    
    FormMapping.ClientHeight = y + 45
    FormMapping.Font.Name = "Tahoma"
    FormMapping.ShowModal()
    if FormMapping.ModalResult == 1:
        d = {}
        for acc in combos:
            d[acc] = combos[acc].ItemIndex - 1
        return d
    else:
        return {}

def check_duplicates(mapping, data):
    global duplicates, result_duplicates, text_height
    #On liste les comptes concern�s par l'insertion
    duplicates = []
    accounts = {}.fromkeys(mapping.values(), 1).keys()
    accounts.sort()
    bp_lines = {}
    #on extrait les num�ros de lignes qui nous int�ressent index�es en fonction du quadruplet (compte, date, mode, montant)
    for a in accounts:
        if a == -1: continue
        for i, date in enumerate(BP.OperationDate[a]):
            date = to_tuple_date(date)
            if date < min_date or date > max_date: continue
            mode = BP.OperationMode[a][i]
            value = "%.2f" %BP.OperationAmount[a][i]
            if mode.startswith("Chq"): mode = "Ch�que �mis"
            key = a, date, mode, value
            bp_lines[key] = i
    
    #On cherche les doublons
    for i, line in enumerate(data):
        account, date, mode, tiers, info, mont = line
        bp_account = mapping.get(account, -1)
        if bp_account != -1:            
            fValue = str_to_float(mont)
            if fValue is None: continue
            key = bp_account, date, mode, "%.2f" %fValue
            if key in bp_lines:
                line_file = [i, account[0], date, mode, tiers, info, fValue]
                i_bp = bp_lines[key]
                line_bp = [i_bp, bp_account, date, BP.OperationMode[bp_account][i_bp], BP.Operationthirdparty[bp_account][i_bp], BP.OperationDetails[bp_account][i_bp], fValue]
                duplicates.append([line_file, line_bp]) #line: index, account, date, mode, tiers, details, montant
    
    if len(duplicates) == 0: return {}
    #On affiche la liste des doublons pour que l'utilisateur puisse choisir lesquels ins�rer
    result_duplicates = [0] * len(duplicates)
    update_result_label()
    grid_duplicates.RowCount = len(duplicates) + 1
    text_height = grid_duplicates.Canvas.TextHeight('A')
    grid_duplicates.DefaultRowHeight = text_height * 3
    form_duplicates.ShowModal()
    do_not_insert = {}
    for i, r in enumerate(result_duplicates):
        if not r:
            index = duplicates[i][0][0]
            do_not_insert[index] = 1
    return do_not_insert

def insert_lines(sender):
    if len(data) == 0: return
    file_accounts = extract_accounts(data)
    mapping = show_mapping(data, file_accounts)
    lines_cnt = len(data)
    if mapping:
        do_not_insert = check_duplicates(mapping, data)
        errors = []
        i = 1
        for index, data_line in enumerate(data):
            if index in do_not_insert: continue
            account, date, mode, tiers, info, mont = data_line
            bp_account = mapping.get(account, -1)
            if bp_account != -1:
                fValue = str_to_float(mont)
                date = to_bp_date(date)
                if (fValue is None) or (BP.LineAdd(bp_account, date, mode, tiers, info, -1, fValue, 0) == 0):
                    errors.append("  - ligne %d: %s, %s, %s, %s, %s" %(i, date, mode, tiers, info, mont))
            i += 1
        cnt_errors = len(errors)
        if cnt_errors > 0:
            if cnt_errors == 1: caption = "Une ligne n'a pas pu �tre ins�r�e :\n"
            else: caption = "%d lignes n'ont pas pu �tre ins�r�es :\n" %len(errors)
            BP.MsgBox(caption + "\n".join(errors), 64)
        BP.AccountRefreshScreen()
        FormImport.ModalResult = 7
        
def get_ofx_value(block, key):
    parts = block.split(key)
    if len(parts) < 2: return None
    block = parts[1]
    return block.replace("<", "\n").replace("\r", "\n").split("\n")[0].strip()

def remove_multiple_spaces(s):
    s = s.replace("\t", " ")
    while "  " in s: s = s.replace("  ", " ")
    return s

def parse_file(path):
    global data, min_date, max_date
    data = []
    s = open(path, "rb").read()

    #Prend en compte les op�rations par carte enregistr�es commes comptes s�par�s
    while "<CCSTMTTRNRS>" in s: s = s.replace("<CCSTMTTRNRS>", "<STMTTRNRS>")

    if "<STMTTRNRS>" in s:
        accounts = s.split("<STMTTRNRS>")[1:]
    else:
        accounts = [s]
    
    i = 0
    for account in accounts:
        i += 1
        group_id = "Compte %d" %i
        account_id = get_ofx_value(account, "<TRNUID>")
        
        bank_num = get_ofx_value(account, "<BANKID>")
        branch_num = get_ofx_value(account, "<BRANCHID>")
        account_num = get_ofx_value(account, "<ACCTID>")
        account_number = []
        if not bank_num is None: account_number.append(bank_num)
        if not branch_num is None: account_number.append(branch_num)
        if not account_num is None: account_number.append(account_num)
        
        if account_number: account_id = (group_id, "n� " + "-".join(account_number))
        else:
            if account_id is None: account_id = (group_id, "")
            else: account_id = (group_id, "ID %s" %account_id)
        
        blocks = account.split("<STMTTRN>")[1:]

        for block in blocks:
            dt = get_ofx_value(block, "<DTPOSTED>")
            dt = ( int(dt[:4], 10), int(dt[4:6], 10), int(dt[6:8], 10) )
            if dt < min_date: min_date = dt
            if dt > max_date: max_date = dt
            mode = get_ofx_value(block, "<TRNTYPE>")
            tiers = get_ofx_value(block, "<NAME>")
            if tiers is None: tiers = ""
            details = get_ofx_value(block, "<MEMO>")
            if details is None: details = ""
            check_num = get_ofx_value(block, "<CHECKNUM>")
            if details == ".": details = ""
            montant = get_ofx_value(block, "<TRNAMT>")
            mode = calculate_paymode(mode, tiers, montant)
            if not check_num is None and "-" in montant:
                if details == "": details = "n�%s" %check_num
                else: details = "n�%s %s" %(check_num, details)
            
            tiers = remove_multiple_spaces(tiers)
            details = remove_multiple_spaces(details)

#            details = unicode(tiers, errors='ignore') # remove wrong char
#            details = unicode(tiers, errors='replace') # do not decode string at all and return blank
#            details = unicode(tiers, errors='strict') # raise exception
#            details = unicode(tiers) # raise exception

#            details = tiers.decode()
            #details = tiers.decode("utf-8")

            tiers = utf8_decode(tiers)
            details = utf8_decode(details)

            
            line = [account_id, dt, mode, tiers, details, montant]
            data.append(line)

    cnt_lines = len(data)
    if cnt_lines == 0: l1.Caption = "Fichier � importer:"
    elif cnt_lines == 1: "Fichier � importer (une ligne):"
    else: l1.Caption = "Fichier � importer (%d lignes):" %cnt_lines
    Grid.RowCount = len(data) + 1
    set_column_widths()
    Grid.Repaint()

def utf8_decode(s):
    table = {"€": "�", "�?": "?", "‚": "�", "ƒ": "�", "„": "�", "…": "�", "†": "�", "‡": "�", "ˆ": "�", "‰": "�", "Š": "�", "‹": "�", "Œ": "�", "�?": "?", "Ž": "�", "�?": "?", "�?": "?", "‘": "�", "’": "�", "“": "�", "�?": "�", "•": "�", "–": "�", "—": "�", "˜": "�", "™": "�", "š": "�", "›": "�", "œ": "�", "�?": "?", "ž": "�", "Ÿ": "�", " ": "�", "¡": "�", "¢": "�", "£": "�", "¤": "�", "¥": "�", "¦": "�", "§": "�", "¨": "�", "©": "�", "ª": "�", "«": "�", "¬": "�", "­": "�", "®": "�", "¯": "�", "°": "�", "±": "�", "²": "�", "³": "�", "´": "�", "µ": "�", "¶": "�", "·": "�", "¸": "�", "¹": "�", "º": "�", "»": "�", "¼": "�", "½": "�", "¾": "�", "¿": "�", "À": "�", "�?": "�", "Â": "�", "Ã": "�", "Ä": "�", "Å": "�", "Æ": "�", "Ç": "�", "È": "�", "É": "�", "Ê": "�", "Ë": "�", "Ì": "�", "�?": "�", "Î": "�", "�?": "�", "�?": "�", "Ñ": "�", "Ò": "�", "Ó": "�", "Ô": "�", "Õ": "�", "Ö": "�", "×": "�", "Ø": "�", "Ù": "�", "Ú": "�", "Û": "�", "Ü": "�", "�?": "�", "Þ": "�", "ß": "�", "à": "�", "á": "�", "â": "�", "ã": "�", "ä": "�", "å": "�", "æ": "�", "ç": "�", "è": "�", "é": "�", "ê": "�", "ë": "�", "ì": "�", "í": "�", "î": "�", "ï": "�", "ð": "�", "ñ": "�", "ò": "�", "ó": "�", "ô": "�", "õ": "�", "ö": "�", "÷": "�", "ø": "�", "ù": "�", "ú": "�", "û": "�", "ü": "�", "ý": "�", "þ": "�", "ÿ": "�"}
    for key in table:
        s = s.replace(key, table[key])
    return s


def calculate_paymode(ofx_paymode, ofx_name, ofx_value):
    global mapping
    
    #Cas sp�ciaux : on cherche � calculer le mode en fonction du tiers
    name = ofx_name.upper().replace(".", "").split(" ")
    if ofx_paymode in ["ATM", "PAYMENT", "DEBIT"] and "-" in ofx_value:
        if "CHEQUE" in name or "CHQ" in name: return "Ch�que �mis"
        if "RETRAIT" in name or "RET" in name or "RETR" in name or "DAB" in name: return "Retrait DAB"
        if "CARTE" in name or "ACHAT" in name or "CB" in name or "MASTERCARD" in name or "VISA" in name: return "Carte"
        if "PRELEVEMENT" in name or "PREL" in name or "PRLV" in name: return "Pr�l�vement"
        if "TIP" in name: return "TIP"
        if "VIR" in name or "VIRT" in name or "VIREMENT" in name: return "Virement �mis"
    
    if ofx_paymode in ["CREDIT"] and not "-" in ofx_value:
        if "VIR" in name or "VIRT" in name or "VIREMENT" in name: return "Virement re�u"
        if "CHEQUE" in name or "CHQ" in name or "CHECK" in name: return "D�p�t de ch�que"
    
    #Cas g�n�riques : on utilise le mapping
    default = ["Pr�l�vement", "Virement re�u"]
    if ofx_paymode is None: debit, credit = default
    else: debit, credit = mapping.get(ofx_paymode, default)
    if "-" in ofx_value: return debit
    return credit

def load_mapping(path):
    lines = open(path, "r").readlines()
    lines = [l.strip() for l in lines if "=" in l]
    d = {}
    for l in lines:
        p = l.find("=")
        key = l[:p]
        d[key] = l[p + 1:].split(",")
    return d

def browse(sender):
    path = BP.OpenDialog("Choisissez le fichier � importer", "", ".ofx", "Open Financial Exchange File (*.ofx)|*.ofx")
    if path != "":
        edit_file.Text = path
        parse_file(path)

def update_result_label():
    total = sum(result_duplicates)
    max = len(result_duplicates)
    if max == 1:
        s_max = ""
    else:
        s_max = " sur les %d pr�sent�s" %max
    if total == 0:
        s_total1 = "Aucun doublon"
        s_total2 = "ne va �tre ajout�"
    elif total == 1:
        s_total1 = "Un doublon"
        s_total2 = "va �tre ajout�"
    else:
        s_total1 = "%s doublons" %total
        s_total2 = "vont �tre ajout�s"
    label_total.Caption = "%s%s %s au compte" %(s_total1, s_max, s_total2)
    

def toggle_line():
    i = grid_duplicates.Selection.Top - 1
    result_duplicates[i] = 1 - result_duplicates[i]
    update_result_label()

def grid_duplicates_key_up(Sender, Key, Shift):
    if Key.Value == 32:
        toggle_line()
        grid_duplicates.Repaint()

def grid_mouse_up(Sender, Button, State, X, Y):
    toggle_line()

def draw_fillrect(canvas, color, R):
    canvas.brush.color = color
    canvas.FillRect(R)

def draw_duplicates(Sender, ACol, ARow, R, State):
    cv = Sender.Canvas
    if ARow == 0:
        R.Left -= 1
        R.Right += 1
    if ARow % 2 == 1: cv.Brush.Color = 0x00eeeeee
    else: cv.Brush.Color = 0x00ffffff
    cv.FillRect(R)
    cv.Font.Color = 0

    margin = int((R.Bottom - R.Top - text_height * 2) / 3)
    if ARow == 0:
        cv.Font.Style = ["fsBold"]
        s = ["Ins�rer", "Compte", "Date", "Mode", "Tiers", "D�tail", "Montant"][ACol]
        cv.TextRect(R, R.Left + int((R.Right - R.Left - cv.TextWidth(s)) / 2), R.Bottom - text_height - margin, s)
    else:
        #line: index, account, date, mode, tiers, details, montant
        if ACol == 0:
            s = ""
            x = R.Left + int((R.Right - R.Left - 10) / 2)
            y = R.Top + int((R.Bottom - R.Top - 10) / 2)
            if "gdSelected" in State:
                draw_fillrect(cv, 0, Rect(x - 2, y - 2, x + 12, y + 12))
                draw_fillrect(cv, 0x00ffffff, Rect(x - 1, y - 1, x + 11, y + 11))
            if result_duplicates[ARow - 1]:
                draw_fillrect(cv, 0x00009900, Rect(x, y, x + 10, y + 10))
            else:
                draw_fillrect(cv, 0x000000cc, Rect(x, y, x + 10, y + 10))
        else:
            line1, line2 = duplicates[ARow - 1]
            line_height = int((R.Bottom - R.Top) / 2)
            if ACol == 2:
                line1 = to_bp_date(line1[ACol])
                line2 = to_bp_date(line2[ACol])
            elif ACol == 6:
                line1 = fmt_float(str(line1[ACol]))
                line2 = fmt_float(str(line2[ACol]))
            else:
                line1 = str(line1[ACol])
                if ACol == 1:
                    line2 = account_names[line2[ACol]]
                else:
                    line2 = str(line2[ACol])
            cv.Font.Color = 0x00cc0000
            if ACol == 6: x = R.Right - 3 - cv.TextWidth(line1)
            else: x = R.Left + 3
            cv.TextRect(Rect(R.Left, R.Top, R.Right, R.Top + line_height), x, R.Top + margin, line1)
            cv.Font.Color = 0
            cv.TextRect(Rect(R.Left, R.Top + line_height, R.Right, R.Bottom), x, R.Bottom - text_height - margin, line2)

    cv.Brush.Style = 1

def resize_form_duplicates(Sender):
    margin = 20
    half = 10

    cW = form_duplicates.ClientWidth
    cH = form_duplicates.ClientHeight

    label_duplicates.Left = margin
    label_duplicates.Top = margin
    
    label_all.Left = label_duplicates.Width + margin * 2
    label_all.Top = margin
    
    grid_duplicates.SetBounds(margin, margin + half + label_duplicates.Height, cW - margin * 2, cH - margin * 4 - button_ok.Height - label_duplicates.Height * 2)
    grid_duplicates.DefaultColWidth = (grid_duplicates.Width - 35 - width_col_0 - width_col_2 - width_col_3 - width_col_6) / 3
    grid_duplicates.ColWidths[0] = width_col_0
    grid_duplicates.ColWidths[2] = width_col_2
    grid_duplicates.ColWidths[3] = width_col_3
    grid_duplicates.ColWidths[6] = width_col_6
    
    label_total.Left = margin
    label_total.Top = grid_duplicates.Top + grid_duplicates.Height + half
    
    button_ok.Left = int((cW - button_ok.Width) / 2)
    button_ok.Top = cH - button_ok.Height - margin

def check_all(Sender):
    global result_duplicates
    l = len(result_duplicates)
    if l > 0:
        result_duplicates = [1 - result_duplicates[0]] * l
    update_result_label()
    grid_duplicates.Repaint()

path_mapping = BP.BankPerfectExePath() + "Scripts\\Import OFX\\mapping.ini"
mapping = load_mapping(path_mapping)

data = []
Acc = BP.AccountCurrent()

#Fen�tre de gestion des doublons
form_duplicates = CreateComponent("TForm", None)
form_duplicates.SetProps(Position = "poMainFormCenter", Width=700, Height=400, Caption = "Liste des doublons", onResize=resize_form_duplicates)

label_duplicates = CreateComponent("TLabel", form_duplicates)
label_duplicates.SetProps(Parent=form_duplicates, Caption="Cliquez sur une ligne (ou appuyez sur ESPACE) pour la cocher ou la d�cocher")

label_all = CreateComponent("TLabel", form_duplicates)
label_all.SetProps(Parent=form_duplicates, Caption="[ Tout cocher / d�cocher ]", OnClick=check_all)

grid_duplicates = CreateComponent("TDrawGrid", form_duplicates)
grid_duplicates.SetProps(Parent=form_duplicates, ColCount=7, RowCount=2, FixedRows=1, FixedCols=0, OnMouseUp=grid_mouse_up, OnKeyUp=grid_duplicates_key_up, OnDrawCell=draw_duplicates, ScrollBars="ssVertical", Options = ["goHorzLine", "goVertLine", "goDrawFocusSelected", "goRowSelect", "goThumbTracking"])

label_total = CreateComponent("TLabel", form_duplicates)
label_total.SetProps(Parent=form_duplicates, Caption="---")

button_ok = CreateComponent("TButton", form_duplicates)
button_ok.SetProps(Parent=form_duplicates, Width=90, Height=25, Caption="OK", Default=1, ModalResult=1)

form_duplicates.Font.Name = "Tahoma"
label_all.Font.Color = 0x00cc0000
label_all.Cursor = -21

width_col_0 = grid_duplicates.Canvas.TextWidth(" Ins�rer ") + 16
width_col_2 = grid_duplicates.Canvas.TextWidth(" 13-10-2003 ") + 8
width_col_3 = grid_duplicates.Canvas.TextWidth(" D�p�t de ch�que ") + 8
width_col_6 = grid_duplicates.Canvas.TextWidth("-99 999 999,99") + 8

#Fen�tre principale
FormImport = CreateComponent("TForm", None)
FormImport.SetProps(Position = "poMainFormCenter", Width=600, Height=400, Caption = "Import OFX", onResize=resize_form)

l1 = CreateComponent("TLabel", FormImport)
l1.SetProps(Parent=FormImport, Left=20, Top=20, Caption="Fichier � importer :")

edit_file = CreateComponent("TEdit", FormImport)
edit_file.SetProps(Parent=FormImport, Left=20, Top=35, Width=250)

button_browse = CreateComponent("TButton", FormImport)
button_browse.SetProps(Parent=FormImport, Left=280, Top=35, Width=80, Height=edit_file.Height, Caption="Parcourir...", OnClick=browse)

Grid = CreateComponent("TDrawGrid", FormImport)
Grid.SetProps(Parent=FormImport, Top=205, Left=20, Width=10, Height=140, ColCount=6, RowCount=1, FixedCols=0, OnDrawCell=draw, ScrollBars="ssVertical", Options = ["goHorzLine", "goVertLine", "goDrawFocusSelected", "goRowSelect", "goThumbTracking"])

button_import = CreateComponent("TButton", FormImport)
button_import.SetProps(Parent=FormImport, Left=40, Top=320, Width=80, Height=25, Caption="Importer", OnClick=insert_lines)

FormImport.Font.Name = "Tahoma"

FormImport.ShowModal()