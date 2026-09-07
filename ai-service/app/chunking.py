from app.config import settings


def chunk_text(text: str, chunk_size: int | None = None, overlap: int | None = None) -> list[str]:
    """
    Simple sliding-window character chunker (per spec: "simple
    recursive/character splitter is fine"). Splits on whitespace-safe
    boundaries where possible, with a fixed overlap between consecutive
    chunks so a fact near a chunk boundary isn't lost entirely from either
    side.
    """
    chunk_size = chunk_size or settings.CHUNK_SIZE
    overlap = overlap if overlap is not None else settings.CHUNK_OVERLAP
    # Guard: overlap must be strictly less than chunk_size, otherwise consecutive
    # chunks are nearly identical (every chunk contains almost all of the previous
    # one), which wastes vector-store space and distorts retrieval ranking.
    overlap = min(overlap, max(0, chunk_size - 1))

    text = text.strip()
    if not text:
        return []

    if len(text) <= chunk_size:
        return [text]

    chunks: list[str] = []
    start = 0
    length = len(text)

    while start < length:
        end = min(start + chunk_size, length)

        # Try not to cut a word in half: back up to the last whitespace
        # within this window, unless that would make the chunk tiny.
        if end < length:
            last_space = text.rfind(" ", start, end)
            if last_space > start + (chunk_size // 2):
                end = last_space

        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)

        if end >= length:
            break

        start = max(end - overlap, start + 1)  # always make forward progress

    return chunks
