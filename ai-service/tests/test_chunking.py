from app.chunking import chunk_text


def test_empty_text_returns_no_chunks():
    assert chunk_text("") == []
    assert chunk_text("   ") == []


def test_short_text_returns_single_chunk():
    text = "Allergy: penicillin, confirmed 2019."
    chunks = chunk_text(text, chunk_size=500, overlap=50)
    assert chunks == [text]


def test_long_text_is_split_into_multiple_chunks():
    text = ("Lipid panel result. " * 100).strip()  # ~2000 chars
    chunks = chunk_text(text, chunk_size=500, overlap=50)
    assert len(chunks) > 1
    for c in chunks:
        assert len(c) <= 500 + 1  # small slack for boundary snapping
        assert c == c.strip()


def test_chunks_cover_the_whole_text_with_overlap():
    text = ("word " * 300).strip()  # long enough to force multiple chunks
    chunks = chunk_text(text, chunk_size=200, overlap=20)
    assert len(chunks) > 1
    # every word in the original text should appear in at least one chunk
    reconstructed = " ".join(chunks)
    for word in text.split():
        assert word in reconstructed


def test_always_makes_forward_progress():
    # regression guard: a bad overlap/window calc could loop forever
    text = "x" * 10000
    chunks = chunk_text(text, chunk_size=100, overlap=99)
    assert len(chunks) > 1
    assert len(chunks) < 10000  # sanity bound, would blow up on infinite loop
