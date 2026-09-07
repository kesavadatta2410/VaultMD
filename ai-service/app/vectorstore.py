import chromadb

from app.config import settings

# A single persistent client for the process. Collections are created
# on-demand per patient (see get_collection) so there's no shared collection
# that could accidentally be queried across patients.
_client = chromadb.PersistentClient(path=settings.CHROMA_PERSIST_DIR)


def _collection_name(patient_id: str) -> str:
    return f"patient_{patient_id}"


def get_collection(patient_id: str):
    """
    Returns (creating if needed) the Chroma collection scoped to exactly one
    patient. Every caller in this service goes through this function with a
    single patient_id, so there is no code path that can search across
    patients - the collection boundary IS the patient boundary.
    """
    return _client.get_or_create_collection(name=_collection_name(patient_id))


def upsert_chunks(collection, record_id: str, chunks: list[str], embeddings: list[list[float]]) -> None:
    if not chunks:
        return
    ids = [f"{record_id}_{i}" for i in range(len(chunks))]
    metadatas = [{"record_id": record_id, "chunk_index": i} for i in range(len(chunks))]
    collection.upsert(ids=ids, documents=chunks, embeddings=embeddings, metadatas=metadatas)


def query_collection(collection, query_embedding: list[float], top_k: int | None = None) -> list[tuple[str, dict]]:
    top_k = top_k if top_k is not None else settings.TOP_K
    count = collection.count()
    if count == 0:
        return []

    n_results = min(top_k, count)
    results = collection.query(query_embeddings=[query_embedding], n_results=n_results)

    docs = results.get("documents") or [[]]
    metas = results.get("metadatas") or [[]]
    return list(zip(docs[0], metas[0]))
