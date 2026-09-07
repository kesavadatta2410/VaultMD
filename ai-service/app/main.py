import logging

from fastapi import FastAPI, HTTPException

from app.chunking import chunk_text
from app.embeddings import embed_texts
from app.llm import generate_answer
from app.schemas import IngestRequest, IngestResponse, QueryRequest, QueryResponse
from app.vectorstore import get_collection, query_collection, upsert_chunks

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("vaultmd.ai-service")

app = FastAPI(title="VaultMD AI Service", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ingest", response_model=IngestResponse)
def ingest(req: IngestRequest):
    chunks = chunk_text(req.text)
    if not chunks:
        raise HTTPException(status_code=400, detail="No content to ingest after chunking")

    embeddings = embed_texts(chunks)
    collection = get_collection(req.patient_id)
    upsert_chunks(collection, req.record_id, chunks, embeddings)

    logger.info("Ingested record %s for patient %s (%d chunks)", req.record_id, req.patient_id, len(chunks))
    return IngestResponse(patient_id=req.patient_id, record_id=req.record_id, chunks_ingested=len(chunks))


@app.post("/query", response_model=QueryResponse)
def query(req: QueryRequest):
    # Every lookup is scoped to exactly one patient's collection - see
    # vectorstore.get_collection. There is no parameter or code path here
    # that can search another patient's data.
    collection = get_collection(req.patient_id)

    query_embedding = embed_texts([req.question])[0]
    results = query_collection(collection, query_embedding)

    if not results:
        return QueryResponse(answer="not found in records", sources=[])

    answer, sources = generate_answer(req.question, results)
    return QueryResponse(answer=answer, sources=sources)
