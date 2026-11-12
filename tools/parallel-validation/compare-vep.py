#!/usr/bin/env python3
"""Porovnání textových protokolů VEP 2.4 a KV po pravidlech (paralelní validace, krok P6).

Vstup: adresář OUT z run-samples.sh (protokoly v OUT/runs/<vep|kv>-<konfigurace>/<balíček>.txt,
verbosity 3). Výstup do --report-dir (výchozí OUT): summary.json, tabulka.md, rozdily.md.

Pro každý balíček a konfiguraci nástrojů se porovná každé pravidlo: počty INFO/WARNING/ERROR
a multimnožina hlášek po normalizaci (cesty, prefix funkce a souboru, bílé znaky, pořadí).
Pravidla přejmenovaná v KV se mapují (RENAMED). Každá rozdílná hláška se zařadí podle tabulky
KNOWN do kategorie: úmyslný známý / oprava chyby VEP / nejasné (nezařazené).

Použití:
  compare-vep.py OUT [--report-dir DIR] [--exclude-rule RULE ...] [--exclude-info]
                     [--configs off on]
"""
import argparse
import collections
import json
import os
import re
import sys

LEVELS = ('INFO', 'WARNING', 'ERROR')

# Pravidla přejmenovaná v KV: název ve VEP -> název v KV
RENAMED = {
    'PSP_ID_DERIVED_FROM_TITLE_ID': 'PSP_ID_DERIVED_FROM_IE_ID',  # VEP #21
}

CAT_INTENDED = 'úmyslný známý'
CAT_VEP_FIX = 'oprava chyby VEP'
CAT_UNCLEAR = 'nejasné'
CATEGORIES = (CAT_INTENDED, CAT_VEP_FIX, CAT_UNCLEAR)

# Známé rozdíly. Každá položka: id, kategorie, popis a podmínky na rozdílnou hlášku:
#   rule  regulární výraz na název pravidla (v pojmenování KV)
#   side  'vep' = hláška jen ve VEP, 'kv' = jen v KV, 'level' = stejný text, jiná úroveň
#   level regulární výraz na úroveň (u 'level' ve tvaru 'VEP>KV', např. 'WARNING>ERROR')
#   msg   regulární výraz (re.search) na normalizovaný text hlášky
#   pkg   regulární výraz na název balíčku (výchozí libovolný)
# První vyhovující položka vyhrává.
KNOWN = []


def K(id, cat, desc, rule='.*', side=None, level='.*', msg='.*', pkg='.*'):
    KNOWN.append(dict(id=id, cat=cat, desc=desc, rule=re.compile(rule), side=side,
                      level=re.compile(level), msg=re.compile(msg), pkg=re.compile(pkg)))


# Tabulka KNOWN je definována ve known.py vedle skriptu (aby šla upravovat bez zásahu do logiky).
_here = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _here)
try:
    import known  # noqa: E402
    known.register(K, CAT_INTENDED, CAT_VEP_FIX)
except ImportError:
    pass

RE_RULE = re.compile(r'^Pravidlo (\S+): (\d+) problém\w*(?: \(([^)]*)\))?\s*$')
RE_SECTION = re.compile(r'^Sekce (.*): \d+ problém')
RE_MSG = re.compile(r'^\t(INFO|WARNING|ERROR): (.*)$')
RE_TOTAL = re.compile(r'^Celkem: \d+ problém\w*(?: \(([^)]*)\))?, balík je: (\w+)')
RE_DMF = re.compile(r'Bude použita verze standardu (.*)')


def parse_counts(s):
    c = dict.fromkeys(LEVELS, 0)
    for v, k in re.findall(r'(\d+)x (INFO|WARNING|ERROR)', s or ''):
        c[k] = int(v)
    return c


