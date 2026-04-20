import subprocess
import os
import shutil
import tempfile


def separate_guitar(input_path: str) -> str:
    """
    Запускает Demucs htdemucs модель.
    Отделяет stem 'other' — это гитара + всё что не drums/bass/vocals.
    Возвращает путь к wav файлу с изолированной гитарой.
    """

    output_dir = tempfile.mkdtemp(prefix="demucs_")

    try:
        result = subprocess.run(
            [
                "python", "-m", "demucs",
                "--two-stems", "other",
                "--out", output_dir,
                input_path
            ],
            check=True,
            capture_output=True,
            text=True
        )
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Demucs завершился с ошибкой: {e.stderr}")

    # Demucs кладёт результат в output_dir/htdemucs/<имя_файла>/other.wav
    basename = os.path.splitext(os.path.basename(input_path))[0]
    guitar_wav = os.path.join(output_dir, "htdemucs", basename, "other.wav")

    # если не нашли по стандартному пути — ищем рекурсивно
    if not os.path.exists(guitar_wav):
        for root, dirs, files in os.walk(output_dir):
            for f in files:
                if f == "other.wav":
                    guitar_wav = os.path.join(root, f)
                    break

    if not os.path.exists(guitar_wav):
        raise RuntimeError(f"Demucs не создал other.wav. Содержимое папки: {os.listdir(output_dir)}")

    # копируем результат во временный файл вне папки Demucs
    result_path = tempfile.mktemp(suffix="_guitar.wav")
    shutil.copy2(guitar_wav, result_path)

    # удаляем всю папку Demucs — она занимает много места
    shutil.rmtree(output_dir, ignore_errors=True)

    return result_path