from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import tempfile
import os
import traceback

from separator import separate_guitar
from pitch import detect_notes
from tab_builder import build_tab

app = FastAPI()

# разрешённые типы — только mp3, m4a, wav
ALLOWED_MIME_TYPES = {
    "audio/mpeg",
    "audio/mp4",
    "audio/x-m4a",
    "audio/wav",
    "audio/x-wav",
    "audio/wave",
}


@app.get("/ping")
def ping():
    # Android вызывает это при старте чтобы проверить что сервер живой
    return {"status": "ok"}


@app.post("/analyze")
async def analyze(
    file: UploadFile = File(...),
    tuning: str = "standard"
):
    # проверяем mime тип ещё на сервере как второй рубеж защиты
    if file.content_type not in ALLOWED_MIME_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Неподдерживаемый формат. Загрузите MP3, M4A или WAV."
        )

    # определяем расширение по mime
    ext_map = {
        "audio/mpeg": ".mp3",
        "audio/mp4": ".m4a",
        "audio/x-m4a": ".m4a",
        "audio/wav": ".wav",
        "audio/x-wav": ".wav",
        "audio/wave": ".wav",
    }
    suffix = ext_map.get(file.content_type, ".mp3")

    # сохраняем загруженный файл во временную папку
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    guitar_path = None

    try:
        # шаг 1 — отделяем гитару через Demucs
        guitar_path = separate_guitar(tmp_path)

        # шаг 2 — определяем ноты через CREPE + librosa onset detection
        notes = detect_notes(guitar_path)

        if not notes:
            raise HTTPException(
                status_code=422,
                detail="Ноты не найдены. Проверьте качество записи."
            )

        # шаг 3 — строим табулатуру
        tab = build_tab(notes, tuning=tuning)

        return JSONResponse({
            "status": "ok",
            "notes": notes,
            "tab": tab
        })

    except HTTPException:
        raise

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        # всегда удаляем временные файлы чтобы не засорять сервер
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)
        if guitar_path and os.path.exists(guitar_path):
            os.unlink(guitar_path)