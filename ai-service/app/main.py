import logging

from fastapi import FastAPI, HTTPException, Request
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from app.chunking import chunk_text
from app.config import settings
from app.embeddings import embed_texts
from app.llm import build_chunk_previews, generate_answer
from app.schemas import IngestRequest, IngestResponse, QueryRequest, QueryResponse
from app.vectorstore import chroma_mode, get_collection, query_collection, upsert_chunks

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("vaultmd.ai-service")

limiter = Limiter(key_func=get_remote_address)

app = FastAPI(title="VaultMD AI Service", version="0.1.0")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "chroma_mode": chroma_mode(),
        "embedding_model": settings.EMBEDDING_MODEL_NAME,
        "gemini_configured": settings.GEMINI_API_KEY is not None,
    }


@app.post("/ingest", response_model=IngestResponse)
@limiter.limit("60/minute")
def ingest(req: IngestRequest, request: Request):
    chunks = chunk_text(req.text)
    if not chunks:
        raise HTTPException(status_code=400, detail="No content to ingest after chunking")

    embeddings = embed_texts(chunks)
    collection = get_collection(req.patient_id)
    upsert_chunks(collection, req.record_id, chunks, embeddings)

    logger.info("Ingested record %s for patient %s (%d chunks)", req.record_id, req.patient_id, len(chunks))
    return IngestResponse(patient_id=req.patient_id, record_id=req.record_id, chunks_ingested=len(chunks))


@app.post("/query", response_model=QueryResponse)
@limiter.limit("20/minute")
def query(req: QueryRequest, request: Request):
    # Every lookup is scoped to exactly one patient's collection - see
    # vectorstore.get_collection. There is no parameter or code path here
    # that can search another patient's data.
    collection = get_collection(req.patient_id)
    collection_empty = collection.count() == 0

    query_embedding = embed_texts([req.question])[0]
    results = query_collection(collection, query_embedding)

    if not results:
        return QueryResponse(answer="not found in records", sources=[], collection_empty=collection_empty)

    answer, sources = generate_answer(req.question, results)
    return QueryResponse(answer=answer, sources=sources, chunk_previews=build_chunk_previews(results))
