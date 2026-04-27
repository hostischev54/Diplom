# tab_builder.py
print("TAB_BUILDER LOADED FROM:", __file__)

def generate_target_tunings():
    """
    Только нужные строи:
    Standard: E, Eb, D, Db, C, B  (shift 0..-5)
    Drop:     D, Db, C, B, Bb, A  (shift 0..-5, Drop D = shift 0)
    """
    tunings = {}

    # Standard base: E2 A2 D3 G3 B3 E4 = [40, 45, 50, 55, 59, 64]
    standard_base = [40, 45, 50, 55, 59, 64]
    standard_names = {0: "E", -1: "Eb", -2: "D", -3: "Db", -4: "C", -5: "B"}
    for shift in range(0, -6, -1):
        strings = {6 - i: standard_base[i] + shift for i in range(6)}
        label = standard_names.get(shift, f"standard_{shift}")
        tunings[f"standard_{shift}"] = {"strings": strings, "label": label, "type": "standard", "shift": shift}

    # Drop base: D2 A2 D3 G3 B3 E4 = [38, 45, 50, 55, 59, 64]
    drop_base = [38, 45, 50, 55, 59, 64]
    drop_names = {0: "Drop D", -1: "Drop Db", -2: "Drop C", -3: "Drop B", -4: "Drop Bb", -5: "Drop A"}
    for shift in range(0, -6, -1):
        strings = {6 - i: drop_base[i] + shift for i in range(6)}
        label = drop_names.get(shift, f"drop_{shift}")
        tunings[f"drop_{shift}"] = {"strings": strings, "label": label, "type": "drop", "shift": shift}

    return tunings


ALL_TUNINGS = generate_target_tunings()

TUNING_ALIASES = {
    "standard":  "standard_0",
    "drop_d":    "drop_0",
    "half_down": "standard_-1",
    "drop_db":   "drop_-1",
}


def detect_tuning_from_notes(notes):
    if not notes:
        return "standard_0", ALL_TUNINGS["standard_0"]["strings"]

    best_name = "standard_0"
    best_score = -float("inf")

    # Небольшой штраф за пониженные строи — предпочитаем стандарт если данных мало
    SHIFT_PENALTY = {0: 0, -1: 30, -2: 80, -3: 150, -4: 250, -5: 380}
    # Drop строй чуть менее приоритетен чем стандарт при равном счёте
    DROP_PENALTY = 20

    for name, info in ALL_TUNINGS.items():
        open_midis = info["strings"]
        shift = info["shift"]
        is_drop = info["type"] == "drop"

        score = 0
        total_fret = 0
        open_string_bonus = 0
        low_string_hits = 0  # попадания на 5-6 струну — важны для Drop

        for n in notes:
            midi = n["midi"]
            fits = [(midi - base, s) for s, base in open_midis.items() if 0 <= midi - base <= 12]
            if fits:
                score += 1
                min_fret = min(f for f, _ in fits)
                best_string = min(fits, key=lambda x: x[0])[1]
                total_fret += min_fret
                if min_fret == 0:
                    open_string_bonus += 150
                if best_string in (5, 6):
                    low_string_hits += 1

        # Drop строй получает бонус если много нот на низких струнах
        drop_bonus = low_string_hits * 10 if is_drop else 0

        penalty = SHIFT_PENALTY.get(shift, abs(shift) * 100) + (DROP_PENALTY if is_drop else 0)
        weighted = score * 1000 - total_fret + open_string_bonus + drop_bonus - penalty

        if weighted > best_score:
            best_score = weighted
            best_name = name

    chosen = ALL_TUNINGS[best_name]
    print(f"Автоопределён строй: {best_name} ({chosen['label']}) score={best_score}")
    return best_name, chosen["strings"]


def build_tab_data(notes, tuning="auto"):
    if not notes:
        return {"strings": {}}

    # Разрешаем клиенту передать конкретный строй или auto
    if tuning in ("auto", "standard", "") or tuning not in ALL_TUNINGS:
        alias = TUNING_ALIASES.get(tuning)
        if alias and tuning not in ("standard", "auto", ""):
            tuning_name = alias
            open_midis = ALL_TUNINGS[alias]["strings"]
            print(f"Используется строй (алиас): {tuning_name} ({ALL_TUNINGS[alias]['label']})")
        else:
            tuning_name, open_midis = detect_tuning_from_notes(notes)
    else:
        tuning_name = tuning
        open_midis = ALL_TUNINGS[tuning]["strings"]
        print(f"Используется строй: {tuning_name} ({ALL_TUNINGS[tuning]['label']})")

    # Кандидаты для каждой ноты
    sequence = []
    for n in notes:
        midi = n["midi"]
        cands = [
            (s, midi - base)
            for s, base in open_midis.items()
            if 0 <= midi - base <= 12
        ]
        if cands:
            sequence.append((n["note"], midi, cands))

    if not sequence:
        return {"strings": {}}

    # DP
    INF = float("inf")
    dp   = [{} for _ in sequence]
    back = [{} for _ in sequence]

    for i, (_, _, cands) in enumerate(sequence):
        for (s, f) in cands:
            base_cost = (0 if f == 0 else f * 0.8 + (15 if f > 5 else 0))

            if i == 0:
                dp[i][(s, f)] = base_cost
                continue

            best, prev = INF, None
            for (ps, pf), pcost in dp[i-1].items():
                fd = abs(f - pf)
                sd = abs(s - ps)
                transition = (
                        fd * 1.0
                        + (0 if sd <= 1 else 8 * (sd - 1))
                        + (30 if fd > 5 else 0)
                        + (25 if sd == 0 and f != 0 else 0)
                )
                cost = pcost + base_cost + transition
                if cost < best:
                    best, prev = cost, (ps, pf)

            dp[i][(s, f)] = best
            back[i][(s, f)] = prev

    # Backtrack
    curr = min(dp[-1], key=dp[-1].get)
    path = [curr]
    for i in range(len(sequence) - 1, 0, -1):
        curr = back[i][curr]
        path.append(curr)
    path.reverse()

    # Рендер
    STRING_NAMES = {1: "e", 2: "B", 3: "G", 4: "D", 5: "A", 6: "E"}
    columns = []
    for (_, _, _), (s, f) in zip(sequence, path):
        col = {i: "-" for i in range(1, 7)}
        col[s] = str(f)
        columns.append(col)

    widths = [max(len(col[s]) for s in range(1, 7)) for col in columns]

    print()
    for s in range(1, 7):
        row = STRING_NAMES[s] + "|"
        for col, w in zip(columns, widths):
            val = col[s].ljust(w, "-")
            row += val + "--"
        print(row + "|")
    print()

    return {
        "strings":      {str(s): [col[s] for col in columns] for s in range(1, 7)},
        "tuning":       tuning_name,
        "string_names": {str(s): midi_to_note_name(open_midis[s]) for s in range(1, 7)},
    }


def midi_to_note_name(midi):
    names = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
    return names[midi % 12]