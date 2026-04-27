# main.py — без изменений кроме импортов
import os, shutil, tempfile, uuid
from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from audio_processor import process_audio_to_notes
from tab_builder import build_tab_data

app = FastAPI()
app.add_middleware(CORSMiddleware, allow_origins=["*"],
                   allow_methods=["*"], allow_headers=["*"])

@app.get("/ping")
async def ping():
    return {"status": "ok"}

# main.py — фрагмент analyze()
@app.post("/analyze")
async def analyze(file: UploadFile = File(...), tuning: str = Form("standard")):
    tmp = tempfile.mkdtemp()
    ext = file.filename.rsplit(".", 1)[-1]
    path = os.path.join(tmp, f"input_{uuid.uuid4()}.{ext}")
    try:
        with open(path, "wb") as f:
            shutil.copyfileobj(file.file, f)

        notes, detected_tuning = process_audio_to_notes(path, tmp, tuning=tuning)
        tab = build_tab_data(notes, detected_tuning)  # передаём уже определённый строй

        return {
            "notes":        notes,
            "strings":      tab.get("strings", {}),
            "tuning":       tab.get("tuning", ""),
            "string_names": tab.get("string_names", {}),
            "total_notes":  len(notes)
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        shutil.rmtree(tmp, ignore_errors=True)



if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)