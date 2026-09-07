from typing import List

from pydantic import BaseModel, Field


class IngestRequest(BaseModel):
    patient_id: str
    record_id: str
    text: str = Field(..., min_length=1)


class IngestResponse(BaseModel):
    patient_id: str
    record_id: str
    chunks_ingested: int


class QueryRequest(BaseModel):
    patient_id: str
    question: str = Field(..., min_length=1)


class ChunkPreview(BaseModel):
    """A retrieved chunk shown to the doctor alongside the AI answer."""
    record_id: str
    snippet: str          # first 200 chars of the chunk
    relevance: float      # 0-1, derived from L2 distance (1 = perfect match)


class QueryResponse(BaseModel):
    answer: str
    sources: List[str]
    chunk_previews: List[ChunkPreview] = []
