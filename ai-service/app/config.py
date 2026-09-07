import os
from pydantic import BaseModel


class Settings(BaseModel):
    CHROMA_PERSIST_DIR: str = os.getenv("CHROMA_PERSIST_DIR", "./chroma_data")
    EMBEDDING_MODEL_NAME: str = os.getenv("EMBEDDING_MODEL_NAME", "all-MiniLM-L6-v2")
    TOP_K: int = int(os.getenv("TOP_K", "4"))

    # ASSUMPTION: if no LLM key is configured, /query falls back to an
    # extractive answer (most relevant chunk verbatim + its source) per the
    # spec's explicit instruction not to block the demo on this.
    OPENAI_API_KEY: str | None = os.getenv("OPENAI_API_KEY") or None
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

    CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "500"))
    CHUNK_OVERLAP: int = int(os.getenv("CHUNK_OVERLAP", "50"))


settings = Settings()
