import re

TUNINGS = {
    "standard": {6: 40, 5: 45, 4: 50, 3: 55, 2: 59, 1: 64},
    "drop_d":   {6: 38, 5: 45, 4: 50, 3: 55, 2: 59, 1: 64},
    "open_g":   {6: 38, 5: 43, 4: 50, 3: 55, 2: 59, 1: 62},
    "half_down":{6: 39, 5: 44, 4: 49, 3: 54, 2: 58, 1: 63},
}

NOTE_NAMES = ["C", "C#", "D", "D#", "E", "F",
              "F#", "G", "G#", "A", "A#", "B"]


def note_to_midi(note_name: str) -> int | None:
    m = re.match(r"([A-G]#?)(\d)", note_name)
    if not m:
        return None
    name, octave = m.group(1), int(m.group(2))
    if name not in NOTE_NAMES:
        return None
    return (octave + 1) * 12 + NOTE_NAMES.index(name)


def find_best_position(midi: int, open_midis: dict) -> dict | None:
    """
    Ищет лучшую позицию ноты на грифе.
    Предпочитает лады 0-7 на более низких струнах.
    """
    candidates = []

    for string_num, open_midi in open_midis.items():
        fret = midi - open_midi
        if 0 <= fret <= 24:
            candidates.append({
                "string": string_num,
                "fret": fret
            })

    if not candidates:
        return None

    candidates.sort(key=lambda x: (
        x["fret"] > 7,
        -x["string"],
        x["fret"]
    ))

    return candidates[0]


def build_tab(notes: list[dict], tuning: str = "standard") -> dict:
    open_midis = TUNINGS.get(tuning, TUNINGS["standard"])

    tab_strings = {str(i): ["--"] * len(notes) for i in range(1, 7)}
    positions = []

    for idx, note_data in enumerate(notes):
        midi = note_to_midi(note_data["note"])
        if midi is None:
            continue

        pos = find_best_position(midi, open_midis)
        if pos is None:
            continue

        string_key = str(pos["string"])
        tab_strings[string_key][idx] = str(pos["fret"])

        positions.append({
            "string": pos["string"],
            "fret": pos["fret"],
            "note": note_data["note"],
            "time": note_data["time"],
            "duration": note_data["duration"]
        })

    return {
        "tuning": tuning,
        "strings": tab_strings,
        "positions": positions
    }