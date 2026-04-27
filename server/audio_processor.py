# audio_processor.py
import os
import subprocess
import glob
from basic_pitch.inference import predict

GUITAR_MIDI_MIN = 28   # E1 — запас ниже Drop B
GUITAR_MIDI_MAX = 88   # E6

# Таблица диапазонов для каждого строя:
# (midi нижней открытой струны, midi верхней 24 лада)
TUNING_MIDI_RANGES = {
    "standard_0":  (40, 88),  # E2 — E6
    "standard_-1": (39, 87),  # Eb2 — Eb6
    "standard_-2": (38, 86),  # D2 — D6
    "standard_-3": (37, 85),  # C#2 — C#6
    "standard_-4": (36, 84),  # C2 — C6
    "standard_-5": (35, 83),  # B1 — B5
    "drop_0":      (38, 88),  # D2 — E6
    "drop_-1":     (37, 87),  # C#2 — Eb6
    "drop_-2":     (36, 86),  # C2 — D6
    "drop_-3":     (35, 85),  # B1 — C#6
    "drop_-4":     (34, 84),  # Bb1 — C6
    "drop_-5":     (33, 83),  # A1 — B5
}

def midi_to_name(midi):
    names = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
    return f"{names[midi % 12]}{(midi // 12) - 1}"

def detect_tuning_early(raw_notes):
    from tab_builder import detect_tuning_from_notes
    note_dicts = []
    for n in raw_notes:
        try:
            start, end, midi, conf = float(n[0]), float(n[1]), int(n[2]), float(n[3])
            if midi < GUITAR_MIDI_MIN or midi > GUITAR_MIDI_MAX:
                continue
            note_dicts.append({"midi": midi, "note": midi_to_name(midi), "confidence": conf})
        except Exception:
            continue
    tuning_name, _ = detect_tuning_from_notes(note_dicts)
    print(f"[Early detection] Строй определён по {len(note_dicts)} сырым нотам: {tuning_name}")
    return tuning_name

def process_audio_to_notes(file_path, temp_dir, tuning=None, max_fret=19):

    subprocess.run([
        "demucs", "--two-stems", "vocals",
        "-o", os.path.join(temp_dir, "separated"),
        file_path
    ], check=True, capture_output=True)

    found = []
    for p in ["**/no_vocals.wav", "**/other.wav"]:
        found = glob.glob(os.path.join(temp_dir, "separated", p), recursive=True)
        if found:
            break
    if not found:
        raise Exception("No instrumental found after Demucs")

    audio_path = found[0]

    _, _, note_events = predict(
        audio_path,
        onset_threshold=0.5,
        frame_threshold=0.3,
        minimum_note_length=60,
        maximum_frequency=1318,
        melodia_trick=False,
        multiple_pitch_bends=False,
    )
    low_notes = [(float(n[0]), int(n[2]), float(n[3])) for n in note_events if int(n[2]) < 45]
    print(f"[DEBUG] Низкие ноты из basic_pitch (midi<45): {low_notes}")

    # Определяем строй по сырым нотам ДО постобработки
    if tuning in ("auto", "standard", "", None):
        tuning = detect_tuning_early(note_events)

    # Берём диапазон конкретно для этого строя
    midi_min, midi_max = TUNING_MIDI_RANGES.get(tuning, (GUITAR_MIDI_MIN, GUITAR_MIDI_MAX))
    print(f"[Range] Строй {tuning}: MIDI {midi_min}..{midi_max} "
          f"({midi_to_name(midi_min)}..{midi_to_name(midi_max)})")

    # Постобработка
    notes = []
    for n in note_events:
        try:
            start, end, midi, conf = float(n[0]), float(n[1]), int(n[2]), float(n[3])
            if conf < 0.4 or (end - start) < 0.05:
                continue
            if midi < midi_min or midi > midi_max:
                continue
            notes.append({
                "midi":       midi,
                "time":       round(start, 3),
                "duration":   round(end - start, 3),
                "confidence": round(conf, 2),
                "note":       midi_to_name(midi),
            })
        except Exception:
            continue

    notes.sort(key=lambda x: x["time"])
    mono = []
    i = 0
    while i < len(notes):
        group = [notes[i]]
        j = i + 1
        while j < len(notes) and notes[j]["time"] - notes[i]["time"] < 0.07:
            group.append(notes[j])
            j += 1
        mono.append(max(group, key=lambda x: x["confidence"]))
        i = j
    mono.sort(key=lambda x: x["time"])

    if len(mono) > 1 and mono[0]["time"] < 0.8 and (mono[1]["time"] - mono[0]["time"]) > 0.4:
        mono = mono[1:]

    deduped = []
    for n in mono:
        recent = [r for r in deduped if abs(r["time"] - n["time"]) < 0.3]
        if not any(r["midi"] == n["midi"] for r in recent):
            deduped.append(n)

    # Октавная коррекция строго в диапазоне определённого строя
    result = []
    for i, n in enumerate(deduped):
        midi = n["midi"]
        if n["confidence"] < 0.55:
            confident = [
                x for x in deduped
                if x["confidence"] >= 0.55 and abs(x["time"] - n["time"]) < 1.5
            ]
            if confident:
                nearest = min(confident, key=lambda x: abs(x["time"] - n["time"]))
                diff = midi - nearest["midi"]
                if diff % 12 == 0 and diff != 0:
                    corrected = nearest["midi"]
                    # Принимаем коррекцию только если попадает в диапазон строя
                    if midi_min <= corrected <= midi_max:
                        midi = corrected
        result.append({**n, "midi": midi, "note": midi_to_name(midi)})

    print("\n" + "=" * 50)
    for i, n in enumerate(result):
        print(f"{i:02d} {n['note']:4s} midi={n['midi']} t={n['time']:.3f} conf={n['confidence']}")
    print("=" * 50)

    return result, tuning