def normalize(msg, ctx):
    m = msg
    for path, repl in ctx:
        if path:
            m = m.replace(path, repl)
    m = re.sub(r'/(?:private/)?(?:var|tmp)/\S*?/(?:tmp|t)_?[^/\s]*/', '<TMP>/', m)
    # prefix názvu validační funkce (VEP: "checkXxx: ...")
    m = re.sub(r'^[a-z][A-Za-z0-9]+: ', '', m)
    # prefix souboru a chybějícího ID záznamu (KV: "mets_x.xml: null: ", "mets_x.xml: MODSMD_...: ")
    m = re.sub(r'^[^\s:]+\.xml: (null: )?(?=[A-Z][A-Z0-9_]*_\d{4}: |/)', '', m)
    # VEP připojuje k hláškám binárních profilů "(soubor <cesta>)", KV ne (soubor je jen v XML protokolu)
    m = re.sub(r' \(soubor [^()]*\)$', '', m)
    # názvy XSD se v KV liší (info_eper2.5.xsd -> info_epe_2.5.xsd)
    m = re.sub(r'<CFG>/fDMF/[^/\s]+/xsd/[^\s:]+\.xsd', '<XSD>', m)
    # názvy fDMF v KV zkráceny (Elektronické monografie -> E-monografie)
    m = m.replace('Elektronické monografie', 'E-monografie').replace('Elektronická periodika', 'E-periodika')
    # uvozovky kolem hodnot (VEP: identifikátor "x", KV: identifikátor x)
    m = re.sub(r'"([^"\s]*)"', r'\1', m)
    m = re.sub(r'\s+', ' ', m).strip().rstrip(',').strip()
    return m


def parse(path, ctx):
    if not os.path.exists(path):
        return None
    with open(path, encoding='utf-8', errors='replace') as f:
        lines = f.read().split('\n')
    r = dict(dmf=None, totals=None, verdict=None, rules={}, failed=False, fail=None)
    rule = section = None
    last = None
    for ln in lines:
        m = RE_DMF.search(ln)
        if m and r['dmf'] is None:
            r['dmf'] = m.group(1).strip()
            continue
        m = RE_SECTION.match(ln)
        if m:
            section = m.group(1)
            rule = None
            continue
        m = RE_RULE.match(ln)
        if m:
            rule = dict(section=section, counts=parse_counts(m.group(3)), msgs=[])
            r['rules'][m.group(1)] = rule
            last = None
            continue
        m = RE_TOTAL.match(ln)
        if m:
            r['totals'] = parse_counts(m.group(1))
            r['verdict'] = m.group(2)
            rule = None
            continue
        if rule is None:
            continue
        m = RE_MSG.match(ln)
        if m:
            last = [m.group(1), m.group(2)]
            rule['msgs'].append(last)
        elif ln.startswith('\t') and last is None:
            pass  # popis pravidla
        elif ln.strip() and last is not None and not ln.startswith('---'):
            last[1] += ' ' + ln.strip()  # pokračování víceřádkové hlášky
    for rd in r['rules'].values():
        rd['msgs'] = collections.Counter((lv, normalize(t, ctx)) for lv, t in rd['msgs'])
    if r['verdict'] is None:
        r['failed'] = True
        err = path[:-4] + '.err'
        txt = '\n'.join(lines)
        if os.path.exists(err):
            txt += '\n' + open(err, encoding='utf-8', errors='replace').read()
        m = re.search(r'(Není definována fDMF[^\n]*|[A-Za-z.]*Exception[^\n]*)', txt)
        r['fail'] = normalize(m.group(1), ctx) if m else '?'
    return r


def filtered(rd, exclude_info):
    counts = dict(rd['counts'])
    msgs = collections.Counter(rd['msgs'])
    if exclude_info:
        counts['INFO'] = 0
        msgs = collections.Counter({k: v for k, v in msgs.items() if k[0] != 'INFO'})
    return counts, msgs


def classify(pkg, rule, side, level, msg):
    for k in KNOWN:
        if k['side'] is not None and k['side'] != side:
            continue
        if k['rule'].search(rule) and k['level'].search(level) and k['msg'].search(msg) \
                and k['pkg'].search(pkg):
            return k
    return None


