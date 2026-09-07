import os
from pathlib import Path

from dotenv import load_dotenv
from pydantic import BaseModel

# Load .env from the ai-service directory first, then fall back to the
# project root (one level up). This means both `uvicorn app.main:app`
# (run from ai-service/) and `docker-compose up` (mounts project root)
# pick up the right env file automatically.
_here = Path(__file__).parent.parent          # ai-service/
_root = _here.parent                           # project root (Vaultmd/)

load_dotenv(_here / ".env", override=False)    # ai-service/.env  (if it exists)
load_dotenv(_root / ".env", override=False)    # project root .env


class Settings(BaseModel):
    # Chroma persistence directory.
    # • Local dev  → resolves to ai-service/chroma_data  (relative to CWD)
    # • Docker     → /data/chroma  (mounted volume)
    # • Render     → set CHROMA_PERSIST_DIR="" to use in-memory mode (free tier)
    CHROMA_PERSIST_DIR: str = os.getenv("CHROMA_PERSIST_DIR", "./chroma_data")

    EMBEDDING_MODEL_NAME: str = os.getenv("EMBEDDING_MODEL_NAME", "all-MiniLM-L6-v2")
    TOP_K: int = int(os.getenv("TOP_K", "4"))

    # Gemini: free tier via Google AI Studio — https://aistudio.google.com/app/apikey
    # If not set, /query falls back to an extractive (verbatim chunk) answer.
    GEMINI_API_KEY: str | None = os.getenv("GEMINI_API_KEY") or None
    # gemini-2.5-flash-lite has the most generous free-tier quota
    # (1000 requests/day, 15/min) of the current Gemini model lineup —
    # important since this demo is publicly queryable. Override via env
    # if you have a paid key and want a stronger model.
    GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-2.5-flash-lite")

    CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "500"))
    CHUNK_OVERLAP: int = int(os.getenv("CHUNK_OVERLAP", "50"))


settings = Settings()
