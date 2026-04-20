import torchcrepe
import torch
import librosa
import numpy as np
import soundfile as sf

GUITAR_MIDI_MIN = 40
GUITAR_MIDI_MAX = 88
CONFIDENCE_THRESHOLD = 0.65


def detect_notes(guitar_path: str) -> list[dict]:
    audio, sr = sf.read(guitar_path)

    if audio.ndim > 1:
        audio = audio.mean(axis=1)

    audio_16k = librosa.resample(audio, orig_sr=sr, target_sr=16000)

    # torchcrepe принимает tensor
    audio_tensor = torch.tensor(audio_16k, dtype=torch.float32).unsqueeze(0)

    # определяем питч каждые 10ms
    hop_length = int(16000 * 0.01)  # 10ms при 16000 Hz

    frequency, confidence = torchcrepe.predict(
        audio_tensor,
        sample_rate=16000,
        hop_length=hop_length,
        fmin=torchcrepe.constants.FMIN,
        fmax=torchcrepe.constants.FMAX,
        model="full",
        return_periodicity=True,
        decoder=torchcrepe.decode.viterbi
    )

    # переводим в numpy
    freq_arr = frequency.squeeze().numpy()
    confidence_arr = confidence.squeeze().numpy()

    # временные метки
    time_arr = np.arange(len(freq_arr)) * 0.01

    # onset detection
    onset_times = librosa.onset.onset_detect(
        y=audio,
        sr=sr,
        units="time",
        backtrack=True,
        pre_max=3,
        post_max=3,
        pre_avg=3,
        post_avg=5,
        delta=0.07,
        wait=10
    )

    if len(onset_times) == 0:
        return []

    notes = []

    for i, onset in enumerate(onset_times):
        end_time = onset_times[i + 1] if i + 1 < len(onset_times) else float(time_arr[-1])

        mask = (time_arr >= onset) & (time_arr < end_time)
        if not mask.any():
            continue

        freqs = freq_arr[mask]
        confs = confidence_arr[mask]

        valid_mask = confs >= CONFIDENCE_THRESHOLD
        if not valid_mask.any():
            continue

        median_freq = float(np.median(freqs[valid_mask]))
        mean_conf = float(np.mean(confs[valid_mask]))

        note_name = freq_to_note(median_freq)
        if note_name is None:
            continue

        notes.append({
            "note": note_name,
            "time": round(float(onset), 3),
            "duration": round(float(end_time - onset), 3),
            "confidence": round(mean_conf, 3),
            "frequency": round(median_freq, 2)
        })

    return notes


def freq_to_note(freq: float) -> str | None:
    if freq <= 0:
        return None

    note_names = ["C", "C#", "D", "D#", "E", "F",
                  "F#", "G", "G#", "A", "A#", "B"]

    midi = round(12 * np.log2(freq / 440.0) + 69)

    if midi < GUITAR_MIDI_MIN or midi > GUITAR_MIDI_MAX:
        return None

    octave = (midi // 12) - 1
    name = note_names[midi % 12]

    return f"{name}{octave}"