def diff_rule(pkg, rule, v, k):
    """v, k: (counts, msgs) nebo None. Vrací seznam položek rozdílu."""
    vc, vm = v if v else (dict.fromkeys(LEVELS, 0), collections.Counter())
    kc, km = k if k else (dict.fromkeys(LEVELS, 0), collections.Counter())
    vo = vm - km
    ko = km - vm
    items = []
    # stejný text, jiná úroveň
    vtext = collections.defaultdict(list)
    for (lv, t), n in vo.items():
        vtext[t].append((lv, n))
    for (lv, t), n in list(ko.items()):
        if t in vtext:
            for i, (vlv, vn) in enumerate(vtext[t]):
                if vlv != lv and vn > 0:
                    p = min(vn, n)
                    items.append(dict(side='level', level=f'{vlv}>{lv}', msg=t, n=p))
                    vtext[t][i] = (vlv, vn - p)
                    vo[(vlv, t)] -= p
                    ko[(lv, t)] -= p
                    n -= p
                    if n == 0:
                        break
    for (lv, t), n in (+vo).items():
        items.append(dict(side='vep', level=lv, msg=t, n=n))
    for (lv, t), n in (+ko).items():
        items.append(dict(side='kv', level=lv, msg=t, n=n))
    # rozdíl v počtech bez rozdílu hlášek (hlášky ve výpisu chybí nebo jsou zkrácené)
    if not items and vc != kc:
        items.append(dict(side='count', level='', msg=f'počty VEP {vc} vs. KV {kc}', n=1))
    for it in items:
        kn = classify(pkg, rule, it['side'], it['level'], it['msg'])
        it['known'] = kn['id'] if kn else None
        it['cat'] = kn['cat'] if kn else CAT_UNCLEAR
    return items


