import threading
from functools import lru_cache

from fastembed import TextEmbedding

from app.config import settings

_model_lock = threading.Lock()

# fastembed (ONNX runtime) instead of sentence-transformers (PyTorch): the
# same all-MiniLM-L6-v2 model, but without pulling in full PyTorch, which by
# itself was enough to exceed Render free tier's 512MB memory limit before
# the model even finished loading.


@lru_cache(maxsize=1)
def _get_model() -> TextEmbedding:
    # Loaded lazily (and cached) rather than at import time, so the app can
    # start and answer /health even before the model is warmed up, and so
    # importing this module in tests doesn't require a model download.
    # The lock prevents double-initialization during concurrent cold-start requests.
    with _model_lock:
        return TextEmbedding(model_name=settings.EMBEDDING_MODEL_NAME)


def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    model = _get_model()
    return [embedding.tolist() for embedding in model.embed(texts)]
