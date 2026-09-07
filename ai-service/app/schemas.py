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


class QueryResponse(BaseModel):
    answer: str
    sources: List[str]