def fmt_counts(r):
    if r is None:
        return '–'
    if r['failed']:
        return 'běh selhal'
    t = r['totals']
    return f"{t['INFO']}/{t['WARNING']}/{t['ERROR']}"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('out', help='adresář OUT z run-samples.sh')
    ap.add_argument('--report-dir', help='kam zapsat summary.json, tabulka.md, rozdily.md (výchozí OUT)')
    ap.add_argument('--exclude-rule', action='append', default=[], help='vyřadit pravidlo (název v KV), lze opakovat')
    ap.add_argument('--exclude-info', action='store_true', help='nepočítat hlášky a počty INFO')
    ap.add_argument('--configs', nargs='*', default=['off', 'on'])
    ap.add_argument('--samples', default=None, help='kořen vzorků (pro normalizaci cest)')
    ap.add_argument('--config-dir', action='append', default=None,
                    help='adresáře konfigurace VEP a KV (pro normalizaci cest), lze opakovat; '
                         'výchozí .local/vep-2.4/validatorConfig a validatorConfig KV v repu')
    ap.add_argument('--max-examples', type=int, default=5, help='počet příkladů hlášek u nejasných rozdílů')
    a = ap.parse_args()
    repo = os.path.abspath(os.path.join(_here, '..', '..'))
    if a.config_dir is None:
        a.config_dir = [os.path.join(repo, '.local/vep-2.4/validatorConfig'),
                        os.path.join(repo, 'modules/sharedModule/src/main/resources/nkp/pspValidator/shared/validatorConfig')]
    out = os.path.abspath(a.out)
    rep = os.path.abspath(a.report_dir or out)
    os.makedirs(rep, exist_ok=True)
    pkgs = [l.rstrip('\n') for l in open(os.path.join(out, 'packages.txt')) if l.strip()]
    samples = a.samples or os.path.commonpath(pkgs)
    names = []
    for c in a.configs:
        d = os.path.join(out, 'runs', f'kv-{c}')
        if os.path.isdir(d):
            names += [f[:-4] for f in os.listdir(d) if f.endswith('.txt')]
    names = sorted(set(names), key=lambda s: s.lower())
    ctx = [(samples, '<SAMPLES>'), (out, '<OUT>')] + [(p, '<CFG>') for p in a.config_dir]

    results = []
    for c in a.configs:
        for name in names:
            vr = parse(os.path.join(out, 'runs', f'vep-{c}', name + '.txt'), ctx)
            kr = parse(os.path.join(out, 'runs', f'kv-{c}', name + '.txt'), ctx)
            if vr is None and kr is None:
                continue
            res = dict(package=name, config=c, vep=None, kv=None, rules={}, only_vep=[], only_kv=[])
            for side, r in (('vep', vr), ('kv', kr)):
                if r is not None:
                    res[side] = dict(dmf=r['dmf'], totals=r['totals'], verdict=r['verdict'],
                                     failed=r['failed'], fail=r['fail'])
            vrules = {RENAMED.get(n, n): rd for n, rd in (vr['rules'] if vr else {}).items()}
            krules = (kr['rules'] if kr else {})
            for rule in sorted(set(vrules) | set(krules)):
                if rule in a.exclude_rule:
                    continue
                v = filtered(vrules[rule], a.exclude_info) if rule in vrules else None
                k = filtered(krules[rule], a.exclude_info) if rule in krules else None
                if v is None and not (vr is None or vr['failed']):
                    res['only_kv'].append(rule)
                if k is None and not (kr is None or kr['failed']):
                    res['only_vep'].append(rule)
                items = diff_rule(name, rule, v, k)
                if items:
                    res['rules'][rule] = dict(
                        vep=v[0] if v else None, kv=k[0] if k else None, items=items,
                        cats=sorted({i['cat'] for i in items}, key=CATEGORIES.index))
            if (vr and vr['failed']) != (kr and kr['failed']) or \
                    (vr and kr and vr['failed'] and kr['failed'] and vr['fail'] != kr['fail']):
                res['rules']['(běh)'] = dict(vep=None, kv=None, cats=[CAT_UNCLEAR], items=[dict(
                    side='run', level='', n=1, known=None, cat=CAT_UNCLEAR,
                    msg=f"VEP: {vr and (vr['fail'] or 'OK')} / KV: {kr and (kr['fail'] or 'OK')}")])
            results.append(res)

    write_summary(rep, results, a)
    write_table(rep, results, a)
    write_diffs(rep, results, a)
    n_unclear = sum(1 for r in results for rd in r['rules'].values() if CAT_UNCLEAR in rd['cats'])
    print(f'{len(results)} porovnání, pravidel s nejasným rozdílem: {n_unclear}; výstup v {rep}')


def verdict_cell(r):
    if r['vep'] is None or r['kv'] is None:
        return 'chybí protokol'
    if r['vep']['failed'] or r['kv']['failed']:
        return 'oba selhaly' if r['vep']['failed'] and r['kv']['failed'] else 'selhal jeden'
    a, b = r['vep']['verdict'], r['kv']['verdict']
    return f'shoda ({a})' if a == b else f'VEP {a}, KV {b}'


