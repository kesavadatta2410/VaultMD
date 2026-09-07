import logging

from app.config import settings

logger = logging.getLogger("vaultmd.ai-service.llm")

SYSTEM_PROMPT = (
    "You are a clinical assistant helping a doctor review one patient's "
    "records. Answer ONLY using the provided context. If the answer is not "
    "supported by the context, reply exactly: \"not found in records\". "
    "Be concise, and mention which record(s) the information came from."
)


def generate_answer(question: str, results: list[tuple[str, dict]]) -> tuple[str, list[str]]:
    """
    results: list of (chunk_text, metadata) from the vector store, already
    scoped to a single patient's collection by the caller.
    Returns (answer, sources) where sources is the list of record_ids cited.
    """
    # Preserve retrieval order (most relevant first) while de-duplicating.
    sources: list[str] = []
    for _, meta in results:
        rid = meta.get("record_id")
        if rid and rid not in sources:
            sources.append(rid)

    if settings.OPENAI_API_KEY:
        try:
            return _generate_with_openai(question, results), sources
        except Exception as exc:
            logger.warning("OpenAI call failed, falling back to extractive answer: %s", exc)

    # Extractive fallback (per spec: return the most relevant chunk(s)
    # verbatim with their source, rather than blocking the demo on an LLM key).
    top_chunk, top_meta = results[0]
    answer = f"[extractive match, no LLM configured] {top_chunk}"
    return answer, [top_meta.get("record_id")] if top_meta.get("record_id") else sources


def _generate_with_openai(question: str, results: list[tuple[str, dict]]) -> str:
    from openai import OpenAI

    client = OpenAI(api_key=settings.OPENAI_API_KEY)
    context = "\n\n".join(f"[{meta.get('record_id')}] {doc}" for doc, meta in results)
    user_prompt = f"Context:\n{context}\n\nQuestion: {question}"

    response = client.chat.completions.create(
        model=settings.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0,
    )
    return response.choices[0].message.content.strip()
