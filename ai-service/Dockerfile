FROM python:3.11-slim

WORKDIR /app

# gcc/g++ needed to build some chromadb native deps
RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc g++ \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download the embedding model at build time so a cold start (e.g. after
# Render's free tier spins the service down from inactivity) doesn't also
# need to fetch it over the network before it can serve /ingest or /query.
RUN python -c "from fastembed import TextEmbedding; TextEmbedding(model_name='sentence-transformers/all-MiniLM-L6-v2')"

COPY app ./app

ENV CHROMA_PERSIST_DIR=/data/chroma
VOLUME ["/data/chroma"]

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