def write_summary(rep, results, a):
    data = dict(params=dict(exclude_rule=a.exclude_rule, exclude_info=a.exclude_info, configs=a.configs,
                            renamed=RENAMED),
                known=[dict(id=k['id'], cat=k['cat'], desc=k['desc']) for k in KNOWN],
                results=results)
    with open(os.path.join(rep, 'summary.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=1)


def write_table(rep, results, a):
    L = ['# Paralelní validace VEP 2.4 × KV: souhrn', '',
         'Počty ve tvaru INFO/WARNING/ERROR. „Pravidla s rozdílem“ = pravidla, kde se liší počty nebo hlášky '
         'po normalizaci; v závorce rozpad podle kategorie (úmyslné / opravy VEP / nejasné).'
         + (' INFO vyřazeny.' if a.exclude_info else '')
         + (f" Vyřazená pravidla: {', '.join(a.exclude_rule)}." if a.exclude_rule else ''), '']
    for c in a.configs:
        rs = [r for r in results if r['config'] == c]
        if not rs:
            continue
        L += [f'## Nástroje {"zapnuté" if c == "on" else "vypnuté"} ({c})', '',
              '| balíček | fDMF (KV) | VEP I/W/E | KV I/W/E | verdikt | pravidla s rozdílem (ú/o/n) |',
              '|---|---|---|---|---|---|']
        for r in rs:
            cnt = collections.Counter()
            for rd in r['rules'].values():
                for cat in rd['cats']:
                    cnt[cat] += 1
            n = len(r['rules'])
            dmf = (r['kv'] or {}).get('dmf') or (r['vep'] or {}).get('dmf') or '?'
            L.append(f"| `{r['package']}` | {dmf} | {fmt_counts(r['vep'])} | {fmt_counts(r['kv'])} | "
                     f"{verdict_cell(r)} | {n} ({cnt[CAT_INTENDED]}/{cnt[CAT_VEP_FIX]}/{cnt[CAT_UNCLEAR]}) |")
        L.append('')
    with open(os.path.join(rep, 'tabulka.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(L))


def write_diffs(rep, results, a):
    # agregace známých rozdílů: id -> (pravidlo, side, level) -> {balíčky, počet hlášek}
    agg = collections.OrderedDict((k['id'], collections.defaultdict(lambda: [set(), 0])) for k in KNOWN)
    unclear = []
    for r in results:
        for rule, rd in r['rules'].items():
            for it in rd['items']:
                if it['known']:
                    e = agg[it['known']][(rule, it['side'], it['level'])]
                    e[0].add(f"{r['package']} [{r['config']}]")
                    e[1] += it['n']
                else:
                    unclear.append((r, rule, rd, it))
    L = ['# Paralelní validace VEP 2.4 × KV: rozdíly', '',
         'Rozdíly po pravidlech a hláškách po normalizaci. Strana „vep“ = hláška jen ve VEP, „kv“ = jen v KV, '
         '„level“ = stejný text s jinou úrovní (VEP>KV).', '']
    for cat in (CAT_INTENDED, CAT_VEP_FIX):
        L += [f'## Kategorie: {cat}', '']
        for k in KNOWN:
            if k['cat'] != cat:
                continue
            groups = agg[k['id']]
            if not groups:
                continue
            L += [f"### {k['id']}", '', k['desc'], '',
                  '| pravidlo | strana | úroveň | hlášek | balíčků | konfigurace |', '|---|---|---|---|---|---|']
            for (rule, side, level), (pk, n) in sorted(groups.items()):
                cfgs = sorted({p.rsplit('[', 1)[1].rstrip(']') for p in pk})
                L.append(f'| {rule} | {side} | {level} | {n} | {len({p.rsplit(" [", 1)[0] for p in pk})} | '
                         f'{", ".join(cfgs)} |')
            L.append('')
    L += [f'## Kategorie: {CAT_UNCLEAR}', '']
    if not unclear:
        L += ['Žádné.', '']
    by = collections.OrderedDict()
    for r, rule, rd, it in unclear:
        by.setdefault((r['package'], r['config'], rule), (rd, []))[1].append(it)
    for (pkg, cfg, rule), (rd, its) in by.items():
        L += [f'### {pkg} [{cfg}] {rule}', '',
              f"VEP {rd['vep']} / KV {rd['kv']}" if rd.get('vep') is not None or rd.get('kv') is not None else '', '']
        for it in its[:a.max_examples]:
            L.append(f"- {it['side']} {it['level']} ×{it['n']}: `{it['msg'][:400]}`")
        if len(its) > a.max_examples:
            L.append(f'- … a dalších {len(its) - a.max_examples} rozdílných hlášek')
        L.append('')
    with open(os.path.join(rep, 'rozdily.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(L))


if __name__ == '__main__':
    main()
