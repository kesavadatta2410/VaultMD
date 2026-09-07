import threading
from functools import lru_cache

from sentence_transformers import SentenceTransformer

from app.config import settings

_model_lock = threading.Lock()


@lru_cache(maxsize=1)
def _get_model() -> SentenceTransformer:
    # Loaded lazily (and cached) rather than at import time, so the app can
    # start and answer /health even before the model is warmed up, and so
    # importing this module in tests doesn't require a model download.
    # The lock prevents double-initialization during concurrent cold-start requests.
    with _model_lock:
        return SentenceTransformer(settings.EMBEDDING_MODEL_NAME)


def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    model = _get_model()
    embeddings = model.encode(texts, show_progress_bar=False, convert_to_numpy=True)
    return embeddings.tolist()
