import logging
import math

from google import genai
from google.genai import types

from app.config import settings

logger = logging.getLogger("vaultmd.ai-service.llm")

SYSTEM_PROMPT = (
    "You are a clinical assistant helping a doctor review one patient's "
    "records. Answer ONLY using the provided context. "
    "If the answer is not supported by the context, reply exactly: "
    '"not found in records". '
    "Be concise, and mention which record(s) the information came from."
)


def generate_answer(
    question: str,
    results: list[tuple[str, dict, float]],
) -> tuple[str, list[str]]:
    """
    Generate a grounded answer from retrieved patient records.

    results:
        List of (chunk_text, metadata, l2_distance) from query_collection().

    Returns:
        (answer_text, list_of_unique_record_ids)
    """
    # Collect unique source record IDs (preserving relevance order).
    sources: list[str] = []
    for _, meta, _ in results:
        record_id = meta.get("record_id")
        if record_id and record_id not in sources:
            sources.append(record_id)

    # No Gemini key → extractive fallback keeps the demo functional.
    if not settings.GEMINI_API_KEY:
        return _extractive_answer(results, sources)

    try:
        client = genai.Client(api_key=settings.GEMINI_API_KEY)

        context = "\n\n".join(
            f"[Record {meta.get('record_id')}] {doc}"
            for doc, meta, _ in results
        )

        user_prompt = (
            f"Patient record context:\n\n"
            f"{context}\n\n"
            f"Doctor's question:\n{question}\n\n"
            "Answer using only the patient record context above."
        )

        response = client.models.generate_content(
            model=settings.GEMINI_MODEL,
            contents=user_prompt,
            config=types.GenerateContentConfig(
                system_instruction=SYSTEM_PROMPT,
                temperature=0.0,
            ),
        )

        answer = (response.text or "").strip()

        if not answer:
            logger.warning("Gemini returned an empty response — falling back to extractive")
            return _extractive_answer(results, sources)

        return answer, sources

    except Exception as exc:
        logger.warning(
            "Gemini call failed, falling back to extractive answer: %s", exc
        )
        return _extractive_answer(results, sources)


def _extractive_answer(
    results: list[tuple[str, dict, float]],
    sources: list[str],
) -> tuple[str, list[str]]:
    """Fallback: return the most relevant chunk verbatim."""
    if not results:
        return "not found in records", []

    top_chunk, top_meta, _ = results[0]
    record_id = top_meta.get("record_id")
    answer = f"[extractive match, no LLM configured] {top_chunk}"
    return answer, ([record_id] if record_id else sources)


def build_chunk_previews(results: list[tuple[str, dict, float]]) -> list[dict]:
    """
    Build the chunk_previews list to return in the API response.
    relevance = 1 / (1 + l2_distance)  → 1.0 = perfect match, ~0 = no match.
    """
    previews = []
    seen_record_ids: set[str] = set()

    for doc, meta, distance in results:
        record_id = meta.get("record_id", "unknown")
        # One preview per record (the most relevant chunk for that record).
        if record_id in seen_record_ids:
            continue
        seen_record_ids.add(record_id)

        relevance = round(1.0 / (1.0 + distance), 3)
        snippet = doc[:220].rstrip()
        if len(doc) > 220:
            snippet += "…"

        previews.append({
            "record_id": record_id,
            "snippet": snippet,
            "relevance": relevance,
        })

    return previews